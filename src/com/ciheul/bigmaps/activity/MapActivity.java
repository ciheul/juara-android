/*
 * Copyright 2013 Ciheul Engineering
 */

// Links:
// https://code.google.com/p/osmdroid/issues/detail?id=392

package com.ciheul.bigmaps.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.bonuspack.overlays.ItemizedOverlayWithBubble;
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
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.ciheul.bigmaps.MapsApplication;
import com.ciheul.bigmaps.R;
import com.ciheul.bigmaps.extend.ViaPointInfoWindow;

public class MapActivity extends Activity implements MapEventsReceiver {

    private MapsApplication app;

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private CharSequence drawerTitle;
    private CharSequence title;
    private String[] featureTitles;

    private ResourceProxy resourceProxy;
    private MapView mapView;
    private MapController mapController;

    /** Overlay */
    private MapEventsOverlay eventsOverlay;
    // private DirectedLocationOverlay currentUser;
    private ItemizedOverlay<OverlayItem> currentUserItemizedOverlay;
    private ItemizedOverlayWithBubble<ExtendedOverlayItem> itemizedOverlayBubble;
    // private ScaleBarOverlay scaleBarOverlay;
    // private MinimapOverlay miniMapOverlay;

    private ArrayList<ExtendedOverlayItem> listMarker;

    private Button btnTrackCurrentUser;
    private ImageView imgZoomIn;
    private ImageView imgZoomOut;

    /** Map Color */
    private int colorCounter = 0;
    public String[] MapColor = { "#762A83", "#9970AB", "#C2A5CF", "#E7D4E8", "#D9F0D3", "#A6DBA0", "#5AAE61", "#1B7837" };

    private String prevRegionTapped;
    private ArrayList<PathOverlay> prevPathOverlay;
    private ArrayList<PathOverlay> choroplethOverlay;

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        app = (MapsApplication) getApplication();

        prefs = getPreferences(MODE_PRIVATE);
        prefsEditor = prefs.edit();

        Log.d("BigMaps", "MapsActivity: onCreate");

        /** Navigation Drawer */

        title = drawerTitle = getTitle();
        featureTitles = getResources().getStringArray(R.array.features_array);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, featureTitles));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        /** Map */

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(15);

        // add shelters' marker to map
        listMarker = app.getListShelterMarker();
        Log.d("BigMaps", String.valueOf(listMarker.size()));
        itemizedOverlayBubble = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(getApplicationContext(), listMarker,
                mapView, new ViaPointInfoWindow(getApplicationContext(), R.layout.bonuspack_bubble, mapView));
        // mapView.getOverlays().add(itemizedOverlayBubble);

        btnTrackCurrentUser = (Button) findViewById(R.id.buttonTrackingMode);
        btnTrackCurrentUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));
                mapController.setZoom(15);
                addUserMarker();
            }
        });

        prevPathOverlay = new ArrayList<PathOverlay>();
        choroplethOverlay = new ArrayList<PathOverlay>();

        eventsOverlay = new MapEventsOverlay(this, this);
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

    /******************/
    /** MAIN METHODS **/
    /******************/

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BigMaps", "MapsActivity: onResume");

        addUserMarker();
        restorePreviousCenterState();
        selectItem(-1, prefs.getInt("prevDrawerSelectedPosition", -1));
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

    /***********************/
    /** NAVIGATION DRAWER **/
    /***********************/

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        menu.findItem(R.id.action_navigation).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
        case R.id.action_navigation:
            Toast.makeText(this, "Directions feature is coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_websearch:
            Toast.makeText(this, "Search feature is coming soon!", Toast.LENGTH_SHORT).show();
            // create intent to perform web search for this planet
            // Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            // intent.putExtra(SearchManager.QUERY, getActionBar().getTitle());
            // // catch event that there's no activity to handle intent
            // if (intent.resolveActivity(getPackageManager()) != null) {
            // startActivity(intent);
            // } else {
            // Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
            // }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(prefs.getInt("prevDrawerSelectedPosition", -1), position);
        }
    }

    private void selectItem(int prevPosition, int position) {
        // Fragment fragment = new FeatureFragment();
        // Bundle args = new Bundle();
        // args.putInt(FeatureFragment.ARG_FEATURE_NUMBER, position);
        // fragment.setArguments(args);
        //
        // FragmentManager fragmentManager = getFragmentManager();
        // fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        //

        if (position == -1) {
            return;
        }

        String feature = getResources().getStringArray(R.array.features_array)[position];

        if (position == prevPosition) {
            if (feature.equals("Choropleth") || feature.equals("Bike Sharing")) {
                mapView.getOverlays().clear();
                addUserMarker();
                mapView.invalidate();
            }

            drawerList.setItemChecked(position, false);
            setTitle(getTitle());
            prefsEditor.putInt("prevDrawerSelectedPosition", -1).commit();
        } else {
            itemizedOverlayBubble.hideBubble();
            mapView.getOverlays().clear();
            mapView.getOverlays().add(eventsOverlay);

            if (feature.equals("Choropleth")) {
                mapController.setZoom(8);
                
                for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMap
                        .entrySet()) {
                    new FastChoroplethTask().execute(entry.getValue());
                }
                
                prefsEditor.putBoolean("choroplethMap", true).commit();
            } else if (feature.equals("Bike Sharing")) {
                addUserMarker();

                mapController.setZoom(15);
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));

                mapView.getOverlays().add(itemizedOverlayBubble);
            } else if (feature.equals("Traffic")) {
                addUserMarker();

                mapController.setZoom(15);
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));

                Toast.makeText(this, "Traffic feature is coming soon!", Toast.LENGTH_SHORT).show();
            } else if (feature.equals("Direktori Bandung")) {
                addUserMarker();

                mapController.setZoom(15);
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));

                Toast.makeText(this, "Direktori Bandung feature is coming soon!", Toast.LENGTH_SHORT).show();
            }

            mapView.invalidate();
            drawerList.setItemChecked(position, true);
            setTitle(featureTitles[position]);
            prefsEditor.putInt("prevDrawerSelectedPosition", position).commit();

        }
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Fragment that appears in the "content_frame", shows a feature
     */
    public static class FeatureFragment extends Fragment {
        public static final String ARG_FEATURE_NUMBER = "feature_number";

        public FeatureFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_feature, container, false);
            int i = getArguments().getInt(ARG_FEATURE_NUMBER);
            String planet = getResources().getStringArray(R.array.features_array)[i];

            int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()), "drawable",
                    getActivity().getPackageName());
            ((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
            getActivity().setTitle(planet);
            return rootView;
        }
    }

    /************************/
    /** MAPEVENTS RECEIVER **/
    /************************/
    private class IsPointInPolygonCallable implements Callable<Boolean> {

        private ArrayList<Double> lonCoordinates;
        private ArrayList<Double> latCoordinates;
        private IGeoPoint tap;
        private String regionName;

        private IsPointInPolygonCallable(String regionName, IGeoPoint tap, ArrayList<Double> lonCoordinates,
                ArrayList<Double> latCoordinates) {
            this.regionName = regionName;
            this.tap = tap;
            this.lonCoordinates = lonCoordinates;
            this.latCoordinates = latCoordinates;
        }

        @Override
        public Boolean call() throws Exception {
            // long threadId = Thread.currentThread().getId() % NUM_THREADS + 1;

            if (isPointInPolygon(tap, lonCoordinates, latCoordinates)) {
                // Log.d("BigMaps", "threadId: " + String.valueOf(threadId) + " " + regionName + " => true");
                return true;
            }

            // Log.d("BigMaps", "threadId: " + String.valueOf(threadId) + " " + regionName + " =>false");
            return false;
        }
    }

    private final static int NUM_THREADS = 9;

    @Override
    public boolean singleTapUpHelper(IGeoPoint tap) {

        if (prefs.getInt("prevDrawerSelectedPosition", -1) == 1) {
            itemizedOverlayBubble.hideBubble();
        } else if (prefs.getInt("prevDrawerSelectedPosition", -1) == 0) {
            if (app.structuredMap != null) {
                String regionTapped = null;

                ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
                HashMap<String, Future<Boolean>> set = new HashMap<String, Future<Boolean>>();

                long start = System.currentTimeMillis();

                // prepare
                for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMap
                        .entrySet()) {
                    Callable<Boolean> callable = null;
                    int size = entry.getValue().size();
                    if (size == 1) {
                        callable = new IsPointInPolygonCallable(entry.getKey(), tap,
                                entry.getValue().get(0).get("lon"), entry.getValue().get(0).get("lat"));
                    } else {
                        ArrayList<Double> lonCoordinates = new ArrayList<Double>();
                        ArrayList<Double> latCoordinates = new ArrayList<Double>();
                        for (int i = 0; i < size; i++) {
                            lonCoordinates.addAll(entry.getValue().get(i).get("lon"));
                            latCoordinates.addAll(entry.getValue().get(i).get("lat"));
                        }
                        callable = new IsPointInPolygonCallable(entry.getKey(), tap, lonCoordinates, latCoordinates);
                    }

                    Future<Boolean> future = pool.submit(callable);
                    set.put(entry.getKey(), future);
                }

                long premiddle = System.currentTimeMillis();
                long duration0 = premiddle - start;

                if (app.DEBUG) {
                    Log.d("BigMaps", "duration0 = " + String.valueOf(duration0));
                }

                for (Map.Entry<String, Future<Boolean>> entry : set.entrySet()) {
                    try {
                        if (entry.getValue().get() == true) {
                            // the main actor!
                            regionTapped = entry.getKey();

                            pool.shutdown(); // Disable new threads from being submitted
                            pool.shutdownNow(); // Force to stop other threads running

                            // Wait a while for existing tasks to terminate
                            if (!pool.awaitTermination(0, TimeUnit.MICROSECONDS)) {
                                pool.shutdownNow(); // Cancel currently executing tasks
                                // Wait a while for tasks to respond to being cancelled
                                if (!pool.awaitTermination(0, TimeUnit.MICROSECONDS)) {
                                    System.err.println("Pool did not terminate");
                                }
                            }
                            break;
                        }
                    } catch (InterruptedException e) {
                        pool.shutdownNow();
                        // Preserve interrupt status
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                long middle = System.currentTimeMillis();
                long duration1 = middle - premiddle;

                if (app.DEBUG) {
                    Log.d("BigMaps", "duration1 = " + String.valueOf(duration1));
                }

                if (regionTapped != null) {
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

                    long end = System.currentTimeMillis();
                    long duration2 = end - middle;
                    long total = end - start;

                    if (app.DEBUG) {
                        Log.d("BigMaps", "duration2 = " + String.valueOf(duration2));
                        Log.d("BigMaps", "-------------------------");
                        Log.d("BigMaps", "total     = " + String.valueOf(total));

                        Log.d("BigMaps", regionTapped);
                        Log.d("BigMaps", " ");
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean longPressHelper(IGeoPoint tap) {
        return false;
    }

    /*******************/

    private void addUserMarker() {
        // set user's current location and its marker's picture
        OverlayItem currentUserOverlay = new OverlayItem("Current User", "", new GeoPoint(app.getCurrentLatitude(),
                app.getCurrentLongitude()));
        Drawable markerImage = getResources().getDrawable(R.drawable.marker_red_32x32);
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
        mapController.setZoom(prefs.getInt("zoomLevel", 15));
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

    /************************/
    /** CHOROPLETH METHODS **/
    /************************/

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
                choroplethOverlay.add(borderOverlay);
            }

            mapView.getOverlays().add(regionOverlay);
            choroplethOverlay.add(regionOverlay);

            colorCounter += 1;

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // update UI asynchronously
            mapView.invalidate();
        }
    }

    private boolean isPointInPolygon(IGeoPoint tap, final ArrayList<Double> lonCoordinates,
            final ArrayList<Double> latCoordinates) {
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