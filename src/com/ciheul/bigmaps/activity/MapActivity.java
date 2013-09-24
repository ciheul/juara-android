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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ciheul.bigmaps.MapsApplication;
import com.ciheul.bigmaps.R;

public class MapActivity extends Activity implements MapEventsReceiver {

    private MapsApplication app;
    private ResourceProxy resourceProxy;
    private MapView mapView;
    private MapController mapController;

    /** Overlay */
    // private DirectedLocationOverlay currentUser;
    private ItemizedOverlay<OverlayItem> currentUserItemizedOverlay;
    // private ScaleBarOverlay scaleBarOverlay;
    // private MinimapOverlay miniMapOverlay;

    private Button btnTrackCurrentUser;
    private ImageView imgZoomIn;
    private ImageView imgZoomOut;
    // private EditText edtSearch;

    /** Map Color */
    private int colorCounter = 0;
    public String[] MapColor = { "#762A83", "#9970AB", "#C2A5CF", "#E7D4E8", "#D9F0D3", "#A6DBA0", "#5AAE61", "#1B7837" };

    private HashMap<String, ArrayList<ArrayList<GeoPoint>>> hashMap;
    private String prevRegionTapped;
    private ArrayList<PathOverlay> prevPathOverlay;

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("BigMaps", "MapsActivity: onCreate");

        // remove title bar
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_map);

        app = (MapsApplication) getApplication();

        prefs = getPreferences(MODE_PRIVATE);
        prefsEditor = prefs.edit();

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(8);

        btnTrackCurrentUser = (Button) findViewById(R.id.buttonTrackingMode);
        btnTrackCurrentUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));

                for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMap
                        .entrySet()) {
                    new FastChoroplethTask().execute(entry.getValue());
                }

                prefsEditor.putBoolean("choroplethMap", true).commit();
            }
        });

        // hashMap = new HashMap<String, ArrayList<ArrayList<GeoPoint>>>();
        // String geoJson = loadJsonFromAsset("jabar-kabupaten.json");
        // addChoroplethFromGeoJSON(geoJson);

        prevPathOverlay = new ArrayList<PathOverlay>();

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(this, this);
        mapView.getOverlays().add(eventsOverlay);

        // scaleBarOverlay = new ScaleBarOverlay(this, resourceProxy);
        // // scaleBarOverlay.setScaleBarOffset(getResources().getDisplayMetrics().widthPixels / 2
        // // - getResources().getDisplayMetrics().xdpi / 2, 10);
        // mapView.getOverlays().add(scaleBarOverlay);
        // scaleBarOverlay.setScaleBarOffset(10, 150);

        // miniMapOverlay = new MinimapOverlay(this, mapView.getTileRequestCompleteHandler());
        // miniMapOverlay.setEnabled(true);
        // mapView.getOverlays().add(miniMapOverlay);

        // edtSearch = (EditText) findViewById(R.id.edtSearch);

        imgZoomIn = (ImageView) findViewById(R.id.imgZoomIn);
        imgZoomIn.setImageResource(R.drawable.zoom_in);
        imgZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapController.zoomIn();
            }
        });

        imgZoomOut = (ImageView) findViewById(R.id.imgZoomOut);
        imgZoomOut.setImageResource(R.drawable.zoom_out);
        imgZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapController.zoomOut();
            }
        });

        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BigMaps", "MapsActivity: onResume");

        addUserMarker();
        restorePreviousCenterState();

        if (prefs.getBoolean("choroplethMap", false) == true) {
            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMap.entrySet()) {
                new FastChoroplethTask().execute(entry.getValue());
            }
        }
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

    /************************/
    /** MAPEVENTS RECEIVER **/
    /************************/
    @Override
    public boolean singleTapUpHelper(IGeoPoint tap) {
        if (app.structuredMap != null) {
            String regionTapped = null;

            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMap.entrySet()) {
                ArrayList<Double> lonCoordinates = new ArrayList<Double>();
                ArrayList<Double> latCoordinates = new ArrayList<Double>();

                ArrayList<HashMap<String, ArrayList<Double>>> value = entry.getValue();
                for (int i = 0; i < value.size(); i++) {
                    HashMap<String, ArrayList<Double>> lonOrLatCoordinates = value.get(i);
                    lonCoordinates.addAll(lonOrLatCoordinates.get("lon"));
                    latCoordinates.addAll(lonOrLatCoordinates.get("lat"));
                }

                if (isPointInPolygon(tap, lonCoordinates, latCoordinates)) {
                    regionTapped = entry.getKey();
                    break;
                }

                lonCoordinates.clear();
                latCoordinates.clear();
            }

            if (regionTapped != null) {
                Log.d("BigMaps", regionTapped);

                if (prevRegionTapped != null) {
                    for (int i = 0; i < prevPathOverlay.size(); i++) {
                        mapView.getOverlays().remove(prevPathOverlay.get(i));
                    }
                    prevPathOverlay.clear();
                }

                Paint stroke = new Paint();
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setColor(Color.parseColor("#666666"));
                stroke.setStrokeWidth(3);

                ArrayList<HashMap<String, ArrayList<Double>>> borders = app.structuredMap.get(regionTapped);
                for (int i = 0; i < borders.size(); i++) {
                    PathOverlay borderOverlay = new PathOverlay(Color.RED, this);
                    borderOverlay.setPaint(stroke);

                    HashMap<String, ArrayList<Double>> lonOrLatCoordinates = borders.get(i);
                    ArrayList<Double> lonCoordinates = lonOrLatCoordinates.get("lon");
                    ArrayList<Double> latCoordinates = lonOrLatCoordinates.get("lat");
                    int size = lonCoordinates.size();

                    for (int j = 0; j < size; j++) {
                        borderOverlay.addPoint(new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)));
                    }

                    mapView.getOverlays().add(borderOverlay);
                    mapView.invalidate();

                    // retain the border information
                    prevPathOverlay.add(borderOverlay);
                }
                prevRegionTapped = regionTapped;
            }
        }

        return false;
    }

    @Override
    public boolean longPressHelper(IGeoPoint tap) {
        // loop 1: region
        if (hashMap != null) {
            String result = null;
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
                // Log.d("BigMaps", "tap: " + result);
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();

                Paint stroke = new Paint();
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setColor(Color.parseColor("#666666"));
                stroke.setStrokeWidth(3);

                if (prevRegionTapped != null) {
                    for (int i = 0; i < prevPathOverlay.size(); i++) {
                        mapView.getOverlays().remove(prevPathOverlay.get(i));
                    }
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
                    prevPathOverlay.add(border);
                    mapView.invalidate();
                }
                prevRegionTapped = result;
            }
        }
        return false;
    }

    /*******************/

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

    private void restorePreviousCenterState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        float lon = prefs.getFloat("prevLongitude", (float) app.getCurrentLongitude());
        float lat = prefs.getFloat("prevLatitude", (float) app.getCurrentLatitude());

        mapController.setCenter(new GeoPoint(lat, lon));
        mapController.setZoom(prefs.getInt("zoomLevel", 8));
        editor.remove("prevLongitude");
        editor.remove("prevLatitude");
        editor.remove("zoomLevel");
        editor.commit();
    }

    private void savePreviousCenterState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("prevLongitude", (float) (mapView.getMapCenter().getLongitudeE6() / 1E6));
        editor.putFloat("prevLatitude", (float) (mapView.getMapCenter().getLatitudeE6() / 1E6));
        editor.putInt("zoomLevel", mapView.getProjection().getZoomLevel());
        editor.commit();
    }

    /*********************/
    /** UTILITY METHODS **/
    /*********************/

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

    /************************/
    /** CHOROPLETH METHODS **/
    /************************/

    private void addChoroplethFromGeoJSON(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray regions = jsonObject.getJSONArray("features");
            // Log.d("BigMaps", String.valueOf(regions.length()));

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

    private class FastChoroplethTask extends AsyncTask<ArrayList, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList... arg) {
            ArrayList<HashMap<String, ArrayList<Double>>> borders = arg[0];

            PathOverlay regionOverlay = new PathOverlay(Color.RED, getApplicationContext());

            Paint fillRegion = new Paint();
            fillRegion.setStyle(Paint.Style.FILL);
            fillRegion.setColor(Color.parseColor(MapColor[colorCounter % MapColor.length]));
            regionOverlay.setPaint(fillRegion);

            Paint stroke = new Paint();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(Color.parseColor("#666666"));
            stroke.setStrokeWidth(2);

            // iterate all borders
            for (int i = 0; i < borders.size(); i++) {
                HashMap<String, ArrayList<Double>> lonOrLatCoordinates = borders.get(i);

                PathOverlay borderOverlay = new PathOverlay(Color.RED, getApplicationContext());
                borderOverlay.setPaint(stroke);

                ArrayList<Double> lonCoordinates = lonOrLatCoordinates.get("lon");
                ArrayList<Double> latCoordinates = lonOrLatCoordinates.get("lat");
                int size = lonCoordinates.size();

                for (int j = 0; j < size; j++) {
                    GeoPoint p = new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j));
                    borderOverlay.addPoint(p);
                    regionOverlay.addPoint(p);
                }

                mapView.getOverlays().add(borderOverlay);
            }

            mapView.getOverlays().add(regionOverlay);
            colorCounter += 1;

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // update UI asynchronously
            mapView.invalidate();
        }

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

    private boolean isPointInPolygon(IGeoPoint tap, final ArrayList<Double> lonCoordinates,
            final ArrayList<Double> latCoordinates) {

        // HashMap<Boolean, Integer> result = new HashMap<Boolean, Integer>();
        // result.put(true, 0);
        // result.put(false, 0);
        //
        // final ArrayList<IGeoPoint> testTap = new ArrayList<IGeoPoint>();
        //
        //
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 - 0.005, tap0.getLongitudeE6() / 1E6 - 0.005));
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 - 0.005, tap0.getLongitudeE6() / 1E6));
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 - 0.005, tap0.getLongitudeE6() / 1E6 + 0.005));
        //
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6, tap0.getLongitudeE6() / 1E6 - 0.005));
        // testTap.add(tap0);
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6, tap0.getLongitudeE6() / 1E6 + 0.005));
        //
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 + 0.005, tap0.getLongitudeE6() / 1E6 - 0.005));
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 + 0.005, tap0.getLongitudeE6() / 1E6));
        // testTap.add(new GeoPoint(tap0.getLatitudeE6() / 1E6 + 0.005, tap0.getLongitudeE6() / 1E6 + 0.005));

        // for (int i = 0; i < testTap.size(); i++) {
        // int intersectCount = 0;
        //
        // for (int j = 0; j < lonCoordinates.size() - 1; j++) {
        // if (rayCastIntersect(testTap.get(i), new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)),
        // new GeoPoint(latCoordinates.get(j + 1), lonCoordinates.get(j + 1)))) {
        // intersectCount++;
        // }
        // }
        //
        // if (((intersectCount % 2) == 1) == true) {
        // result.put(true, result.get(true) + 1);
        // } else {
        // result.put(false, result.get(false) + 1);
        // }
        // }

        // Log.d("BigMaps", result.toString());
        // Log.d("BigMaps", " ");

        int intersectCount = 0;

        for (int j = 0; j < lonCoordinates.size() - 1; j++) {
            if (rayCastIntersect(tap, new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)), new GeoPoint(
                    latCoordinates.get(j + 1), lonCoordinates.get(j + 1)))) {
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