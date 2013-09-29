package com.ciheul.bigmaps.data;

public class ShelterModel implements Comparable<ShelterModel> {

    private int id;
    private String name;
    private String shelterType;
    private int capacity;
    private double longitude;
    private double latitude;
    private float distance;

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShelterType() {
        return shelterType;
    }

    public void setShelterType(String shelterType) {
        this.shelterType = shelterType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(ShelterModel another) {

        if (this.getDistance() > another.getDistance()) {
            return 1;
        } else if (this.getDistance() < another.getDistance()) {
            return -1;
        } else {
            return 0;
        }
    }

}
