/*
 * Copyright 2013 Ciheul Engineering
 */

package com.ciheul.bigmaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.util.GeoPoint;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import com.ciheul.bigmaps.data.BigBikeContentProvider;
import com.ciheul.bigmaps.data.BigBikeDatabaseHelper;
import com.ciheul.bigmaps.data.ShelterModel;
import com.ciheul.bigmaps.util.GPSTracker;

public class MapsApplication extends Application {

    public final boolean DEBUG = false;

    /** Current user's location */
    private double currentLongitude;
    private double currentLatitude;

    private List<ShelterModel> shelters;

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
    public HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>> structuredMapKelurahan;
    public HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>> structuredMapKecamatan;
    public HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>> selectedMap;

    private static final int KELURAHAN = 0;
    private static final int KECAMATAN = 1;

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

        if (structuredMapKelurahan == null) {
            // Log.d("BigMaps", "structuredMap is null");
            structuredMapKelurahan = new HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>>();
            createStructuredMapFromGeoJSON("kelurahan_kota_bandung.json", KELURAHAN);
        }

        if (structuredMapKecamatan == null) {
            // Log.d("BigMaps", "structuredMap is null");
            structuredMapKecamatan = new HashMap<String, ArrayList<HashMap<String, ArrayList<Double>>>>();
            createStructuredMapFromGeoJSON("kecamatan_kota_bandung.json", KECAMATAN);
        }

        insertSampleData();
        shelters = queryShelters();
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

    private void createStructuredMapFromGeoJSON(final String filename, final int MAP_DATA_TYPE) {
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
                        extractRegionJSONArray(regions.getJSONObject(i), MAP_DATA_TYPE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void extractRegionJSONArray(final JSONObject region, final int MAP_DATA_TYPE) {

        // ArrayList<Double> coordinates;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // get region name
                    String regionName = "";
                    if (MAP_DATA_TYPE == KELURAHAN) {
                        regionName = region.getJSONObject("properties").getString("Kelurahan");
                    } else if (MAP_DATA_TYPE == KECAMATAN) {
                        regionName = region.getJSONObject("properties").getString("Kecamatan");
                    }

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

                    if (MAP_DATA_TYPE == KELURAHAN) {
                        structuredMapKelurahan.put(regionName, valuesStructuredMap);
                    } else if (MAP_DATA_TYPE == KECAMATAN) {
                        structuredMapKecamatan.put(regionName, valuesStructuredMap);
                    }
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

    /********************/
    /** DEBUGGING ONLY **/
    /********************/

    private List<ShelterModel> queryShelters() {
        List<ShelterModel> shelters = new ArrayList<ShelterModel>();

        String[] projection = { BigBikeDatabaseHelper.COL_SHELTER_ID, BigBikeDatabaseHelper.COL_SHELTER_NAME,
                BigBikeDatabaseHelper.COL_CAPACITY, BigBikeDatabaseHelper.COL_LON, BigBikeDatabaseHelper.COL_LAT };
        Cursor cursor = getContentResolver().query(BigBikeContentProvider.SHELTER_CONTENT_URI, projection, null, null,
                null);

        Location currentLocation = new Location("");
        currentLocation.setLongitude(currentLongitude);
        currentLocation.setLatitude(currentLatitude);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                ShelterModel shelter = new ShelterModel();
                shelter.setId(cursor.getInt(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_SHELTER_ID)));
                shelter.setName(cursor.getString(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_SHELTER_NAME)));
                shelter.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_CAPACITY)));

                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_LON));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_LAT));
                shelter.setLongitude(longitude);
                shelter.setLatitude(latitude);

                Location targetLocation = new Location("");
                targetLocation.setLongitude(longitude);
                targetLocation.setLatitude(latitude);
                shelter.setDistance(currentLocation.distanceTo(targetLocation) / 1000);

                shelters.add(shelter);
            }
        }

        Collections.sort(shelters);

        return shelters;
    }

    public ArrayList<ExtendedOverlayItem> getListShelterMarker() {
        ArrayList<ShelterModel> listShelter = (ArrayList<ShelterModel>) getShelters();

        ArrayList<ExtendedOverlayItem> listMarker = new ArrayList<ExtendedOverlayItem>();
        for (ShelterModel shelter : listShelter) {
            ExtendedOverlayItem extendedOverlay = new ExtendedOverlayItem(shelter.getName(), "Kapasitas: "
                    + shelter.getCapacity(), new GeoPoint(shelter.getLatitude(), shelter.getLongitude()), null);
            Drawable markerShelter = getResources().getDrawable(R.drawable.marker_blue_32x32);
            extendedOverlay.setMarker(markerShelter);
            listMarker.add(extendedOverlay);
        }

        return listMarker;
    }

    public List<ShelterModel> getShelters() {
        return shelters;
    }

    private void insertSampleData() {
        List<ContentValues> listValues = new ArrayList<ContentValues>();

        ContentValues values = new ContentValues();
        values.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Kafe Halaman");
        values.put(BigBikeDatabaseHelper.COL_CAPACITY, 9);
        values.put(BigBikeDatabaseHelper.COL_LON, 107.61117);
        values.put(BigBikeDatabaseHelper.COL_LAT, -6.88510);
        values.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values);

        ContentValues values2 = new ContentValues();
        values2.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Fly Over Pasupati");
        values2.put(BigBikeDatabaseHelper.COL_CAPACITY, 0);
        values2.put(BigBikeDatabaseHelper.COL_LON, 107.61246);
        values2.put(BigBikeDatabaseHelper.COL_LAT, -6.89848);
        values2.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values2);

        ContentValues values3 = new ContentValues();
        values3.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Kebun Binatang Tamansari");
        values3.put(BigBikeDatabaseHelper.COL_CAPACITY, 3);
        values3.put(BigBikeDatabaseHelper.COL_LON, 107.60812);
        values3.put(BigBikeDatabaseHelper.COL_LAT, -6.89341);
        values3.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values3);

        ContentValues values4 = new ContentValues();
        values4.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Teuku Umar");
        values4.put(BigBikeDatabaseHelper.COL_CAPACITY, 5);
        values4.put(BigBikeDatabaseHelper.COL_LON, 107.61366);
        values4.put(BigBikeDatabaseHelper.COL_LAT, -6.89124);
        values4.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values4);

        ContentValues values5 = new ContentValues();
        values5.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Monumen Pahlawan");
        values5.put(BigBikeDatabaseHelper.COL_CAPACITY, 11);
        values5.put(BigBikeDatabaseHelper.COL_LON, 107.61829);
        values5.put(BigBikeDatabaseHelper.COL_LAT, -6.89286);
        values5.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values5);

        ContentValues values6 = new ContentValues();
        values6.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Gasibu");
        values6.put(BigBikeDatabaseHelper.COL_CAPACITY, 12);
        values6.put(BigBikeDatabaseHelper.COL_LON, 107.61859);
        values6.put(BigBikeDatabaseHelper.COL_LAT, -6.90010);
        values6.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values6);

        ContentValues values7 = new ContentValues();
        values7.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Taman Flexi Dago");
        values7.put(BigBikeDatabaseHelper.COL_CAPACITY, 4);
        values7.put(BigBikeDatabaseHelper.COL_LON, 107.61072);
        values7.put(BigBikeDatabaseHelper.COL_LAT, -6.90391);
        values7.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values7);

        ContentValues values8 = new ContentValues();
        values8.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Cibeunying Park");
        values8.put(BigBikeDatabaseHelper.COL_CAPACITY, 0);
        values8.put(BigBikeDatabaseHelper.COL_LON, 107.62437);
        values8.put(BigBikeDatabaseHelper.COL_LAT, -6.90523);
        values8.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values8);

        ContentValues values9 = new ContentValues();
        values9.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Taman Pramuka");
        values9.put(BigBikeDatabaseHelper.COL_CAPACITY, 0);
        values9.put(BigBikeDatabaseHelper.COL_LON, 107.62688);
        values9.put(BigBikeDatabaseHelper.COL_LAT, -6.91028);
        values9.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values9);

        ContentValues values10 = new ContentValues();
        values10.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Kantor Pos Indonesia");
        values10.put(BigBikeDatabaseHelper.COL_CAPACITY, 12);
        values10.put(BigBikeDatabaseHelper.COL_LON, 107.61739);
        values10.put(BigBikeDatabaseHelper.COL_LAT, -6.90694);
        values10.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values10);

        ContentValues values11 = new ContentValues();
        values11.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Grapari Dago");
        values11.put(BigBikeDatabaseHelper.COL_CAPACITY, 15);
        values11.put(BigBikeDatabaseHelper.COL_LON, 107.61542);
        values11.put(BigBikeDatabaseHelper.COL_LAT, -6.88273);
        values11.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values11);

        ContentValues values12 = new ContentValues();
        values12.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Indomaret Tubagus Ismail Raya");
        values12.put(BigBikeDatabaseHelper.COL_CAPACITY, 0);
        values12.put(BigBikeDatabaseHelper.COL_LON, 107.61872);
        values12.put(BigBikeDatabaseHelper.COL_LAT, -6.88516);
        values12.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values12);

        ContentValues values13 = new ContentValues();
        values13.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "RM. Ampera Tubagus Ismail Raya");
        values13.put(BigBikeDatabaseHelper.COL_CAPACITY, 2);
        values13.put(BigBikeDatabaseHelper.COL_LON, 107.62261);
        values13.put(BigBikeDatabaseHelper.COL_LAT, -6.88591);
        values13.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values13);

        ContentValues values14 = new ContentValues();
        values14.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Elok Market Cigadung");
        values14.put(BigBikeDatabaseHelper.COL_CAPACITY, 7);
        values14.put(BigBikeDatabaseHelper.COL_LON, 107.62490);
        values14.put(BigBikeDatabaseHelper.COL_LAT, -6.88036);
        values14.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values14);

        ContentValues values15 = new ContentValues();
        values15.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Lapangan Tenis Cigadung");
        values15.put(BigBikeDatabaseHelper.COL_CAPACITY, 2);
        values15.put(BigBikeDatabaseHelper.COL_LON, 107.62225);
        values15.put(BigBikeDatabaseHelper.COL_LAT, -6.87211);
        values15.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values15);

        ContentValues values16 = new ContentValues();
        values16.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Borma Dago");
        values16.put(BigBikeDatabaseHelper.COL_CAPACITY, 1);
        values16.put(BigBikeDatabaseHelper.COL_LON, 107.61792);
        values16.put(BigBikeDatabaseHelper.COL_LAT, -6.87695);
        values16.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values16);

        ContentValues values17 = new ContentValues();
        values17.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Dago Pojok");
        values17.put(BigBikeDatabaseHelper.COL_CAPACITY, 8);
        values17.put(BigBikeDatabaseHelper.COL_LON, 107.61899);
        values17.put(BigBikeDatabaseHelper.COL_LAT, -6.87419);
        values17.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values17);

        ContentValues values18 = new ContentValues();
        values18.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Taman Budaya Jawa Barat");
        values18.put(BigBikeDatabaseHelper.COL_CAPACITY, 4);
        values18.put(BigBikeDatabaseHelper.COL_LON, 107.62009);
        values18.put(BigBikeDatabaseHelper.COL_LAT, -6.87018);
        values18.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values18);

        ContentValues values19 = new ContentValues();
        values19.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Terminal Dago");
        values19.put(BigBikeDatabaseHelper.COL_CAPACITY, 9);
        values19.put(BigBikeDatabaseHelper.COL_LON, 107.62114);
        values19.put(BigBikeDatabaseHelper.COL_LAT, -6.86689);
        values19.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values19);

        ContentValues values20 = new ContentValues();
        values20.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Dago Atas 1");
        values20.put(BigBikeDatabaseHelper.COL_CAPACITY, 7);
        values20.put(BigBikeDatabaseHelper.COL_LON, 107.62549);
        values20.put(BigBikeDatabaseHelper.COL_LAT, -6.86316);
        values20.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values20);

        ContentValues values21 = new ContentValues();
        values21.put(BigBikeDatabaseHelper.COL_SHELTER_NAME, "Cibeunying Kolot");
        values21.put(BigBikeDatabaseHelper.COL_CAPACITY, 0);
        values21.put(BigBikeDatabaseHelper.COL_LON, 107.62647);
        values21.put(BigBikeDatabaseHelper.COL_LAT, -6.88660);
        values21.put(BigBikeDatabaseHelper.COL_UPDATED_AT, "2013-09-10T15:03:31.511158");
        listValues.add(values21);

        for (ContentValues value : listValues) {
            getContentResolver().insert(BigBikeContentProvider.SHELTER_CONTENT_URI, value);
        }
    }
}
