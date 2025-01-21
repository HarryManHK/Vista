package com.example.vista;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.location.Location;
import android.Manifest;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;

import java.io.IOException;
import java.util.List;

public class BusArrivalAlertPage extends AppCompatActivity {

    private MapView mapView;
    private Button btnFindBusEditConfirm;
    private Button btnFindBusEditNext;
    private String START_POINT_LAT;
    private String START_POINT_LONG;
    private String DESTINATION_LAT;
    private String DESTINATION_LONG;
    private BusDatabaseHelper BusDBHelper;
    private String TAG = "BusArrivalAlertPage_Debug";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView currentLocationTextView;
    private LocationRequest locationRequest;
    private static final int LOCATION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid library configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        // Set content view
        setContentView(R.layout.activity_bus_arrival_alert_page);

        // Enable edge-to-edge support for the app
        EdgeToEdge.enable(this);

        // Initialize MapView and Buttons
        mapView = findViewById(R.id.map);
        btnFindBusEditConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnFindBusEditNext = findViewById(R.id.btnFindBusEditNext);

        // Initialize BusDatabaseHelper
        BusDBHelper = BusDatabaseHelper.getInstance(this);

        // Configure MapView
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Set map tiles source
        mapView.setBuiltInZoomControls(true); // Show zoom controls
        mapView.setMultiTouchControls(true); // Enable pinch-to-zoom

        // Set default position to a known location (e.g., Hong Kong)
        GeoPoint startPoint = new GeoPoint(22.3964, 114.1095); // Coordinates for Hong Kong
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(12); // Set zoom level

        // Fetch data from the database and add markers to the map
        getDBLocation();

        // Initialize location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the TextView to show the current location
        currentLocationTextView = findViewById(R.id.CurrentLocation);

        // Create a LocationRequest for continuous updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Set update interval (in milliseconds)
        locationRequest.setFastestInterval(5000); // Set fastest update interval (in milliseconds)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // High accuracy

        // Get current location and update UI
        getCurrentLocation();

        // Apply window insets for system bars handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up button listeners
        btnFindBusEditConfirm.setOnClickListener(view -> {
            // Handle Confirm Button click
            handleConfirmButtonClick();
        });

        btnFindBusEditNext.setOnClickListener(view -> {
            // Handle Next Button click
            handleNextButtonClick();
        });
    }

    private void getCurrentLocation() {
        // Check if location permissions are granted (you can check at runtime if necessary)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Start receiving location updates
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            // Handle permission request if not granted
            Toast.makeText(this, "Location permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private com.google.android.gms.location.LocationCallback locationCallback = new com.google.android.gms.location.LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null && locationResult.getLocations().size() > 0) {
                // Get the most recent location
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Update the TextView with the current location
                    double distance = calculateDistance(latitude, longitude, Double.valueOf(DESTINATION_LAT), Double.valueOf(DESTINATION_LONG));
                    currentLocationTextView.setText("Current Location: \n" + latitude + ", \n" + longitude + "\n" + distance + "KM");
                }
            }
        }
    };

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double R = 6371.0;

        // Distance in kilometers
        double distance = R * c;

        return distance; // distance in kilometers
    }

    private void getDBLocation() {
        Cursor cursor = null;
        try {
            // Get bus route data from the database
            cursor = BusDBHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                // Extract bus route data from cursor
                START_POINT_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LAT));
                START_POINT_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LONG));
                DESTINATION_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LAT));
                DESTINATION_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LONG));

                // Log for debugging
                Log.d(TAG, "Location data loaded: " + START_POINT_LAT + ", " + START_POINT_LONG);

                // Add markers to the map
                addMarkersToMap();
            } else {
                Log.d(TAG, "No bus route data found.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading DB Location data: " + e.toString());
            Toast.makeText(this, "Error loading DB Location data", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void addMarkersToMap() {
        try {
            // Start point
            GeoPoint startPoint = new GeoPoint(Double.valueOf(START_POINT_LAT), Double.valueOf(START_POINT_LONG));
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(startPoint);
            startMarker.setTitle("Start Point");
            mapView.getOverlays().add(startMarker);
            // Destination point
            GeoPoint destinationPoint = new GeoPoint(Double.valueOf(DESTINATION_LAT), Double.valueOf(DESTINATION_LONG));
            Marker destinationMarker = new Marker(mapView);
            destinationMarker.setPosition(destinationPoint);
            destinationMarker.setTitle("Destination");
            mapView.getOverlays().add(destinationMarker);
        } catch (Exception e) {
            Log.e(TAG, "Error adding markers to map: " + e.toString());
            Toast.makeText(this, "Error adding markers", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleConfirmButtonClick() {
        // Handle logic for confirm button click
        // (e.g., start bus arrival alert logic)
    }

    private void handleNextButtonClick() {
        // Handle logic for next button click
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback); // Stop location updates to save battery
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start location updates when the activity is resumed
        getCurrentLocation();
    }
}