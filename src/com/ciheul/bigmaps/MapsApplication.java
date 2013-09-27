/*
 * Copyright 2013 Ciheul Engineering
 */

package com.ciheul.bigmaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.util.Log;

import com.ciheul.bigmaps.util.GPSTracker;

public class MapsApplication extends Application {

    public final boolean DEBUG = true;

    /** Current user's location */
    private double currentLongitude;
    private double currentLatitude;

    // private int NUM_THREADS = 10;
    //
    // private boolean isStructuredMapCreated = false;
    // private boolean isPolygonThreadPoolReady = false;

    /**
     * Data Structure for Map
     * 
     * key: region name value: its border in and out.
     * 
     * According to Android Documentation, a parallel arrays performs faster than a pair of values in an array. Hence,
     * the data structure is designed for such purpose.
     * 
     * regionName -> [{ lon: [x1, x2, x3], lat: [y1, y2, y3] }, { lon: [x1, x2, x3], lat: [y1, y2, y3] }, {}, ...]
     */
    public HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>> structuredMap;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("BigMaps", "MapsApplication: onCreate");

        GPSTracker gps = new GPSTracker(this);

        if (gps.canGetLocation() && gps.getLongitude() != 0.0 && gps.getLatitude() != 0.0) {
            setCurrentLongitude(gps.getLongitude());
            setCurrentLatitude(gps.getLatitude());
        } else {
            // gps.showSettingsAlert();
            setCurrentLongitude(107.61226);
            setCurrentLatitude(-6.89848);
        }

        if (structuredMap == null) {
            Log.d("BigMaps", "structuredMap is null");
            structuredMap = new HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>>();
            createStructuredMapFromGeoJSON("jabar-kabupaten.json");
        }

        // initializeIsPointInPolygon();
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    private int num_of_regions = 0;

    // private int regions_created_counter = 0;

    private void createStructuredMapFromGeoJSON(final String filename) {
        // as the task doesn't need to update the UI and its heavy to construct a data structure, use Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // convert from a text file to a string
                    InputStream inputStream = getAssets().open(filename);
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    inputStream.close();
                    String json = new String(buffer, "UTF-8");

                    final JSONArray regions = new JSONObject(json).getJSONArray("features");
                    num_of_regions = regions.length();

                    for (int i = 0; i < num_of_regions; i++) {
                        extractRegionJSONArray(regions.getJSONObject(i));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void extractRegionJSONArray(final JSONObject region) {

        // ArrayList<Double> coordinates;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // get region name
                    String regionName = region.getJSONObject("properties").getString("NAMA_KAB");

                    // get arrays of borders (in and out)
                    JSONArray borders = region.getJSONObject("geometry").getJSONArray("coordinates");
                    int num_of_borders = borders.length();

                    // the data structure
                    ArrayList<HashMap<String, ArrayList<Double>>> valuesStructuredMap = new ArrayList<HashMap<String, ArrayList<Double>>>();

                    for (int i = 0; i < num_of_borders; i++) {
                        // indicator whether an object is a list or a pair of [lon, lat]
                        boolean isList = false;

                        // {'lon': [x1, x2, x3], 'lat': [y1, y2, y3]}
                        HashMap<String, ArrayList<Double>> lonOrLatCoordinates = new HashMap<String, ArrayList<Double>>();

                        // [x1, x2, x3] or [y1, y2, y3]
                        ArrayList<Double> lonCoordinates = new ArrayList<Double>();
                        ArrayList<Double> latCoordinates = new ArrayList<Double>();

                        JSONArray a = borders.getJSONArray(i);
                        int num_of_elements = a.length();

                        // loop 3: either another list of [lon, lat] or [lon, lat]
                        for (int k = 0; k < num_of_elements; k++) {
                            JSONArray b = a.getJSONArray(k);

                            if (b.length() > 2) {
                                // loop 4: [lon, lat]
                                for (int l = 0; l < b.length(); l++) {
                                    JSONArray c = b.getJSONArray(l);
                                    lonCoordinates.add(c.getDouble(0));
                                    latCoordinates.add(c.getDouble(1));
                                }

                                lonOrLatCoordinates.put("lon", lonCoordinates);
                                lonOrLatCoordinates.put("lat", latCoordinates);

                                isList = true;
                            } else {
                                // [lon, lat]
                                lonCoordinates.add(b.getDouble(0));
                                latCoordinates.add(b.getDouble(1));
                            }
                        }

                        if (isList == false) {
                            lonOrLatCoordinates.put("lon", lonCoordinates);
                            lonOrLatCoordinates.put("lat", latCoordinates);
                        }

                        valuesStructuredMap.add(lonOrLatCoordinates);
                    }
                    structuredMap.put(regionName, valuesStructuredMap);
                    // regions_created_counter += 1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // private class IsPointInPolygonCallable implements Callable<Boolean> {
    //
    // private ArrayList<Double> lonCoordinates;
    // private ArrayList<Double> latCoordinates;
    // private IGeoPoint tap;
    // private String regionName;
    //
    // private IsPointInPolygonCallable(String regionName, ArrayList<Double> lonCoordinates,
    // ArrayList<Double> latCoordinates) {
    // this.regionName = regionName;
    // this.lonCoordinates = lonCoordinates;
    // this.latCoordinates = latCoordinates;
    // }
    //
    // @Override
    // public Boolean call() throws Exception {
    // long threadId = Thread.currentThread().getId() % NUM_THREADS + 1;
    // if (isPointInPolygon(tap, lonCoordinates, latCoordinates)) {
    // // Log.d("BigMaps", "threadId: " + String.valueOf(threadId) + " " + regionName + " => true");
    // return true;
    // }
    // // Log.d("BigMaps", "threadId: " + String.valueOf(threadId) + " " + regionName + " =>false");
    // return false;
    // }
    //
    // }

    // private void initializeIsPointInPolygon() {
    // Log.d("BigMaps", "MapsApplication: initializeIsPointInPolygon()");
    // Log.d("BigMaps", "num_of_regions  = " + String.valueOf(num_of_regions));
    // Log.d("BigMaps", "regions_created = " + String.valueOf(regions_created_counter));
    // new Thread(new Runnable() {
    // @Override
    // public void run() {
    // while (!Thread.currentThread().isInterrupted()) {
    // if (regions_created_counter != 0 && num_of_regions != 0 && regions_created_counter == num_of_regions) {
    // Log.d("BigMaps", "time to create!");
    // isPolygonThreadPoolReady = true;
    // Thread.currentThread().interrupt();
    // } else {
    // Log.d("BigMaps", "it's not the time.");
    // }
    // }
    // }
    // }).start();
    //
    // ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    // HashMap<String, Future<Boolean>> set = new HashMap<String, Future<Boolean>>();
    //
    // long start = System.currentTimeMillis();
    //
    // // prepare
    // for (Map.Entry<String, ArrayList<HashMap<String, ArrayList<Double>>>> entry : structuredMap.entrySet()) {
    // Callable<Boolean> callable = null;
    // int size = entry.getValue().size();
    // if (size == 1) {
    // callable = new IsPointInPolygonCallable(entry.getKey(), entry.getValue().get(0).get("lon"), entry
    // .getValue().get(0).get("lat"));
    // } else {
    // ArrayList<Double> lonCoordinates = new ArrayList<Double>();
    // ArrayList<Double> latCoordinates = new ArrayList<Double>();
    // for (int i = 0; i < size; i++) {
    // lonCoordinates.addAll(entry.getValue().get(i).get("lon"));
    // latCoordinates.addAll(entry.getValue().get(i).get("lat"));
    // }
    // callable = new IsPointInPolygonCallable(entry.getKey(), lonCoordinates, latCoordinates);
    // }
    //
    // Future<Boolean> future = pool.submit(callable);
    // set.put(entry.getKey(), future);
    // }
    // }

    // private boolean isPointInPolygon(IGeoPoint tap, final ArrayList<Double> lonCoordinates,
    // final ArrayList<Double> latCoordinates) {
    // int intersectCount = 0;
    //
    // for (int j = 0; j < lonCoordinates.size() - 1; j++) {
    // if (rayCastIntersect(tap, new GeoPoint(latCoordinates.get(j), lonCoordinates.get(j)), new GeoPoint(
    // latCoordinates.get(j + 1), lonCoordinates.get(j + 1)))) {
    // intersectCount++;
    // }
    // }
    //
    // return ((intersectCount % 2) == 1); // odd = inside, even = outside;
    // }
    //
    // private boolean rayCastIntersect(IGeoPoint tap, GeoPoint vertA, GeoPoint vertB) {
    //
    // double aY = vertA.getLatitudeE6() / 1E6;
    // double bY = vertB.getLatitudeE6() / 1E6;
    // double aX = vertA.getLongitudeE6() / 1E6;
    // double bX = vertB.getLongitudeE6() / 1E6;
    // double pY = tap.getLatitudeE6() / 1E6;
    // double pX = tap.getLongitudeE6() / 1E6;
    //
    // if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
    // return false; // a and b can't both be above or below pt.y, and a or b must be east of pt.x
    // }
    //
    // double m = (aY - bY) / (aX - bX); // Rise over run
    // double bee = (-aX) * m + aY; // y = mx + b
    // double x = (pY - bee) / m; // algebra is neat!
    //
    // return x > pX;
    // }
}
