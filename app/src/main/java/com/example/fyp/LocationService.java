package com.example.fyp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import java.util.List;

public class LocationService extends Service {

    private final IBinder mBinder = new MyBinder();
    private WifiManager wifi;
    public static Context context;
    private TelephonyManager telephonyManager;
    private List<SignalSource> sources;
    private LocationCallback locationCallback;
    private boolean wifiConnected;
    private boolean dataConnected;
    private SignalSource currentSource;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseInterface dbInterface;

    public LocationService() {}

    @Override
    public void onCreate() {
        context = getApplicationContext();

        wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {

                }
                for (Location location : locationResult.getLocations()) {
                    generateSignalSources();
                    checkConnectionStatus();
                    if (wifiConnected) {
                        saveLocation(location, getWifiLevel(), getWifiName());

                    }
                    if (dataConnected) {
                        saveLocation(location, getDataLevel(), getDataName());
                    }

                }
                Log.println(Log.DEBUG, "LocationService: LocationCallback", "Success");

            }


        };
    }
    @Override
    public void onDestroy() {
        Log.println(Log.DEBUG, "destroy", "lol");
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel("FYP", "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(notificationChannel);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,0);
        Notification notification =
                new Notification.Builder(this, "FYP")
                        .setContentIntent(pendingIntent)
                        .build();
        this.startForeground(320492, notification);
        getLocation();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    public void seDatabaseInterface(DatabaseInterface dbInterface) {
        this.dbInterface = dbInterface;
    }

    public class MyBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }
    @SuppressLint("MissingPermission")
    private void getLocation() {


        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
        Looper.myLooper();

    }

    private void generateSignalSources() {
        checkConnectionStatus();
        if (wifiConnected) {
            if (checkSources(getWifiName())) {

            } else if (getWifiName() == null) {

            } else {
                sources.add(new SignalSource(getWifiName(), dbInterface));
                if (sources.size() == 1) {
                    currentSource = sources.get(0);
                }
            }
        }
        if (dataConnected) {
            if (checkSources(getDataName())) {

            } else if (getDataName() == null) {

            } else {
                sources.add(new SignalSource(getDataName(), dbInterface));
                if (sources.size() == 1) {
                    currentSource = sources.get(0);
                }
            }

        }
    }

    private void checkConnectionStatus() {
        WifiInfo wifiInfo = (WifiInfo) wifi.getConnectionInfo();
        if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED)) {
            wifiConnected = true;
        } else {
            wifiConnected = false;
        }
        dataConnected = telephonyManager.isDataEnabled();
    }

    private boolean checkSources(String name) {
        for (SignalSource signalSource : sources) {
            if (signalSource.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int getWifiLevel() {
        WifiInfo wifiInfo = (WifiInfo) wifi.getConnectionInfo();
        return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
    }

    private String getWifiName() {
        WifiInfo wifiInfo = (WifiInfo) wifi.getConnectionInfo();
        checkConnectionStatus();
        if (wifiConnected) {
            return wifiInfo.getSSID();
        } else {
            return null;
        }
    }

    private String getDataName() {
        checkConnectionStatus();
        if (dataConnected) {
            return "data";
        } else {
            return null;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private int getDataLevel() {
        return telephonyManager.getSignalStrength().getLevel();
    }

    private void saveLocation(Location location, int level, String name) {
        if (sources.size() == 0) {

        } else {
            SignalStrengthLocation signalStrengthLocation = new SignalStrengthLocation(location, level, name);
            for (SignalSource signalSource : sources) {
                if (signalSource.getName().equals(name)) {
                    signalSource.addSignalStrengthLocation(signalStrengthLocation);
                }
            }
        }

    }

    public void setSources(List<SignalSource> sources) {
        this.sources = sources;
    }

    public List<SignalSource> getSources() {
        return sources;
    }

    public void setCurrentSource(SignalSource currentSource) {
        this.currentSource = currentSource;
    }

    public SignalSource getCurrentSource() {
        return currentSource;
    }
}