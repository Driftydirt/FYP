package com.example.fyp;

public class CustomWeightedLatLng {
    private double lat;
    private double lng;
    private int intensity;

    public CustomWeightedLatLng(double lat, double lng, int intensity) {
        this.lat = lat;
        this.lng = lng;
        this.intensity = intensity;
    }

    public CustomWeightedLatLng() {
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

}
