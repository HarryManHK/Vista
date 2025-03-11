package com.example.vista;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import org.osmdroid.views.overlay.Polyline;

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
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

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
    // busStopItems 用來標記所有站點（包含起點、DB 取得的站點、終點）
    private List<BusStopOverlayItem> busStopItems = new ArrayList<>();
    // waypoints 用來規劃紅線路徑，僅包含 DB 中的巴士站（起點與終點排除）
    private List<GeoPoint> waypoints = new ArrayList<>();
    private Polyline routeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_bus_arrival_alert_page);
        // 設定 EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapView = findViewById(R.id.map);
        btnFindBusEditConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnFindBusEditNext = findViewById(R.id.btnFindBusEditNext);
        currentLocationTextView = findViewById(R.id.CurrentLocation);

        busDBHelper = BusDatabaseHelper.getInstance(this);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(8.0);
        mapView.setMaxZoomLevel(20.0);

        GeoPoint startPoint = new GeoPoint(22.3964, 114.1095);
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(12);

        // 紅線 (Polyline) 設定
        routeLine = new Polyline();
        routeLine.setColor(Color.RED);
        routeLine.setWidth(10.0f);
        mapView.getOverlays().add(routeLine);

        getDBLocation();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getCurrentLocation();

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

                            // 依據紅線（routeLine）的點計算沿路距離，若無則以直線距離計算
                            List<GeoPoint> routePoints = routeLine.getPoints();
                            double distanceToEnd;
                            if (routePoints != null && routePoints.size() > 0) {
                                distanceToEnd = computeRouteDistance(new GeoPoint(latitude, longitude), routePoints);
                            } else {
                                distanceToEnd = calculateDistance(latitude, longitude, Double.parseDouble(DESTINATION_LAT), Double.parseDouble(DESTINATION_LONG));
                            }
                            String locationText = String.format(Locale.getDefault(),
                                    "Current Location:\n%.6f, %.6f\nDistance to end point (route):\n%.2f km",
                                    latitude, longitude, distanceToEnd);
                            currentLocationTextView.setText(locationText);
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
        waypoints.clear();
        routeLine.setPoints(new ArrayList<>());
        mapView.getOverlays().removeIf(overlay -> overlay instanceof BusStopOverlay);

        // 加入起點標記（不加入紅線連線）
        GeoPoint startPoint = null;
        if (START_POINT_LAT != null && START_POINT_LONG != null) {
            try {
                startPoint = new GeoPoint(Double.parseDouble(START_POINT_LAT), Double.parseDouble(START_POINT_LONG));
                busStopItems.add(new BusStopOverlayItem(startPoint, "Start Point", R.drawable.start_point));
                Log.d(TAG, "Start point added at: lat=" + START_POINT_LAT + ", lng=" + START_POINT_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding START point: " + e);
            }
        }

        // 加入 DB 中所有的巴士站 (用於標記與紅線連線)
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

                    // 如果該站與起點相同，則更新起點標記並不加入到 waypoints
                    if (startPoint != null &&
                            Math.abs(busStopPoint.getLatitude() - startPoint.getLatitude()) < 1e-6 &&
                            Math.abs(busStopPoint.getLongitude() - startPoint.getLongitude()) < 1e-6) {
                        // 更新起點標題（若需要）
                        busStopItems.get(0).title = title;
                        continue;
                    }

                    busStopItems.add(new BusStopOverlayItem(busStopPoint, title, R.drawable.bus_stop));
                    // 將 DB 中的巴士站加入 waypoints（用於紅線規劃）
                    waypoints.add(busStopPoint);
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

        // 加入終點標記（若 DB 中已有，則更新圖標與標題；不加入紅線連線）
        if (DESTINATION_LAT != null && DESTINATION_LONG != null) {
            try {
                GeoPoint destPoint = new GeoPoint(Double.parseDouble(DESTINATION_LAT), Double.parseDouble(DESTINATION_LONG));
                boolean isDestSet = false;
                for (BusStopOverlayItem item : busStopItems) {
                    if (Math.abs(item.point.getLatitude() - destPoint.getLatitude()) < 1e-6 &&
                            Math.abs(item.point.getLongitude() - destPoint.getLongitude()) < 1e-6) {
                        item.title = "Destination (" + item.title + ")";
                        item.drawableRes = R.drawable.end_point;
                        isDestSet = true;
                        break;
                    }
                }
                if (!isDestSet) {
                    busStopItems.add(new BusStopOverlayItem(destPoint, "Destination", R.drawable.end_point));
                }
                Log.d(TAG, "Destination point added at: lat=" + DESTINATION_LAT + ", lng=" + DESTINATION_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding DESTINATION point: " + e);
            }
        }

        // 紅線規劃：僅以 DB 中的巴士站 (waypoints) 連線，至少需要兩個點
        if (waypoints.size() >= 2) {
            GeoPoint redLineStart = waypoints.get(0);
            GeoPoint redLineEnd = waypoints.get(waypoints.size() - 1);
            List<GeoPoint> intermediate = new ArrayList<>();
            if (waypoints.size() > 2) {
                intermediate.addAll(waypoints.subList(1, waypoints.size() - 1));
            }
            fetchRouteFromOSRM(redLineStart, redLineEnd, intermediate);
        } else {
            Log.w(TAG, "Not enough bus stops to draw red line.");
        }

        mapView.getOverlays().add(new BusStopOverlay());
        mapView.invalidate();
    }

    // 使用 DB 中巴士站的座標來取得紅線路徑
    private void fetchRouteFromOSRM(GeoPoint startPoint, GeoPoint endPoint, List<GeoPoint> intermediate) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String coordinates = startPoint.getLongitude() + "," + startPoint.getLatitude();
                for (GeoPoint point : intermediate) {
                    coordinates += ";" + point.getLongitude() + "," + point.getLatitude();
                }
                coordinates += ";" + endPoint.getLongitude() + "," + endPoint.getLatitude();

                String url = "https://router.project-osrm.org/route/v1/driving/" + coordinates + "?overview=full&geometries=geojson";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();

                List<GeoPoint> routePoints = parseOSRMJson(jsonData);
                runOnUiThread(() -> {
                    if (routePoints.size() >= 2) {
                        routeLine.setPoints(routePoints);
                        Log.d(TAG, "OSRM route set with " + routePoints.size() + " points.");
                        for (GeoPoint point : routePoints) {
                            Log.d(TAG, "  Route point: lat=" + point.getLatitude() + ", lng=" + point.getLongitude());
                        }
                    } else {
                        Log.w(TAG, "Not enough points from OSRM: " + routePoints.size());
                        Toast.makeText(this, "無法從 OSRM 獲取有效路線", Toast.LENGTH_SHORT).show();
                    }
                    mapView.invalidate();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching OSRM route: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "獲取路線失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<GeoPoint> parseOSRMJson(String jsonData) {
        List<GeoPoint> points = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray geometry = route.getJSONObject("geometry").getJSONArray("coordinates");
                for (int i = 0; i < geometry.length(); i++) {
                    JSONArray coord = geometry.getJSONArray(i);
                    double lng = coord.getDouble(0); // OSRM returns [lng, lat]
                    double lat = coord.getDouble(1);
                    points.add(new GeoPoint(lat, lng));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing OSRM JSON: " + e.getMessage());
        }
        return points;
    }

    // 根據目前位置與路線規劃的點，計算沿路距離
    private double computeRouteDistance(GeoPoint current, List<GeoPoint> routePoints) {
        if (routePoints == null || routePoints.size() == 0) return -1;
        int nearestIndex = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < routePoints.size(); i++) {
            double d = calculateDistance(current.getLatitude(), current.getLongitude(),
                    routePoints.get(i).getLatitude(), routePoints.get(i).getLongitude());
            if (d < minDist) {
                minDist = d;
                nearestIndex = i;
            }
        }
        double total = 0;
        for (int i = nearestIndex; i < routePoints.size() - 1; i++) {
            total += calculateDistance(routePoints.get(i).getLatitude(), routePoints.get(i).getLongitude(),
                    routePoints.get(i+1).getLatitude(), routePoints.get(i+1).getLongitude());
        }
        return total;
    }

    // 自訂的巴士站標記 Overlay，並處理點擊事件以顯示 pop-up window（AlertDialog）
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

                org.osmdroid.api.IGeoPoint geoPoint = new GeoPoint(point.getLatitude(), point.getLongitude());
                android.graphics.Point screenPoint = new android.graphics.Point();
                mapView.getProjection().toPixels(geoPoint, screenPoint);

                Bitmap bitmap = drawableToBitmap(drawable);
                int x = screenPoint.x - bitmap.getWidth() / 2;
                int y = screenPoint.y - bitmap.getHeight();
                canvas.drawBitmap(bitmap, x, y, paint);
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
            android.graphics.Point tapPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            for (BusStopOverlayItem item : busStopItems) {
                org.osmdroid.api.IGeoPoint geoPoint = new GeoPoint(item.point.getLatitude(), item.point.getLongitude());
                android.graphics.Point screenPoint = new android.graphics.Point();
                mapView.getProjection().toPixels(geoPoint, screenPoint);
                // 若點擊位置與標記點接近（40 像素內），則以 AlertDialog 顯示詳情
                if (Math.abs(tapPoint.x - screenPoint.x) < 40 && Math.abs(tapPoint.y - screenPoint.y) < 40) {
                    new AlertDialog.Builder(BusArrivalAlertPage.this)
                            .setTitle("巴士站詳情")
                            .setMessage(item.title)
                            .setPositiveButton("關閉", (dialog, which) -> dialog.dismiss())
                            .show();
                    return true;
                }
            }
            return false;
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

    // 使用 Haversine 公式計算兩點間的距離（公里）
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
