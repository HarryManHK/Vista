package com.example.vista;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;

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
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error adding markers: " + e.getMessage());
        }
    }

    // Confirm Button click handler
    private void handleConfirmButtonClick() {
        // Add logic for the confirm button here (e.g., save data or navigate to another page)
        Toast.makeText(this, "Confirm button clicked", Toast.LENGTH_SHORT).show();
    }

    // Next Button click handler
    private void handleNextButtonClick() {
        // Add logic for the next button here (e.g., move to the next page or step)
        Toast.makeText(this, "Next button clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach(); // To avoid memory leaks
        }
    }
}