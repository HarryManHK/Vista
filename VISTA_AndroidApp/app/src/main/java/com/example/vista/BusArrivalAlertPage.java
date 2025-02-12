package com.example.vista;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.gms.tasks.Task;
import android.Manifest;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.BusStopInformation; // <-- Import your new class

import java.util.Locale;

import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BusArrivalAlertPage extends AppCompatActivity {

    private static final String TAG = "BusArrivalAlertPage_Debug";

    private MapView mapView;
    private Button btnFindBusEditConfirm;
    private Button btnFindBusEditNext;

    // Fields from the main BusRoute table (start & destination)
    private String START_POINT_LAT;
    private String START_POINT_LONG;
    private String DESTINATION_LAT;
    private String DESTINATION_LONG;

    // Also retrieve routeNumber & bound from the BusDatabaseHelper
    private String routeNumber;
    private String routeBound; // e.g. "O" for outbound, "I" for inbound

    private BusDatabaseHelper busDBHelper;
    private BusStopInformation busStopInfoHelper; // <-- We'll use this to load all stops

    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView currentLocationTextView;
    private LocationRequest locationRequest;
    private static final int LOCATION_REQUEST_CODE = 100;
    private MyLocationNewOverlay mLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid library configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        // Set content view
        setContentView(R.layout.activity_bus_arrival_alert_page);

        // Enable edge-to-edge support for the app
        EdgeToEdge.enable(this);

        // Initialize views
        mapView = findViewById(R.id.map);
        btnFindBusEditConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnFindBusEditNext = findViewById(R.id.btnFindBusEditNext);
        currentLocationTextView = findViewById(R.id.CurrentLocation);

        // Initialize Database Helpers
        busDBHelper = BusDatabaseHelper.getInstance(this);
        busStopInfoHelper = new BusStopInformation(this);

        // Configure MapView
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Set a default position (e.g., Hong Kong)
        GeoPoint startPoint = new GeoPoint(22.3964, 114.1095);
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(12);

        // Fetch data from the database and add markers to the map
        getDBLocation(); // <-- Loads route data (start/destination + routeNumber/bound) & calls addMarkersToMap()

        // Initialize location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Create a LocationRequest for continuous updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);        // 10 seconds
        locationRequest.setFastestInterval(5000);  // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Request current location
        getCurrentLocation();

        // Apply window insets for system bars handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up the MyLocation overlay
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.setOptionsMenuEnabled(true);
        mLocationOverlay.setDrawAccuracyEnabled(true);

        // Set a custom GPS icon if desired
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_gps_fixed);
        if (drawable instanceof BitmapDrawable) {
            mLocationOverlay.setPersonIcon(((BitmapDrawable) drawable).getBitmap());
        } else {
            Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            mLocationOverlay.setPersonIcon(bitmap);
        }
        mapView.getOverlays().add(mLocationOverlay);

        // Set up button listeners
        btnFindBusEditConfirm.setOnClickListener(view -> handleConfirmButtonClick());
        btnFindBusEditNext.setOnClickListener(view -> handleNextButtonClick());
    }

    private void getCurrentLocation() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Start receiving location updates
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            // Permission not granted
            Toast.makeText(this, "Location permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private com.google.android.gms.location.LocationCallback locationCallback =
            new com.google.android.gms.location.LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Update MyLocation overlay
                            Location osmdroidLocation = new Location("fused");
                            osmdroidLocation.setLatitude(latitude);
                            osmdroidLocation.setLongitude(longitude);
                            mLocationOverlay.onLocationChanged(osmdroidLocation, null);

                            // Optionally move the map center
                            mapView.getController().animateTo(new GeoPoint(latitude, longitude));
                            mapView.invalidate();

                            // Calculate distance to the final destination
                            if (DESTINATION_LAT != null && DESTINATION_LONG != null) {
                                double distance = calculateDistance(latitude, longitude,
                                        Double.parseDouble(DESTINATION_LAT), Double.parseDouble(DESTINATION_LONG));
                                String locationText = String.format(Locale.getDefault(),
                                        "Current Location:\n%.6f, %.6f\nDistance to end point:\n%.2f km",
                                        latitude, longitude, distance);
                                currentLocationTextView.setText(locationText);
                            }
                        }
                    }
                }
            };

    /**
     * Loads the latest bus route info from BusDatabaseHelper:
     * - routeNumber, bound
     * - start/destination lat/long
     * Then calls addMarkersToMap() to place markers.
     */
    private void getDBLocation() {
        Cursor cursor = null;
        try {
            cursor = busDBHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                // Extract route data
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                routeBound  = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));

                START_POINT_LAT  = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LAT));
                START_POINT_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LONG));
                DESTINATION_LAT  = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LAT));
                DESTINATION_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LONG));

                Log.d(TAG, "Loaded route=" + routeNumber + ", bound=" + routeBound);
                Log.d(TAG, "Start(" + START_POINT_LAT + ", " + START_POINT_LONG + "), Dest("
                        + DESTINATION_LAT + ", " + DESTINATION_LONG + ")");

                // Add markers for start, end, and all intermediate stops
                addMarkersToMap();
            } else {
                Log.d(TAG, "No bus route data found.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading DB Location data: " + e);
            Toast.makeText(this, "Error loading DB Location data", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Places markers for the start point, destination, and ALL stops in the route.
     */
    private void addMarkersToMap() {
        // 1) Add START marker
        try {
            if (START_POINT_LAT != null && START_POINT_LONG != null) {
                GeoPoint startPoint = new GeoPoint(
                        Double.parseDouble(START_POINT_LAT),
                        Double.parseDouble(START_POINT_LONG)
                );
                Marker startMarker = new Marker(mapView);
                startMarker.setPosition(startPoint);
                startMarker.setTitle("Start Point");
                mapView.getOverlays().add(startMarker);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding START marker: " + e);
        }

        // 2) Add DESTINATION marker
        try {
            if (DESTINATION_LAT != null && DESTINATION_LONG != null) {
                GeoPoint destinationPoint = new GeoPoint(
                        Double.parseDouble(DESTINATION_LAT),
                        Double.parseDouble(DESTINATION_LONG)
                );
                Marker destinationMarker = new Marker(mapView);
                destinationMarker.setPosition(destinationPoint);
                destinationMarker.setTitle("Destination");
                mapView.getOverlays().add(destinationMarker);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding DESTINATION marker: " + e);
        }

        // 3) Add ALL INTERMEDIATE STOPS from BusStopInformation
        //    Only do this if routeNumber & routeBound are available
        if (routeNumber != null && routeBound != null) {
            Cursor stopCursor = null;
            try {
                // "routeBound" might be "O", "I", "outbound", "inbound", etc.
                // Make sure you insert the correct format into busStopInfoHelper.
                // For example, if you inserted "O" in the 'bound' column, use the same here.
                stopCursor = busStopInfoHelper.getStopsForRoute(routeNumber, routeBound);

                if (stopCursor != null && stopCursor.moveToFirst()) {
                    do {
                        String stopNameEn = stopCursor.getString(
                                stopCursor.getColumnIndexOrThrow(BusStopInformation.COLUMN_STOP_NAME_EN));
                        String stopNameZh = stopCursor.getString(
                                stopCursor.getColumnIndexOrThrow(BusStopInformation.COLUMN_STOP_NAME_ZH));
                        String lat = stopCursor.getString(
                                stopCursor.getColumnIndexOrThrow(BusStopInformation.COLUMN_STOP_LAT));
                        String lng = stopCursor.getString(
                                stopCursor.getColumnIndexOrThrow(BusStopInformation.COLUMN_STOP_LONG));

                        try {
                            double dLat = Double.parseDouble(lat);
                            double dLng = Double.parseDouble(lng);
                            GeoPoint stopPoint = new GeoPoint(dLat, dLng);

                            Marker stopMarker = new Marker(mapView);
                            stopMarker.setPosition(stopPoint);
                            // You can combine English & Chinese names or use only one
                            stopMarker.setTitle(stopNameEn + " / " + stopNameZh);

                            mapView.getOverlays().add(stopMarker);
                        } catch (NumberFormatException nfe) {
                            Log.e(TAG, "Invalid lat/long for stop: " + nfe);
                        }
                    } while (stopCursor.moveToNext());
                } else {
                    Log.d(TAG, "No intermediate stops found for route " + routeNumber + " bound " + routeBound);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding intermediate stop markers: " + e);
            } finally {
                if (stopCursor != null) {
                    stopCursor.close();
                }
            }
        }

        // Refresh map after adding everything
        mapView.invalidate();
    }

    private void handleConfirmButtonClick() {
        // TODO: Implement any logic needed for the "Confirm" button
        Toast.makeText(this, "Confirm button clicked", Toast.LENGTH_SHORT).show();
    }

    private void handleNextButtonClick() {
        // TODO: Implement any logic needed for the "Next" button
        Toast.makeText(this, "Next button clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-enable location overlay
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        mapView.getController().setZoom(18);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable location overlay to save battery
        mLocationOverlay.disableMyLocation();
        mLocationOverlay.disableFollowLocation();
    }

    /**
     * Calculate approximate distance (in KM) using the Haversine formula
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        final double R = 6371.0; // Radius of the earth in km
        return R * c;            // Distance in kilometers
    }
}
