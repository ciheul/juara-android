package com.ciheul.bigmaps;

import android.app.Application;
import android.util.Log;

import com.ciheul.bigmaps.util.GPSTracker;

public class MapsApplication extends Application {

    private double currentLongitude;
    private double currentLatitude;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("BigMaps", "MapsApplication: onCreate");

        GPSTracker gps = new GPSTracker(this);

        Log.d("BigMaps", gps.getLongitude() + ", " + gps.getLatitude());
        if (gps.canGetLocation() && gps.getLongitude() != 0.0 && gps.getLatitude() != 0.0) {
            Log.d("BigMaps", "yes");
            setCurrentLongitude(gps.getLongitude());
            setCurrentLatitude(gps.getLatitude());
        } else {
            Log.d("BigMaps", "no");
            // gps.showSettingsAlert();
            setCurrentLongitude(107.61226);
            setCurrentLatitude(-6.89848);
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

}
