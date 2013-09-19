// Links:
// https://code.google.com/p/osmdroid/issues/detail?id=392

package com.ciheul.bigmaps.activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.ciheul.bigmaps.MapsApplication;
import com.ciheul.bigmaps.R;

public class MapActivity extends Activity {

    private ResourceProxy resourceProxy;
    private MapView mapView;
    private MapController mapController;
    // private DirectedLocationOverlay currentUser;
    private ItemizedOverlay<OverlayItem> currentUserItemizedOverlay;
    private MapsApplication app;
    private MapEventsReceiver mapEventsReceiver;

    private Button btnTrackCurrentUser;

    private int colorCounter = 0;
    // private String[] MapColor = { "#762A83", "#9970AB", "#C2A5CF", "#E7D4E8", "#D9F0D3", "#A6DBA0"};

    public String[] MapColor = { "#762A83", "#9970AB", "#C2A5CF", "#E7D4E8", "#D9F0D3", "#A6DBA0", "#5AAE61", "#1B7837" };

    private HashMap<String, ArrayList<ArrayList<GeoPoint>>> hashMap;
    private String prevRegionTapped;
    private ArrayList<PathOverlay> prevPathOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("BigMaps", "MapsActivity: onCreate");

        // remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_map);

        app = (MapsApplication) getApplication();

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(8);

        btnTrackCurrentUser = (Button) findViewById(R.id.buttonTrackingMode);
        btnTrackCurrentUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));
            }
        });

        mapView.invalidate();

        hashMap = new HashMap<String, ArrayList<ArrayList<GeoPoint>>>();
        String geoJson = loadJsonFromAsset("jabar-kabupaten.json");
        addChoroplethFromGeoJSON(geoJson);

        prevPathOverlay = new ArrayList<PathOverlay>();

        mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapUpHelper(IGeoPoint tap) {
                String result = null;
                // loop 1: region
                for (Map.Entry<String, ArrayList<ArrayList<GeoPoint>>> entry : hashMap.entrySet()) {
                    ArrayList<ArrayList<GeoPoint>> borderInOutGeoPoint = entry.getValue();
                    // loop 2: border in and out
                    for (int i = 0; i < borderInOutGeoPoint.size(); i++) {
                        ArrayList<GeoPoint> borderGeoPoint = borderInOutGeoPoint.get(i);
                        if (isPointInPolygon(tap, borderGeoPoint)) {
                            result = entry.getKey();
                            break;
                        }
                    }
                }

                if (result != null) {
                    Log.d("BigMaps", "tap: " + result);
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();

                    Paint stroke = new Paint();
                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setColor(Color.parseColor("#666666"));
                    stroke.setStrokeWidth(3);

                    if (prevRegionTapped != null) {
                        for (int i = 0; i < mapView.getOverlayManager().size(); i++) {
                            // Log.d("BigMaps", mapView.getOverlays());
                        }
                        for (int i = 0; i < prevPathOverlay.size(); i++) {
                            Log.d("BigMaps", "prev: " + String.valueOf(prevPathOverlay.get(i)));
                            mapView.getOverlays().remove(prevPathOverlay.get(i));
                        }
                        Log.d("BigMaps", "size: " + String.valueOf(mapView.getOverlays().size()));
                        prevPathOverlay.clear();

                    }

                    ArrayList<ArrayList<GeoPoint>> borderInOutGeoPoint = hashMap.get(result);
                    for (int i = 0; i < borderInOutGeoPoint.size(); i++) {
                        PathOverlay border = new PathOverlay(Color.RED, getApplicationContext());
                        border.setPaint(stroke);

                        ArrayList<GeoPoint> borderGeoPoint = borderInOutGeoPoint.get(i);
                        for (int j = 0; j < borderGeoPoint.size(); j++) {
                            border.addPoint(borderGeoPoint.get(j));
                        }
                        mapView.getOverlays().add(border);
//                        int index = mapView.getOverlays().size();
//                        Log.d("BigMaps", "curr: " + String.valueOf(index));
                        prevPathOverlay.add(border);
                        mapView.invalidate();
                    }

                    prevRegionTapped = result;
                }

                return false;
            }

            @Override
            public boolean longPressHelper(IGeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(this, mapEventsReceiver);
        mapView.getOverlays().add(eventsOverlay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BigMaps", "MapsActivity: onResume");
        addUserMarker();

        restorePreviousCenterState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("BigMaps", "MapsActivity: onPause");
        savePreviousCenterState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("BigMaps", "MapsActivity: onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("BigMaps", "MapsActivity: onRestoreInstanceState");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    private void addUserMarker() {
        // set user's current location and its marker's picture
        OverlayItem currentUserOverlay = new OverlayItem("Current User", "", new GeoPoint(app.getCurrentLatitude(),
                app.getCurrentLongitude()));
        Drawable markerImage = getResources().getDrawable(R.drawable.marker_bullet_blue_16x16);
        currentUserOverlay.setMarker(markerImage);

        // add user's current location to map
        ArrayList<OverlayItem> currentUserItem = new ArrayList<OverlayItem>();
        currentUserItem.add(currentUserOverlay);

        currentUserItemizedOverlay = new ItemizedIconOverlay<OverlayItem>(currentUserItem,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return true;
                    }
                }, resourceProxy);
        mapView.getOverlays().add(currentUserItemizedOverlay);
    }

    private String loadJsonFromAsset(String filename) {
        String json = null;
        try {
            InputStream inputStream = getAssets().open(filename);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }

    private void addChoroplethFromGeoJSON(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray regions = jsonObject.getJSONArray("features");
            Log.d("BigMaps", String.valueOf(regions.length()));

            // loop 1: region
            for (int i = 0; i < regions.length(); i++) {
                new ChoroplethTask().execute(regions.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class ChoroplethTask extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... regions) {
            try {
                JSONObject properties = regions[0].getJSONObject("properties");
                String regionName = properties.getString("NAMA_KAB");
                // Log.d("BigMaps", regionName);

                JSONObject geometry = regions[0].getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");

                PathOverlay regionOverlay = new PathOverlay(Color.RED, getApplicationContext());

                Paint stroke = new Paint();
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setColor(Color.parseColor("#666666"));
                stroke.setStrokeWidth(2);

                Paint fill = new Paint();
                fill.setStyle(Paint.Style.FILL);
                fill.setColor(Color.parseColor(MapColor[colorCounter % MapColor.length]));
                regionOverlay.setPaint(fill);

                ArrayList<ArrayList<GeoPoint>> regionGeoPoints = new ArrayList<ArrayList<GeoPoint>>();
                double lon = 0;
                double lat = 0;
                // loop 2: border in and out
                for (int j = 0; j < coordinates.length(); j++) {
                    PathOverlay borderOverlay = new PathOverlay(Color.RED, getApplicationContext());
                    borderOverlay.setPaint(stroke);

                    boolean isList = false;
                    ArrayList<GeoPoint> regionGeoPoint = new ArrayList<GeoPoint>();
                    JSONArray a = coordinates.getJSONArray(j);

                    // loop 3: either another list of [lon, lat] or [lon, lat]
                    for (int k = 0; k < a.length(); k++) {
                        JSONArray b = a.getJSONArray(k);

                        if (b.length() > 2) {
                            // loop 4: [lon, lat]
                            for (int l = 0; l < b.length(); l++) {
                                JSONArray c = b.getJSONArray(l);
                                lon = c.getDouble(0);
                                lat = c.getDouble(1);
                                GeoPoint p = new GeoPoint(lat, lon);
                                regionOverlay.addPoint(p);
                                borderOverlay.addPoint(p);

                                regionGeoPoint.add(p);
                            }
                            regionGeoPoints.add(regionGeoPoint);
                            isList = true;
                        } else {
                            // [lon, lat]
                            lon = b.getDouble(0);
                            lat = b.getDouble(1);
                            GeoPoint p = new GeoPoint(lat, lon);
                            regionOverlay.addPoint(p);
                            borderOverlay.addPoint(p);
                            regionGeoPoint.add(p);
                        }
                    }

                    if (isList == false) {
                        regionGeoPoints.add(regionGeoPoint);
                    }

                    mapView.getOverlays().add(borderOverlay);
                }

                mapView.getOverlays().add(regionOverlay);

                colorCounter += 1;

                hashMap.put(regionName, regionGeoPoints);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // update UI asynchronously
            mapView.invalidate();
        }

    }

    private void restorePreviousCenterState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        float lon = prefs.getFloat("prevLongitude", (float) app.getCurrentLongitude());
        float lat = prefs.getFloat("prevLatitude", (float) app.getCurrentLatitude());

        mapController.setCenter(new GeoPoint(lat, lon));
        editor.remove("prevLongitude");
        editor.remove("prevLatitude");
        editor.commit();
    }

    private void savePreviousCenterState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("prevLongitude", (float) (mapView.getMapCenter().getLongitudeE6() / 1E6));
        editor.putFloat("prevLatitude", (float) (mapView.getMapCenter().getLatitudeE6() / 1E6));
        editor.commit();
    }

    private boolean isPointInPolygon(IGeoPoint tap, ArrayList<GeoPoint> vertices) {
        int intersectCount = 0;
        for (int j = 0; j < vertices.size() - 1; j++) {
            if (rayCastIntersect(tap, vertices.get(j), vertices.get(j + 1))) {
                intersectCount++;
            }
        }

        return ((intersectCount % 2) == 1); // odd = inside, even = outside;
    }

    private boolean rayCastIntersect(IGeoPoint tap, GeoPoint vertA, GeoPoint vertB) {

        double aY = vertA.getLatitudeE6() / 1E6;
        double bY = vertB.getLatitudeE6() / 1E6;
        double aX = vertA.getLongitudeE6() / 1E6;
        double bX = vertB.getLongitudeE6() / 1E6;
        double pY = tap.getLatitudeE6() / 1E6;
        double pX = tap.getLongitudeE6() / 1E6;

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false; // a and b can't both be above or below pt.y, and a or b must be east of pt.x
        }

        double m = (aY - bY) / (aX - bX); // Rise over run
        double bee = (-aX) * m + aY; // y = mx + b
        double x = (pY - bee) / m; // algebra is neat!

        return x > pX;
    }
}