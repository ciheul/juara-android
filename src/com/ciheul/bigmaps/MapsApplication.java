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

    /** Current user's location */
    private double currentLongitude;
    private double currentLatitude;

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
                    int num_of_regions = regions.length();

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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
