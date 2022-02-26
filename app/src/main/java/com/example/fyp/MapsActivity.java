package com.example.fyp;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.fyp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private TextView wifiTextView;
    private TextView dataTextView;
    public static Context context;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private Location currentLocation;
    private WifiManager wifi;
    private ConnectivityManager connectivityManager;
    private TelephonyManager telephonyManager;
    private List<SignalSource> sources;
    private HeatmapTileProvider heatmapTileProvider;
    private TileOverlay overlay;
    private String currentSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();



        wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);

        sources = generateSignalSources();

        currentSource = getWifiName();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        wifiTextView = findViewById(R.id.wifi_text);
        wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", getWifiLevel()));
        dataTextView = findViewById(R.id.data_text);
        dataTextView.setText(MessageFormat.format("Data Strength: {0}", getDataLevel()));
        final Button button = findViewById(R.id.location);
        button.setOnClickListener(v -> resetView());
        locationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {

                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;

                    wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", getWifiLevel()));
                    dataTextView.setText(MessageFormat.format("Data Strength: {0}", getDataLevel()));

                    saveLocation(location, getWifiLevel(), getWifiName());
                    saveLocation(location, getDataLevel(), getDataName());
                    updateUI();
                }
            }


        };
    }
    public boolean locationPermissionRequest() {
        int fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        int backgroundLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        String[] requests = new String[]{};

        if (coarseLocationPermission == PERMISSION_DENIED) {
            requests = Arrays.copyOf(requests, requests.length + 1);
            requests[requests.length - 1] = new String(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (fineLocationPermission == PERMISSION_DENIED) {
                requests = Arrays.copyOf(requests, requests.length + 1);
                requests[requests.length - 1] = new String(Manifest.permission.ACCESS_FINE_LOCATION);
                if (backgroundLocationPermission == PERMISSION_DENIED) {
                    requests = Arrays.copyOf(requests, requests.length + 1);
                    requests[requests.length - 1] = new String(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
            }
        }
        if (requests.length > 0) {
            ActivityCompat.requestPermissions(MapsActivity.this, requests, 1);
            return false;
        } else {
            return true;
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length <= 0) {

            } else if(grantResults[0] == PERMISSION_GRANTED) {
                getLocation();
            } else {

            }
        }
    }
    private void getLocation() {


        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                locationCallback,
                                Looper.getMainLooper());
                        Log.println(Log.DEBUG,"lol", "lol");
                    }
                }
        );
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    Log.println(Log.DEBUG,"lol2", "lol2");

                }
            }
        });

        Looper.myLooper();

    }
    private List<SignalSource> generateSignalSources() {
        List<SignalSource> sources = new ArrayList<>();
        /*
        * get wifi name does not work null
        * */
        sources.add(new SignalSource(getWifiName()));
        sources.add(new SignalSource(getDataName()));
        return sources;
    }
    private void updateUI() {
        if (currentLocation != null) {
            mMap.clear();
            currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("here"));
            List<SignalStrengthLocation> selectedLocaitons = new ArrayList<>();
            for (SignalSource source: sources) {
                if(source.getName().equals(currentSource)) {
                    selectedLocaitons = source.getSignalStrengthLocationList();
                }
            }
            changeDataHeatmap(selectedLocaitons);
        }
    }

    private void createHeatmap(List<SignalStrengthLocation> locations) {
        List<WeightedLatLng> latLngs = getHeatmapData(locations);
        int[] colours = {
                Color.rgb(255,0,0),

                Color.rgb(102,225,0)

        };
        float[] startPoints = {
                0.1f, 0.8f
        };
        Gradient gradient = new Gradient(colours, startPoints);
        heatmapTileProvider = new HeatmapTileProvider.Builder().weightedData(latLngs).radius(50).gradient(gradient).build();

        overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
    }

    private void changeDataHeatmap(List<SignalStrengthLocation> locations) {

        if(heatmapTileProvider == null | overlay == null ) {
            createHeatmap(locations);
        } else {
            List<WeightedLatLng> latLngs = getHeatmapData(locations);

            overlay.clearTileCache();
            heatmapTileProvider.setWeightedData(latLngs);
        }
    }
    private void resetView() {
        CameraPosition cameraPosition;
        if(mMap == null) {
            return;
        }
        if(currentLatLng == null) {
            cameraPosition = CameraPosition.builder().target(new LatLng(0,0)).bearing(0).zoom(0).build();
        } else {
            cameraPosition = CameraPosition.builder().target(currentLatLng).bearing(0).zoom(20).build();
        }
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }
    private int getWifiLevel() {
        WifiInfo wifiInfo = (WifiInfo) wifi.getConnectionInfo();
        return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
    }
    private String getWifiName() {
        WifiInfo wifiInfo = (WifiInfo) wifi.getConnectionInfo();
        return wifiInfo.getSSID();
    }
    private String getDataName() {
        return "data";
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    private int getDataLevel() {
        return telephonyManager.getSignalStrength().getLevel();
    }

    private void saveLocation(Location location, int level, String name) {
        SignalStrengthLocation signalStrengthLocation = new SignalStrengthLocation(location, level);
        int count = 0;
        for (SignalSource signalSource: sources) {
            if(signalSource.getName().equals(name)) {
                signalSource.addSignalStrengthLocation(signalStrengthLocation);
            } else {
                count++;
            }
        }
        if (sources.size() == count) {
            sources.add(new SignalSource(name, signalStrengthLocation));
        }
    }

    private List<WeightedLatLng> getHeatmapData(List<SignalStrengthLocation> locations) {
        List<WeightedLatLng> heatmapData = new ArrayList<>();
        for (SignalStrengthLocation location: locations) {
            heatmapData.add(location.getWeightedLatLng());
        }
        return heatmapData;
    }
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        updateUI();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onStart() {
        super.onStart();
        locationPermissionRequest();
        getLocation();
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        resetView();
        List<SignalStrengthLocation> selectedLocaitons = new ArrayList<>();
        for (SignalSource source: sources) {
            if(source.getName().equals(currentSource)) {
                selectedLocaitons = source.getSignalStrengthLocationList();
            }
        }
        if(selectedLocaitons.size() == 0) {

        } else {
            createHeatmap(selectedLocaitons);
        }


        // Add a marker in Sydney and move the camera
    }
    @Override
    public void onResume() {
        super.onResume();
        resetView();

    }
}