package com.example.fyp;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.Calendar;
import java.util.Date;

public class SignalStrengthLocation {
    private int level;
    private CustomWeightedLatLng customWeightedLatLng;
    private String name;
    private Date creationTime;

    public SignalStrengthLocation(Location location, int level, String name) {
        this.level = level;
        this.name = name;
        this.creationTime = Calendar.getInstance().getTime();
        this.customWeightedLatLng = new CustomWeightedLatLng(location.getLatitude(), location.getLongitude(), level);
    }

    public SignalStrengthLocation() {
    }

    public CustomWeightedLatLng getCustomWeightedLatLng() {
        return customWeightedLatLng;
    }

    public WeightedLatLng generateWeightedLatLng() {
        return new WeightedLatLng(new LatLng(customWeightedLatLng.getLat(), customWeightedLatLng.getLng()), level);
    }

    public int getLevel() {
        return level;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public String getName() {
        return name;
    }
}
