/*
 * Copyright 2013 Ciheul Engineering
 */

// Links:
// https://code.google.com/p/osmdroid/issues/detail?id=392

package com.ciheul.bigmaps.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
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
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.ciheul.bigmaps.MapsApplication;
import com.ciheul.bigmaps.R;
import com.ciheul.bigmaps.activity.SearchFragment.OnSelectedSearchResultListener;
import com.ciheul.bigmaps.data.JuaraContentProvider;
import com.ciheul.bigmaps.data.JuaraDatabaseHelper;
import com.ciheul.bigmaps.extend.ViaPointInfoWindow;
import com.ciheul.bigmaps.util.FoursquareHandler;

public class MapActivity extends Activity implements MapEventsReceiver, OnSelectedSearchResultListener {

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
    private ItemizedOverlay<OverlayItem> currentUserItemizedOverlay;
    private ItemizedOverlayWithBubble<ExtendedOverlayItem> itemizedOverlayBubble;
    // private DirectedLocationOverlay currentUser;
    // private ScaleBarOverlay scaleBarOverlay;
    // private MinimapOverlay miniMapOverlay;

    private ArrayList<ExtendedOverlayItem> listMarker;

    private TextView tvRegionName;
    private ImageView imgZoomIn;
    private ImageView imgZoomOut;
    private SearchView searchView;
    // private Button btnTrackCurrentUser;

    private ArrayList<PathOverlay> prevPathOverlay;
    private ArrayList<PathOverlay> choroplethOverlay;

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    // the number is decided empirically. the range between 8-12 gives optimal result
    private final static int NUM_THREADS = 9;

    private FoursquareHandler foursquareHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d("BigMaps", "MapsActivity: onCreate");

        app = (MapsApplication) getApplication();

        prefs = getPreferences(MODE_PRIVATE);
        prefsEditor = prefs.edit();

        foursquareHandler = new FoursquareHandler();

        /***********************/
        /** Navigation Drawer **/
        /***********************/

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

        /*********/
        /** Map **/
        /*********/

        resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        // mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(15);

        // add shelters' marker to map
        listMarker = app.getListShelterMarker();

        itemizedOverlayBubble = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(getApplicationContext(), listMarker,
                mapView, new ViaPointInfoWindow(getApplicationContext(), R.layout.bonuspack_bubble, mapView));
        // mapView.getOverlays().add(itemizedOverlayBubble);

        // btnTrackCurrentUser = (Button) findViewById(R.id.buttonTrackingMode);
        // btnTrackCurrentUser.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View view) {
        // mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));
        // mapController.setZoom(15);
        // // addUserMarker();
        // }
        // });

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

        tvRegionName = (TextView) findViewById(R.id.tvRegionName);

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

        Log.d("BigMaps", prefs.getString("currentMapMode", "xxx"));
        Log.d("BigMaps", prefs.getString("currentRegionTapped", "yyy"));

        // addUserMarker();
        restorePreviousCenterState();

        selectItem(-1, prefs.getInt("currentDrawerSelectedPosition", -1));

        if (!prefs.getString("currentRegionTapped", "None").equals("None")) {
            showRegionName(prefs.getString("currentRegionTapped", "None"));
        }

        invalidateOptionsMenu();
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
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("BigMaps", "MapsActivity: onBackPressed");
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

    /***********************/
    /** NAVIGATION DRAWER **/
    /***********************/

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            prefsEditor.putString("currentRegionTapped", "None").commit();
            selectItem(prefs.getInt("currentDrawerSelectedPosition", -1), position);
        }
    }

    private void selectItem(int prevPosition, int position) {
        if (position == -1) {
            return;
        }

        tvRegionName.setVisibility(View.INVISIBLE);
        String feature = getResources().getStringArray(R.array.features_array)[position];

        if (position == prevPosition) {
            if (feature.equals("Kelurahan") || feature.equals("Kecamatan")) {
                mapView.getOverlays().clear();
                // addUserMarker();
                mapView.invalidate();
                prefsEditor.putString("currentMapMode", "None").commit();
            }

            drawerList.setItemChecked(position, false);
            setTitle(getTitle());
            prefsEditor.putInt("currentDrawerSelectedPosition", -1).commit();
        } else {
            itemizedOverlayBubble.hideBubble();
            mapView.getOverlays().clear();
            mapView.getOverlays().add(eventsOverlay);

            if (feature.equals("Kecamatan")) {
                drawChoropleth(prefs.getString("currentMapMode", "None"));
                prefsEditor.putBoolean("Kecamatan", true).commit();
            } else if (feature.equals("Kelurahan")) {
                mapController.setZoom(12);
                mapController.setCenter(new GeoPoint(app.bandungLatitude, app.bandungLongitude));

                for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMapKelurahan
                        .entrySet()) {
                    new FastChoroplethTask("None").execute(entry.getValue());
                }

                prefsEditor.putString("currentMapMode", "None").commit();
                prefsEditor.putBoolean("Kelurahan", true).commit();
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
                // addUserMarker();

                mapController.setZoom(15);
                mapView.getController().animateTo(new GeoPoint(app.getCurrentLatitude(), app.getCurrentLongitude()));

                Toast.makeText(this, "Direktori Bandung feature is coming soon!", Toast.LENGTH_SHORT).show();
            }

            mapView.invalidate();
            drawerList.setItemChecked(position, true);
            setTitle(featureTitles[position]);
            prefsEditor.putInt("currentDrawerSelectedPosition", position).commit();
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

    /****************/
    /** ACTION BAR **/
    /****************/

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("BigMaps", "onPrepareOptionsMenu");
        // If the nav drawer is open, hide action items related to the content view or the drawer is not selected
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);

        menu.findItem(R.id.action_choropleth).setVisible(!drawerOpen);
        menu.findItem(R.id.action_region_list).setVisible(!drawerOpen);
        menu.findItem(R.id.action_search).setVisible(!drawerOpen);

        if (this.title.equals(getTitle())) {
            menu.findItem(R.id.action_choropleth).setVisible(false);
            menu.findItem(R.id.action_region_list).setVisible(false);
            menu.findItem(R.id.action_search).setVisible(true);
        } else if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
            menu.findItem(R.id.action_choropleth).setVisible(true);
            menu.findItem(R.id.action_region_list).setVisible(true);
            menu.findItem(R.id.action_search).setVisible(false);
        } else if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KELURAHAN) {
            menu.findItem(R.id.action_choropleth).setVisible(false);
            menu.findItem(R.id.action_region_list).setVisible(true);
            menu.findItem(R.id.action_search).setVisible(false);
        }

        if (prefs.getBoolean("isSearchViewExpanded", false)) {
            menu.findItem(R.id.action_region_list).collapseActionView();
            prefsEditor.putBoolean("isSearchViewExpanded", false).commit();
        }
        // if (menu.findItem(R.id.action_region_list).isActionViewExpanded()) {
        // menu.findItem(R.id.action_region_list).collapseActionView();
        // }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("BigMaps", "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);

        if (searchView == null) {
            Log.d("BigMaps", "searchView is null");

            searchView = (SearchView) menu.findItem(R.id.action_region_list).getActionView();
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        return true;
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

        case R.id.action_choropleth:
            final String[] stringArrayChoropleth = { "Jumlah Penduduk", "Luas Area", "Kepadatan Penduduk", "None" };

            AlertDialog.Builder choroplethBuilder = new AlertDialog.Builder(this);
            choroplethBuilder.setTitle("Pilih Peta Berdasarkan");

            choroplethBuilder.setItems(stringArrayChoropleth, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int position) {
                    tvRegionName.setVisibility(View.INVISIBLE);

                    clearHighlightedRegion();

                    drawChoropleth(stringArrayChoropleth[position]);
                }
            });

            AlertDialog choroplethDialog = choroplethBuilder.create();
            choroplethDialog.show();

            return true;

        case R.id.action_region_list:
            FragmentManager fm = getFragmentManager();

            // if (fm.findFragmentById(android.R.id.content) == null) {
            SearchFragment fragment = new SearchFragment();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(android.R.id.content, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d("BigMaps", "fm. content. null");
            // } else {
            // Log.d("BigMaps", "fm. content. not null");
            // }
            return true;

        case R.id.action_search:
            HashMap<String, Object> params = new HashMap<String, Object>();
            double lon = mapView.getMapCenter().getLongitudeE6() / 1E6;
            double lat = mapView.getMapCenter().getLatitudeE6() / 1E6;
            String latLon = lat + "," + lon;
            params.put("ll", latLon);
            foursquareHandler.searchVenue(params);
            Toast.makeText(this, "searching venues...", Toast.LENGTH_SHORT).show();
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /*********/
    /** MAP **/
    /*********/

    @Override
    public boolean singleTapUpHelper(IGeoPoint tap) {

        // this one is for bike sharing. useless for a time being.
        if (prefs.getInt("currentDrawerSelectedPosition", -1) == -999) {
            itemizedOverlayBubble.hideBubble();
        }
        // TODO investigate whether accessing the attribute directly is faster than using getter method
        else if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN
                || prefs.getInt("currentDrawerSelectedPosition", -1) == app.KELURAHAN) {

            if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
                app.selectedMap = app.structuredMapKecamatan;
            } else if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KELURAHAN) {
                app.selectedMap = app.structuredMapKelurahan;
            }

            if (app.selectedMap != null) {
                String regionTapped = null;

                ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
                HashMap<String, Future<Boolean>> set = new HashMap<String, Future<Boolean>>();

                // prepare
                for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.selectedMap
                        .entrySet()) {
                    Callable<Boolean> callable = null;
                    int size = entry.getValue().size();
                    if (size == 1) {
                        callable = new IsPointInPolygonCallable(tap, entry.getValue().get(0).get("lon"), entry
                                .getValue().get(0).get("lat"));
                    } else {
                        ArrayList<Double> lonCoordinates = new ArrayList<Double>();
                        ArrayList<Double> latCoordinates = new ArrayList<Double>();
                        for (int i = 0; i < size; i++) {
                            lonCoordinates.addAll(entry.getValue().get(i).get("lon"));
                            latCoordinates.addAll(entry.getValue().get(i).get("lat"));
                        }
                        callable = new IsPointInPolygonCallable(tap, lonCoordinates, latCoordinates);
                    }

                    Future<Boolean> future = pool.submit(callable);
                    set.put(entry.getKey(), future);
                }

                // get which region user tapping, otherwise no region is tapped
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

                // when a region is tapped, highlight
                if (regionTapped != null) {
                    clearHighlightedRegion();
                    highlightTappedRegion(regionTapped);
                    showRegionName(regionTapped);
                } else {
                    // when user tapping no region
                    clearHighlightedRegion();
                    tvRegionName.setVisibility(View.INVISIBLE);
                    prefsEditor.putString("currentRegionTapped", "None").commit();
                }
            }
        }

        return false;
    }

    @Override
    public boolean longPressHelper(IGeoPoint tap) {
        return false;
    }

    private void highlightTappedRegion(String regionName) {
        // thicken the border line
        Paint stroke = new Paint();
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(Color.parseColor("#666666"));
        stroke.setStrokeWidth(6);

        Paint fillRegion = new Paint();
        fillRegion.setStyle(Paint.Style.FILL);
        fillRegion.setColor(Color.YELLOW);

        // TODO not elegant
        if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
            app.selectedMap = app.structuredMapKecamatan;
        } else if (prefs.getInt("currentDrawerSelectedPosition", -1) == app.KELURAHAN) {
            app.selectedMap = app.structuredMapKelurahan;
        }

        ArrayList<HashMap<String, ArrayList<Double>>> borders = app.selectedMap.get(regionName);
        for (int i = 0; i < borders.size(); i++) {
            PathOverlay borderOverlay = new PathOverlay(Color.RED, getApplicationContext());
            borderOverlay.setPaint(stroke);

            PathOverlay regionOverlay = new PathOverlay(Color.RED, getApplicationContext());
            regionOverlay.setPaint(fillRegion);

            HashMap<String, ArrayList<Double>> lonOrLatCoordinates = borders.get(i);
            ArrayList<Double> lonCoordinates = lonOrLatCoordinates.get("lon");
            ArrayList<Double> latCoordinates = lonOrLatCoordinates.get("lat");
            int size = lonCoordinates.size();

            for (int j = 0; j < size; j++) {
                borderOverlay.addPoint(new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)));
                regionOverlay.addPoint(new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)));
            }

            mapView.getOverlays().add(borderOverlay);
            mapView.getOverlays().add(regionOverlay);

            mapView.invalidate();

            // retain the border information
            prevPathOverlay.add(borderOverlay);
            prevPathOverlay.add(regionOverlay);
        }

        prefsEditor.putString("currentRegionTapped", regionName).commit();
    }

    private void clearHighlightedRegion() {
        // TODO not elegant
        if (!prefs.getString("currentRegionTapped", "None").equals("None")) {
            for (int i = 0; i < prevPathOverlay.size(); i++) {
                mapView.getOverlays().remove(prevPathOverlay.get(i));
            }
            prevPathOverlay.clear();
            mapView.invalidate();
            prefsEditor.putString("currentRegionTapped", "None").commit();
        }
    }

    private void showRegionName(String regionName) {
        String additionalInfo = "";

        if (prefs.getString("currentMapMode", "None").equals("Jumlah Penduduk")
                && prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
            String[] projection = { JuaraDatabaseHelper.COL_POPULATION_MALE, JuaraDatabaseHelper.COL_POPULATION_FEMALE };
            String selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME + "='" + regionName + "'";
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    selection, null, null);

            if (cursor != null) {
                cursor.moveToFirst();
                int male = cursor.getInt(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_MALE));
                int female = cursor.getInt(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_FEMALE));
                int total = male + female;

                additionalInfo = " = " + total + " penduduk";
            }
            cursor.close();
        } else if (prefs.getString("currentMapMode", "None").equals("Luas Area")
                && prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
            String[] projection = { JuaraDatabaseHelper.COL_LAND_AREA };
            String selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME + "='" + regionName + "'";
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    selection, null, null);

            if (cursor != null) {
                cursor.moveToFirst();
                double landArea = cursor.getDouble(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_LAND_AREA));

                additionalInfo = " = " + landArea + " km2";
            }
            cursor.close();
        }
        if (prefs.getString("currentMapMode", "None").equals("Kepadatan Penduduk")
                && prefs.getInt("currentDrawerSelectedPosition", -1) == app.KECAMATAN) {
            String[] projection = { JuaraDatabaseHelper.COL_POPULATION_DENSITY };
            String selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME + "='" + regionName + "'";
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    selection, null, null);

            if (cursor != null) {
                cursor.moveToFirst();
                int density = cursor.getInt(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_DENSITY));

                additionalInfo = " = " + density + " penduduk/km2";
            }
            cursor.close();
        }

        tvRegionName.setText(regionName + additionalInfo);
        tvRegionName.setVisibility(View.VISIBLE);
    }

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

    /************************/
    /** CHOROPLETH METHODS **/
    /************************/

    private class FastChoroplethTask extends AsyncTask<ArrayList, Void, Void> {

        private String choroplethMode;
        private int var;
        private double varD;

        public FastChoroplethTask(String choroplethMode) {
            this.choroplethMode = choroplethMode;
        }

        public FastChoroplethTask(String choroplethMode, int var) {
            this.choroplethMode = choroplethMode;
            this.var = var;
        }

        public FastChoroplethTask(String choroplethMode, double varD) {
            this.choroplethMode = choroplethMode;
            this.varD = varD;
        }

        @Override
        protected Void doInBackground(ArrayList... arg) {
            ArrayList<HashMap<String, ArrayList<Double>>> borders = arg[0];

            PathOverlay regionOverlay = new PathOverlay(Color.RED, getApplicationContext());

            Paint fillRegion = new Paint();
            fillRegion.setStyle(Paint.Style.FILL);
            fillRegion.setAlpha(200);

            Paint stroke = new Paint();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(Color.parseColor("#666666"));
            stroke.setStrokeWidth(2);

            // iterate all borders
            for (int i = 0; i < borders.size(); i++) {
                HashMap<String, ArrayList<Double>> lonOrLatCoordinates = borders.get(i);

                PathOverlay borderOverlay = new PathOverlay(Color.RED, getApplicationContext());
                borderOverlay.setPaint(stroke);

                if (choroplethMode.equals("None")) {
                    fillRegion.setColor(Color.parseColor("#00aef0"));
                } else if (choroplethMode.equals("Jumlah Penduduk")) {
                    fillRegion.setColor(getColorPopulation(var));
                } else if (choroplethMode.equals("Luas Area")) {
                    fillRegion.setColor(getColorLandArea(varD));
                } else if (choroplethMode.equals("Kepadatan Penduduk")) {
                    fillRegion.setColor(getColorDensity(varD));
                }
                regionOverlay.setPaint(fillRegion);

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

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!prefs.getString("currentRegionTapped", "None").equals("None")) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt("increment", 1);
                message.setData(bundle);
                highlightRegionHandler.sendMessage(message);
            }

            // update UI asynchronously
            mapView.invalidate();
        }
    }

    private void drawChoropleth(String mapMode) {
        mapController.setZoom(12);
        mapView.getOverlays().clear();
        mapView.getOverlays().add(eventsOverlay);

        if (mapMode.equals("Jumlah Penduduk")) {
            String[] projection = { JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME,
                    JuaraDatabaseHelper.COL_POPULATION_MALE, JuaraDatabaseHelper.COL_POPULATION_FEMALE };
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    null, null, null);

            HashMap<String, Integer> regionPopulation = new HashMap<String, Integer>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor
                            .getColumnIndexOrThrow(JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME));
                    int male = cursor.getInt(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_MALE));
                    int female = cursor.getInt(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_FEMALE));
                    int total = male + female;
                    regionPopulation.put(name, total);
                }
            }
            cursor.close();

            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMapKecamatan
                    .entrySet()) {
                int totalPopulation = -1;
                if (regionPopulation.containsKey(entry.getKey())) {
                    totalPopulation = regionPopulation.get(entry.getKey());
                }

                new FastChoroplethTask("Jumlah Penduduk", totalPopulation).execute(entry.getValue());
            }
        } else if (mapMode.equals("Luas Area")) {
            String[] projection = { JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME, JuaraDatabaseHelper.COL_LAND_AREA };
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    null, null, null);

            HashMap<String, Double> regionLandArea = new HashMap<String, Double>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor
                            .getColumnIndexOrThrow(JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME));
                    double landArea = cursor.getDouble(cursor.getColumnIndexOrThrow(JuaraDatabaseHelper.COL_LAND_AREA));
                    regionLandArea.put(name, landArea);
                }
            }
            cursor.close();

            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMapKecamatan
                    .entrySet()) {
                double landArea = -1;
                if (regionLandArea.containsKey(entry.getKey())) {
                    landArea = regionLandArea.get(entry.getKey());
                }

                new FastChoroplethTask("Luas Area", landArea).execute(entry.getValue());
            }
        } else if (mapMode.equals("Kepadatan Penduduk")) {
            String[] projection = { JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME,
                    JuaraDatabaseHelper.COL_POPULATION_DENSITY };
            Cursor cursor = getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI, projection,
                    null, null, null);

            HashMap<String, Double> regionDensity = new HashMap<String, Double>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor
                            .getColumnIndexOrThrow(JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME));
                    double density = cursor.getDouble(cursor
                            .getColumnIndexOrThrow(JuaraDatabaseHelper.COL_POPULATION_DENSITY));
                    regionDensity.put(name, density);
                }
            }
            cursor.close();

            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMapKecamatan
                    .entrySet()) {
                double density = -1;
                if (regionDensity.containsKey(entry.getKey())) {
                    density = regionDensity.get(entry.getKey());
                }

                new FastChoroplethTask("Kepadatan Penduduk", density).execute(entry.getValue());
            }
        }

        else {
            for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : app.structuredMapKecamatan
                    .entrySet()) {
                new FastChoroplethTask("None").execute(entry.getValue());
            }
        }

        prefsEditor.putString("currentMapMode", mapMode).commit();
    }

    private final HighlightRegionHandler highlightRegionHandler = new HighlightRegionHandler(this);

    private static class HighlightRegionHandler extends Handler {
        private final WeakReference<MapActivity> mActivity;
        private int counter = 0;

        public HighlightRegionHandler(MapActivity activity) {
            this.mActivity = new WeakReference<MapActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MapActivity activity = mActivity.get();
            int increment = msg.getData().getInt("increment");
            counter += increment;
            // Log.d("BigMaps", String.valueOf(counter));

            if (activity.prefs.getInt("currentDrawerSelectedPosition", -1) == activity.app.KECAMATAN) {
                activity.app.selectedMap = activity.app.structuredMapKecamatan;
            } else if (activity.prefs.getInt("currentDrawerSelectedPosition", -1) == activity.app.KELURAHAN) {
                activity.app.selectedMap = activity.app.structuredMapKelurahan;
            }

            if (counter == activity.app.selectedMap.size()) {
                activity.highlightTappedRegion(activity.prefs.getString("currentRegionTapped", "None"));

                // reset
                counter = 0;
            }
        }
    }

    private class IsPointInPolygonCallable implements Callable<Boolean> {
        private ArrayList<Double> lonCoordinates;
        private ArrayList<Double> latCoordinates;
        private IGeoPoint tap;

        private IsPointInPolygonCallable(IGeoPoint tap, ArrayList<Double> lonCoordinates,
                ArrayList<Double> latCoordinates) {
            this.tap = tap;
            this.lonCoordinates = lonCoordinates;
            this.latCoordinates = latCoordinates;
        }

        @Override
        public Boolean call() throws Exception {
            if (isPointInPolygon(tap, lonCoordinates, latCoordinates)) {
                return true;
            }

            return false;
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

        // odd = inside, even = outside;
        return ((intersectCount % 2) == 1);
    }

    private boolean rayCastIntersect(IGeoPoint tap, GeoPoint vertA, GeoPoint vertB) {
        double aY = vertA.getLatitudeE6() / 1E6;
        double bY = vertB.getLatitudeE6() / 1E6;
        double aX = vertA.getLongitudeE6() / 1E6;
        double bX = vertB.getLongitudeE6() / 1E6;
        double pY = tap.getLatitudeE6() / 1E6;
        double pX = tap.getLongitudeE6() / 1E6;

        // a and b can't both be above or below pt.y, and a or b must be east of pt.x
        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false;
        }

        double m = (aY - bY) / (aX - bX); // Rise over run
        double bee = (-aX) * m + aY; // y = mx + b
        double x = (pY - bee) / m; // algebra is neat!

        return x > pX;
    }

    private int getColorPopulation(int numOfPopulation) {
        int color = Color.WHITE;

        // no data
        if (numOfPopulation == -1) {
            return Color.BLACK;
        }

        if (numOfPopulation < 25000) {
            color = Color.parseColor("#FFFFB2");
        } else if (numOfPopulation < 50000) {
            color = Color.parseColor("#FECC5C");
        } else if (numOfPopulation < 75000) {
            color = Color.parseColor("#FD8D3C");
        } else if (numOfPopulation < 100000) {
            color = Color.parseColor("#F03B20");
        } else {
            color = Color.parseColor("#BD0026");
        }

        return color;
    }

    private int getColorLandArea(double landArea) {
        int color = Color.WHITE;

        // no data
        if (landArea == -1) {
            return Color.BLACK;
        }

        if (landArea < 2.0) {
            color = Color.parseColor("#EDF8FB");
        } else if (landArea < 4.0) {
            color = Color.parseColor("#B2E2E2");
        } else if (landArea < 6.0) {
            color = Color.parseColor("#66C2A4");
        } else if (landArea < 8.0) {
            color = Color.parseColor("#2CA25F");
        } else {
            color = Color.parseColor("#006D2C");
        }

        return color;
    }

    private int getColorDensity(double density) {
        int color = Color.WHITE;

        // no data
        if (density == -1) {
            return Color.BLACK;
        }

        if (density < 5000.0) {
            color = Color.parseColor("#F1EEF6");
        } else if (density < 10000.0) {
            color = Color.parseColor("#D7B5D8");
        } else if (density < 15000.0) {
            color = Color.parseColor("#DF65B0");
        } else if (density < 20000.0) {
            color = Color.parseColor("#DD1C77");
        } else {
            color = Color.parseColor("#980043");
        }

        return color;
    }

    @Override
    public void onRegionSelected(String regionName, Fragment fragment) {
        Toast.makeText(this, regionName, Toast.LENGTH_SHORT).show();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.remove(fragment).commit();

        clearHighlightedRegion();
        showRegionName(regionName);
        highlightTappedRegion(regionName);
    }
}