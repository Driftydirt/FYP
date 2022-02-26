package com.example.fyp;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

public class SignalStrengthLocation {
    private final int level;
    private final Location location;
    private final WeightedLatLng weightedLatLng;

    public SignalStrengthLocation(Location location, int level) {
        this.location = location;
        this.level = level;
        this.weightedLatLng = new WeightedLatLng(new LatLng(location.getLatitude(),location.getLongitude()), level);
    }

    public WeightedLatLng getWeightedLatLng() {
        return weightedLatLng;
    }

    public int getLevel() {
        return level;
    }

    public Location getLocation() {
        return location;
    }
}
