
package com.example.fyp;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.fyp.databinding.ActivityMapsBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ServiceConnection {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private TextView wifiTextView;
    private TextView dataTextView;
    private TextView heatmapTypeTextView;
    private TextView currentUserTextView;
    public static Context context;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private Location currentLocation;
    private WifiManager wifi;
    private TelephonyManager telephonyManager;
    private List<SignalSource> sources;
    private SignalSource currentSource;
    private boolean wifiConnected;
    private boolean dataConnected;
    private Intent intent;
    private LocationService service;
    private DatabaseInterface dbInterface;
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MapsActivity", "Start" );

        context = getApplicationContext();


        wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        sources = new ArrayList<>();

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

// Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        signInLauncher.launch(signInIntent);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        wifiTextView = findViewById(R.id.wifi_text);
        heatmapTypeTextView = findViewById(R.id.heatmap_type);
        dbInterface = new DatabaseInterface();
        dbInterface.getSignalStrengthLocationDB();
        generateSignalSources();

        if (sources.size() != 0) {
            currentSource = sources.get(0);
        }

        checkConnectionStatus();
        if (wifiConnected) {
            wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", getWifiLevel()));
        } else {
            wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", "Disconnected"));
        }

        dataTextView = findViewById(R.id.data_text);

        if (dataConnected) {
            dataTextView.setText(MessageFormat.format("Data Strength: {0}", getDataLevel()));
        } else {
            dataTextView.setText(MessageFormat.format("Data Strength: {0}", "Disconnected"));
        }

        if (currentSource == null) {
            heatmapTypeTextView.setText("Selected Source: N/A");
        } else {
            heatmapTypeTextView.setText(MessageFormat.format("Selected Source: {0}", currentSource.getName()));
        }

        final Button save = findViewById(R.id.save);
        final Button swap_source = findViewById(R.id.button_swap);
        final Button sign_out = findViewById(R.id.sign_out);
        sign_out.setOnClickListener(v -> signOut());
        swap_source.setOnClickListener(v -> getNextSource());
        save.setOnClickListener(v -> save());
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
                    generateSignalSources();
                    checkConnectionStatus();
                    if (wifiConnected) {
                        wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", getWifiLevel()));
                        saveLocation(location, getWifiLevel(), getWifiName());

                    } else {
                        wifiTextView.setText(MessageFormat.format("Wifi Strength: {0}", "Disconnected"));
                    }
                    if (dataConnected) {
                        dataTextView.setText(MessageFormat.format("Data Strength: {0}", getDataLevel()));
                        saveLocation(location, getDataLevel(), getDataName());
                    } else {
                        dataTextView.setText(MessageFormat.format("Data Strength: {0}", "Disconnected"));
                    }
                    Log.println(Log.DEBUG, "MapsActivity: LocationCallback", "Success");
                    updateUI();
                }
            }


        };
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            currentUser = new User(user, dbInterface);
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        currentUser = null;
                    }
                });
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length <= 0) {

            } else if (grantResults[0] == PERMISSION_GRANTED) {
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
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
        );
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;

                }
            }
        });

        Looper.myLooper();

    }

    public void save() {
        currentUser.saveUser();
    }

    private void generateSignalSources() {
        checkConnectionStatus();
        if (wifiConnected) {
            if (checkSources(getWifiName())) {

            } else if (getWifiName() == null) {

            } else {
                SignalSource source = new SignalSource(getWifiName(), dbInterface);

                Log.d("MapsActivity", "Added source " + getWifiName());
                if (currentUser != null) {
                    currentUser.addSignalSource(source);

                }
                sources.add(source);

                if (sources.size() == 1) {
                    currentSource = sources.get(0);
                    heatmapTypeTextView.setText(MessageFormat.format("Selected Source: {0}", currentSource.getName()));

                }
            }
        }
        if (dataConnected) {
            if (checkSources(getDataName())) {

            } else if (getDataName() == null) {

            } else {
                SignalSource source = new SignalSource(getDataName(), dbInterface);

                Log.d("MapsActivity", "Added source " + getDataName());
                if (currentUser != null) {
                    currentUser.addSignalSource(source);

                }

                sources.add(new SignalSource(getDataName(), dbInterface));
                if (sources.size() == 1) {
                    currentSource = sources.get(0);
                    heatmapTypeTextView.setText(MessageFormat.format("Selected Source: {0}", currentSource.getName()));

                }
            }

        }

        if (sources != null) {
            if(currentUser != null) {
                currentUser.addSignalSource(sources);
            }
        }

        if (currentUser != null) {
            dbInterface.getSignalSourceDB(currentUser.getUid(), dbInterface);
            List<String> sourcesNames = new ArrayList<>();
            sources.forEach((n) -> sourcesNames.add(n.getName()));
            for (SignalSource signalSource : currentUser.getSignalSourcesList()) {
                if (!sourcesNames.contains(signalSource.getName())) {
                    sources.add(signalSource);
                }
            }
        }
    }

    private boolean checkSources(String name) {
        for (SignalSource signalSource : sources) {
            if (signalSource.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        if (currentLocation != null & currentSource != null) {
            mMap.clear();
            heatmapTypeTextView.setText(MessageFormat.format("Selected Source: {0}", currentSource.getName()));

            currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("here"));
            List<SignalStrengthLocation> selectedLocaitons = new ArrayList<>();
            for (SignalSource source : sources) {
                if (source.equals(currentSource)) {
                    selectedLocaitons = source.getSignalStrengthLocationList();
                }
            }
            createHeatmap(selectedLocaitons);
        }
    }

    private void createHeatmap(List<SignalStrengthLocation> locations) {
        List<WeightedLatLng> latLngs = getHeatmapData(locations);


        if (latLngs.size() == 0) {

        } else {
            List<WeightedLatLng> levelOneLatLngs = new ArrayList<>();
            List<WeightedLatLng> levelTwoLatLngs = new ArrayList<>();
            List<WeightedLatLng> levelThreeLatLngs = new ArrayList<>();
            List<WeightedLatLng> levelFourLatLngs = new ArrayList<>();
            for (WeightedLatLng latLng: latLngs) {
                switch((int) latLng.getIntensity()) {
                    case 1:
                        levelOneLatLngs.add(latLng);
                        break;
                    case 2:
                        levelTwoLatLngs.add(latLng);
                        break;
                    case 3:
                        levelThreeLatLngs.add(latLng);
                        break;
                    case 4:
                        levelFourLatLngs.add(latLng);
                        break;
                    default:
                        levelOneLatLngs.add(latLng);

                }
            }

            if (levelOneLatLngs.size() != 0) {
                TileOverlay overlayOne = mMap.addTileOverlay(generateTileOverlayOptions(levelOneLatLngs, 1));
            }
            if (levelTwoLatLngs.size() != 0) {
                TileOverlay overlayOne = mMap.addTileOverlay(generateTileOverlayOptions(levelTwoLatLngs, 2));

            }
            if (levelThreeLatLngs.size() != 0) {
                TileOverlay overlayOne = mMap.addTileOverlay(generateTileOverlayOptions(levelThreeLatLngs, 3));
            }
            if (levelFourLatLngs.size() != 0) {
                TileOverlay overlayOne = mMap.addTileOverlay(generateTileOverlayOptions(levelFourLatLngs, 4));
            }

        }

    }

    private TileOverlayOptions generateTileOverlayOptions(List<WeightedLatLng> latLngs, int level) {
        float[] startPoints = {
                0.5f
        };
        int[] colours;
        switch (level) {
            case 1:
                colours = new int[]{
                        Color.rgb(255, 0, 0)
                };
                break;
            case 2:
                colours = new int[]{
                        Color.rgb(255, 102, 0)
                };
                break;
            case 3:
                colours = new int[]{
                        Color.rgb(255, 255, 0)
                };
                break;
            case 4:
                colours = new int[]{
                        Color.rgb(104, 255, 0)
                };
                break;
            default:
                colours = new int[]{
                        Color.rgb(0, 0, 0)
                };

        }
        Gradient gradient = new Gradient(colours, startPoints);

        HeatmapTileProvider heatmapTileProvider = new HeatmapTileProvider.Builder().weightedData(latLngs).radius(35).opacity(0.5).maxIntensity(1).gradient(gradient).build();
        return new TileOverlayOptions().tileProvider(heatmapTileProvider);


    }

    private void resetView() {
        CameraPosition cameraPosition;
        if (mMap == null) {
            return;
        }
        if (currentLatLng == null) {
            cameraPosition = CameraPosition.builder().target(new LatLng(51, 0)).bearing(0).zoom(5).build();
        } else {
            cameraPosition = CameraPosition.builder().target(currentLatLng).bearing(0).zoom(17).build();
        }
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

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
            return telephonyManager.getNetworkOperatorName();
        } else {
            return null;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private int getDataLevel() {
        Log.d("Telephony", telephonyManager.getNetworkOperatorName());
        return telephonyManager.getSignalStrength().getLevel();
    }

    private void saveLocation(Location location, int level, String name) {
        if (sources.size() == 0) {

        } else {
            SignalStrengthLocation signalStrengthLocation = new SignalStrengthLocation(location, level, name);
            dbInterface.addSignalStrengthLocationDB(signalStrengthLocation);
        }

    }

    private List<WeightedLatLng> getHeatmapData(List<SignalStrengthLocation> locations) {
        List<WeightedLatLng> heatmapData = new ArrayList<>();
        for (SignalStrengthLocation location : locations) {
            heatmapData.add(location.generateWeightedLatLng());
        }
        return heatmapData;
    }

    private void getNextSource() {
        SignalSource currentSourceTemp = currentSource;
        for (int i = 0; i < sources.size(); i++) {
            SignalSource signalSource = sources.get(i);
            if (signalSource.equals(currentSource)) {
                if (i + 1 == sources.size()) {
                    currentSourceTemp = sources.get(0);
                } else {
                    currentSourceTemp = sources.get(i + 1);
                }

            }
        }
        currentSource = currentSourceTemp;
        updateUI();

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onStart() {
        super.onStart();
        locationPermissionRequest();
        getLocation();
    }

    ;

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
        for (SignalSource source : sources) {
            if (source.equals(currentSource)) {
                selectedLocaitons = source.getSignalStrengthLocationList();
            }
        }
        if (selectedLocaitons.size() == 0) {

        } else {
            createHeatmap(selectedLocaitons);
        }


        // Add a marker in Sydney and move the camera
    }

    @Override
    public void onResume() {
        super.onResume();
        resetView();
        getLocation();

        if (service != null) {
            sources = service.getSources();
            currentSource = service.getCurrentSource();
            context.unbindService(this);
            context.stopService(intent);
            service = null;
        }

    }

    @Override
    public void onPause() {
        startService();
        fusedLocationClient.removeLocationUpdates(locationCallback);

        super.onPause();
    }
    private void startService() {

        intent = new Intent(context,LocationService.class);
        context.startService(intent);
        context.bindService(intent, this, context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        LocationService.MyBinder b = (LocationService.MyBinder) binder;
        service = b.getService();
        service.setSources(sources);
        service.setCurrentSource(currentSource);
        service.seDatabaseInterface(dbInterface);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}