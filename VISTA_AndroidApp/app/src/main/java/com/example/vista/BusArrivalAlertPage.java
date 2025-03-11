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
import org.osmdroid.views.overlay.Overlay;

import com.example.vista.DatabaseHelper.BusStopInfomationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import android.Manifest;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BusArrivalAlertPage extends AppCompatActivity {

    private static final String TAG = "BusArrivalAlertPage_Debug";

    private MapView mapView;
    private Button btnFindBusEditConfirm;
    private Button btnFindBusEditNext;

    private String START_POINT_LAT;
    private String START_POINT_LONG;
    private String DESTINATION_LAT;
    private String DESTINATION_LONG;
    private String routeNumber;
    private String routeBound;

    private BusDatabaseHelper busDBHelper;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView currentLocationTextView;
    private LocationRequest locationRequest;
    private static final int LOCATION_REQUEST_CODE = 100;
    private MyLocationNewOverlay mLocationOverlay;
    private List<BusStopOverlayItem> busStopItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_bus_arrival_alert_page);
        EdgeToEdge.enable(this);

        mapView = findViewById(R.id.map);
        btnFindBusEditConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnFindBusEditNext = findViewById(R.id.btnFindBusEditNext);
        currentLocationTextView = findViewById(R.id.CurrentLocation);

        busDBHelper = BusDatabaseHelper.getInstance(this);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(8.0); // 限制最小縮放級別，避免過小
        mapView.setMaxZoomLevel(20.0); // 設置最大縮放級別

        // Hard code start point
        GeoPoint startPoint = new GeoPoint(22.3964, 114.1095);
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(12);

        // Fetch and display data
        getDBLocation();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getCurrentLocation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.setOptionsMenuEnabled(true);
        mLocationOverlay.setDrawAccuracyEnabled(true);

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

        btnFindBusEditConfirm.setOnClickListener(view -> handleConfirmButtonClick());
        btnFindBusEditNext.setOnClickListener(view -> handleNextButtonClick());

        // Fetch bus stops
        BusStopInfomationHelper busStopInfoHelper = new BusStopInfomationHelper(this);
        busStopInfoHelper.fetchAndStoreBusStops(routeNumber, routeBound, new BusStopInfomationHelper.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(boolean success) {
                if (success) {
                    Log.d(TAG, "Bus stops fetched and stored successfully.");
                    printAllRecord();
                } else {
                    Log.e(TAG, "Failed to fetch bus stops.");
                }
            }
        });

        printAllRecord();
    }

    private void printAllRecord() {
        BusStopInfomationHelper helper = new BusStopInfomationHelper(this);
        Cursor cursor = helper.getAllStopsRaw();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_ID));
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_ROUTE_NUMBER));
                String bound = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BOUND));
                String stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME));
                String stopNameZh = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME_ZH));
                int seq = cursor.getInt(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_SEQ));
                String stopId = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_ID));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LONG));

                Log.d(TAG,
                        "Record: ID=" + id +
                                ", routeNumber=" + routeNumber +
                                ", bound=" + bound +
                                ", stopNameEn=" + stopNameEn +
                                ", stopNameZh=" + stopNameZh +
                                ", seq=" + seq +
                                ", stopId=" + stopId +
                                ", lat=" + lat +
                                ", lng=" + lng
                );
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.d(TAG, "No records found in bus_stops table.");
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
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

                            Location osmdroidLocation = new Location("fused");
                            osmdroidLocation.setLatitude(latitude);
                            osmdroidLocation.setLongitude(longitude);
                            mLocationOverlay.onLocationChanged(osmdroidLocation, null);

                            mapView.getController().animateTo(new GeoPoint(latitude, longitude));
                            mapView.invalidate();

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

    private void getDBLocation() {
        Cursor cursor = null;
        try {
            cursor = busDBHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                routeBound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));

                START_POINT_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LAT));
                START_POINT_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LONG));
                DESTINATION_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LAT));
                DESTINATION_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LONG));

                Log.d(TAG, "Loaded route=" + routeNumber + ", bound=" + routeBound);

                String apiBound = routeBound.equals("O") ? "outbound" :
                        routeBound.equals("I") ? "inbound" : routeBound;

                addBusStopsToMap();
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

    private void addBusStopsToMap() {
        busStopItems.clear();
        mapView.getOverlays().removeIf(overlay -> overlay instanceof BusStopOverlay);

        // Add START point
        if (START_POINT_LAT != null && START_POINT_LONG != null) {
            try {
                GeoPoint startPoint = new GeoPoint(Double.parseDouble(START_POINT_LAT), Double.parseDouble(START_POINT_LONG));
                busStopItems.add(new BusStopOverlayItem(startPoint, "Start Point", R.drawable.start_point));
                Log.d(TAG, "Start point added at: lat=" + START_POINT_LAT + ", lng=" + START_POINT_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding START point: " + e);
            }
        }

        // Add DESTINATION point
        if (DESTINATION_LAT != null && DESTINATION_LONG != null) {
            try {
                GeoPoint destPoint = new GeoPoint(Double.parseDouble(DESTINATION_LAT), Double.parseDouble(DESTINATION_LONG));
                busStopItems.add(new BusStopOverlayItem(destPoint, "Destination", R.drawable.end_point));
                Log.d(TAG, "Destination point added at: lat=" + DESTINATION_LAT + ", lng=" + DESTINATION_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding DESTINATION point: " + e);
            }
        }

        // Add bus stops
        BusStopInfomationHelper helper = new BusStopInfomationHelper(this);
        Cursor cursor = null;
        try {
            cursor = helper.getAllStopsRaw();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LAT));
                    double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LONG));
                    String stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME));
                    String stopNameZh = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME_ZH));

                    GeoPoint busStopPoint = new GeoPoint(lat, lng);
                    String title = stopNameEn + " (" + stopNameZh + ")";
                    if (lat == Double.parseDouble(START_POINT_LAT) && lng == Double.parseDouble(START_POINT_LONG)) {
                        busStopItems.get(0).title = title; // Update start point title
                        continue;
                    } else if (lat == Double.parseDouble(DESTINATION_LAT) && lng == Double.parseDouble(DESTINATION_LONG)) {
                        busStopItems.get(1).title = title; // Update destination title
                        continue;
                    }

                    busStopItems.add(new BusStopOverlayItem(busStopPoint, title, R.drawable.bus_stop));
                    Log.d(TAG, "Bus stop added at: lat=" + lat + ", lng=" + lng);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding bus stops: " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        mapView.getOverlays().add(new BusStopOverlay());
        mapView.invalidate();
    }

    private class BusStopOverlay extends Overlay {
        private Paint paint;

        public BusStopOverlay() {
            paint = new Paint();
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow) return;

            for (BusStopOverlayItem item : busStopItems) {
                GeoPoint point = item.point;
                Drawable drawable = ContextCompat.getDrawable(BusArrivalAlertPage.this, item.drawableRes);
                if (drawable == null) continue;

                // Convert GeoPoint to screen coordinates
                org.osmdroid.api.IGeoPoint geoPoint = new GeoPoint(point.getLatitude(), point.getLongitude());
                android.graphics.Point screenPoint = new android.graphics.Point();
                mapView.getProjection().toPixels(geoPoint, screenPoint);

                // Draw the icon centered at the point
                Bitmap bitmap = drawableToBitmap(drawable);
                int x = screenPoint.x - bitmap.getWidth() / 2;
                int y = screenPoint.y - bitmap.getHeight(); // Bottom of icon aligns with point
                canvas.drawBitmap(bitmap, x, y, paint);
            }
        }
    }

    private class BusStopOverlayItem {
        GeoPoint point;
        String title;
        int drawableRes;

        BusStopOverlayItem(GeoPoint point, String title, int drawableRes) {
            this.point = point;
            this.title = title;
            this.drawableRes = drawableRes;
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void handleConfirmButtonClick() {
        Toast.makeText(this, "Confirm button clicked", Toast.LENGTH_SHORT).show();
    }

    private void handleNextButtonClick() {
        Toast.makeText(this, "Next button clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        mapView.getController().setZoom(18);
        mapView.invalidate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationOverlay.disableMyLocation();
        mLocationOverlay.disableFollowLocation();
    }

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

        final double R = 6371.0;
        return R * c;
    }
}