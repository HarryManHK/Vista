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
import com.example.vista.map.overlay.BusStopOverlay;
import com.example.vista.map.overlay.BusStopOverlayItem;
import com.example.vista.repository.BusRouteRepository;
import org.osmdroid.views.overlay.Polyline;

import com.example.vista.DatabaseHelper.BusStopInfomationHelper;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import android.Manifest;

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
import java.io.IOException;
// For ISO8601 parsing
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

import com.example.vista.TextToSpeech.CustomTextToSpeech;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import java.util.ArrayList;

public class BusArrivalAlertPage extends AppCompatActivity {

    private String voiceLanguage = "en"; // 預設英文

    private BusDatabaseHelper busDatabaseHelper;
    private BusStopInfomationHelper busStopInfoHelper;

    private static final String TAG = "BusArrivalAlertPage_Debug";

    private CustomTextToSpeech tts;
    private SpeechRecognizerHelper speechHelper;
    private boolean ttsEnabled = true;
    private boolean speechEnabled = true;

    private MapView mapView;
    private Button btnFindBusEditConfirm;
    private Button btnFindBusEditNext;

    private String START_POINT_LAT;
    private String START_POINT_LONG;
    private String DESTINATION_LAT;
    private String DESTINATION_LONG;
    private String routeNumber;
    private String routeBound;

    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 2001;
    private static final String LANGUAGE_EN = "en";
    private static final String LANGUAGE_ZH = "zh";
    private static final double DEFAULT_CENTER_LAT = 22.3964;
    private static final double DEFAULT_CENTER_LNG = 114.1095;
    // 工具方法：安全解析站序
    public String getVoiceLanguage() {
        return voiceLanguage;
    }
    private int parseRouteSeq(String seqStr) {
        try {
            return Integer.parseInt(seqStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    // Fallback: calculate straight-line (Haversine) distance and announce via TTS
    private void fallbackDistance(double lat1, double lon1, double lat2, double lon2, String reason) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // in kilometers

        String zh = String.format("直線距離約 %.1f 公里 (原因: %s)", distance, reason);
        String en = String.format("Straight-line distance: %.1f km (Reason: %s)", distance, reason);
        tts.speak(new String[]{zh, en});
        Log.d(TAG, "[FallbackDistance] " + en);
    }

    private static final double DEFAULT_MIN_ZOOM = 8.0;
    private static final double DEFAULT_MAX_ZOOM = 20.0;

    private BusRouteRepository busRouteRepo;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView currentLocationTextView;
    private LocationRequest locationRequest;

    private MyLocationNewOverlay mLocationOverlay;
    private List<GeoPoint> waypoints = new ArrayList<>();
    private Polyline routeLine;
    // 恢復 busStopItems 聲明，供 overlay 使用
    private List<BusStopOverlayItem> busStopItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 取得語音語言設定
        com.example.vista.DatabaseHelper.SettingDatabaseHelper settingDbHelper = com.example.vista.DatabaseHelper.SettingDatabaseHelper.getInstance(this);
        voiceLanguage = settingDbHelper.getColumnVoiceLanguage();
        if (voiceLanguage == null) voiceLanguage = "en";
        // 初始化 database helper
        busDatabaseHelper = BusDatabaseHelper.getInstance(this);
        busStopInfoHelper = new BusStopInfomationHelper(this);
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_bus_arrival_alert_page);

        // 初始化 busStopItems：自動根據資料庫取得路線與所有站點
        Cursor cursor = busDatabaseHelper.getLatestBusRoute();
        if (cursor != null && cursor.moveToFirst()) {
            routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
            routeBound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
            cursor.close();
        }
        Cursor stopsCursor = busStopInfoHelper.getAllStopsForRoute(routeNumber, routeBound);
        busStopItems.clear();
        if (stopsCursor != null && stopsCursor.moveToFirst()) {
            do {
                String stopId = stopsCursor.getString(stopsCursor.getColumnIndexOrThrow("bus_stop_id"));
                String title = stopsCursor.getString(stopsCursor.getColumnIndexOrThrow("bus_stop_name"));
                double lat = stopsCursor.getDouble(stopsCursor.getColumnIndexOrThrow("bus_stop_lat"));
                double lng = stopsCursor.getDouble(stopsCursor.getColumnIndexOrThrow("bus_stop_long"));
                busStopItems.add(new BusStopOverlayItem(stopId, new GeoPoint(lat, lng), title, 0));
            } while (stopsCursor.moveToNext());
            stopsCursor.close();
        }
        // 根據用戶設定的起點 stopId 設定 currentTargetStopIndex
String userStartStopId = busDatabaseHelper.getStartPointStopId();
currentTargetStopIndex = 0;
for (int i = 0; i < busStopItems.size(); i++) {
    if (busStopItems.get(i).stopId.equals(userStartStopId)) {
        currentTargetStopIndex = i;
        break;
    }
}

        btnFindBusEditConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnFindBusEditNext = findViewById(R.id.btnFindBusEditNext);
        currentLocationTextView = findViewById(R.id.CurrentLocation);

        // 語音播報初始化
        tts = new CustomTextToSpeech(this);
        // 語音辨識初始化
        speechHelper = new SpeechRecognizerHelper(this, new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        msg = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        msg = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        msg = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        msg = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        msg = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        msg = "No match";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        msg = "RecognitionService busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        msg = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        msg = "No speech input";
                        break;
                    default:
                        msg = "Unknown error";
                        break;
                }
                Log.e(TAG, "SpeechRecognizer error: " + msg + " (" + error + ")");
                Toast.makeText(BusArrivalAlertPage.this, "語音辨識失敗: " + msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d(TAG, "SpeechRecognizer matches: " + matches);
                if (matches != null && !matches.isEmpty()) {
                    handleVoiceCommand(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        btnFindBusEditConfirm.setOnLongClickListener(v -> {
            if (speechEnabled) {
                // 強制使用粵語語音辨識（如需普通話可改 zh-TW）
                speechHelper.startListening("zh-HK");
                Toast.makeText(this, "請開始說話...", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapView = findViewById(R.id.map);
        mapView.getOverlays().clear();

        busRouteRepo = new BusRouteRepository(this);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(8.0);
        mapView.setMaxZoomLevel(20.0);

        GeoPoint initPoint = new GeoPoint(22.3964, 114.1095);
        mapView.getController().setCenter(initPoint);
        mapView.getController().setZoom(12);

        routeLine = new Polyline();
        routeLine.setPoints(new ArrayList<>()); // 保證 points 不為 null
        routeLine.setColor(Color.RED);
        routeLine.setWidth(10.0f);
        if (!mapView.getOverlays().contains(routeLine)) { // 確保 routeLine 被加入覆蓋物
            mapView.getOverlays().add(routeLine);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 檢查定位權限
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ||
                androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }

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

        // 頁面一開啟就自動啟動到站提醒
        reminderEnabled = true;
        reminderStopIndex = 1;
        reminderHasSpoken = false;
        tts.speak(new String[]{"已啟動到站提醒", "Bus stop reminder activated"});

        showLoading(true);
        printAllRecord();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDBLocation();
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        mapView.getController().setZoom(18);
        mapView.invalidate();
    }

    private void showLoading(boolean show) {
        // TODO: 實作 loading spinner 控制，建議用 ProgressBar 或 DialogFragment
    }

    private void printAllRecord() {
        try (Cursor cursor = busRouteRepo.getAllStopsRaw()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_ID));
                    String routeNumber = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_ROUTE_NUMBER));
                    String bound = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BOUND));
                    String stopNameEn = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME));
                    String stopNameZh = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME_ZH));
                    int seq = cursor.getInt(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_SEQ));
                    String stopId = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_ID));
                    double lat = cursor
                            .getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LAT));
                    double lng = cursor
                            .getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LONG));

                    Log.d(TAG,
                            "Record: ID=" + id +
                                    ", routeNumber=" + routeNumber +
                                    ", bound=" + bound +
                                    ", stopNameEn=" + stopNameEn +
                                    ", stopNameZh=" + stopNameZh +
                                    ", seq=" + seq +
                                    ", stopId=" + stopId +
                                    ", lat=" + lat +
                                    ", lng=" + lng);
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No records found in bus_stops table.");
            }
        }
    }

    // --- NLP 語音指令處理 ---
    private void handleVoiceCommand(String command) {
        com.example.vista.VoiceControl.NLPService nlpService = new com.example.vista.VoiceControl.DeepseekNLPApiService();
        nlpService.processText(command, new com.example.vista.VoiceControl.NLPCallback() {
            @Override
            public void onSuccess(org.json.JSONObject commandJson) {
                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "NLP raw json: " + commandJson.toString());
                        String action = commandJson.optString("action", null);
                        if ((action == null || action.trim().isEmpty() || action.trim().equals("其他")) && commandJson.has("content")) {
                            String content = commandJson.optString("content");
                            try {
                                content = content.trim();
                                if (content.startsWith("```json")) {
                                    content = content.substring(7).trim();
                                }
                                if (content.startsWith("```") && content.endsWith("```")) {
                                    content = content.substring(3, content.length() - 3).trim();
                                } else if (content.startsWith("```")) {
                                    content = content.substring(3).trim();
                                }
                                org.json.JSONObject innerJson = new org.json.JSONObject(content);
                                action = innerJson.optString("action", "");
                            } catch (Exception e) {
                                Log.d(TAG, "Failed to parse content as JSON: " + content);
                                Toast.makeText(BusArrivalAlertPage.this, "語音指令內容解析失敗，請再說一次", Toast.LENGTH_SHORT).show();
                                action = null;
                            }
                        }
                        if (action == null || action.trim().isEmpty() || action.trim().equals("其他")) {
                            Log.d(TAG, "NLP 無法判斷 action，忽略本次指令");
                            return;
                        }
                        action = action.trim().toLowerCase(java.util.Locale.ROOT);
                        Log.d(TAG, "NLP action: " + action);
                        switch (action) {
                            case "查距離":
                                Log.d(TAG, "[NLP] case 查距離: before getDistanceToNextStop");
                                getDistanceToNextStop();
                                Log.d(TAG, "[NLP] case 查距離: after getDistanceToNextStop");
                                break;
                            case "查巴士到站時間":
                                Log.d(TAG, "[NLP] case 查巴士到站時間: before getBusArrivalTime");
                                getBusArrivalTime();
                                Log.d(TAG, "[NLP] case 查巴士到站時間: after getBusArrivalTime");
                                break;
                            case "巴士到站提醒":
                                Log.d(TAG, "[NLP] case 啟動巴士到站提醒: before");
                                reminderEnabled = true;
                                reminderHasSpoken = false;
                                Log.d(TAG, "[NLP] 啟動提醒 reminderEnabled=" + reminderEnabled + ", reminderHasSpoken=" + reminderHasSpoken);
                                tts.speak(new String[]{"已啟動到站提醒", "Bus stop reminder activated"});
                                Log.d(TAG, "[NLP] case 啟動巴士到站提醒: after");
                                break;
                            case "關閉巴士到站提醒":
                                Log.d(TAG, "[NLP] case 關閉巴士到站提醒: before");
                                reminderEnabled = false;
                                Log.d(TAG, "[NLP] 關閉提醒 reminderEnabled=" + reminderEnabled);
                                break;
                            case "下一站":
                            case "next stop":
                                Log.d(TAG, "[NLP] case 下一站: before");
                                int nextIndex = currentTargetStopIndex + 1;
                                if (busStopItems != null && nextIndex < busStopItems.size()) {
                                    String nextStopName = busStopItems.get(nextIndex).title;
                                    String zh = "下一站" + nextStopName;
                                    String en = "Next stop " + nextStopName;
                                    if ("zh".equalsIgnoreCase(voiceLanguage)) {
                                        tts.speak(new String[]{zh});
                                        Log.d(TAG, "[NLP] 下一站播報: " + zh);
                                    } else {
                                        tts.speak(new String[]{en});
                                        Log.d(TAG, "[NLP] 下一站播報: " + en);
                                    }
                                } else {
                                    String zh = "已到達終點";
                                    String en = "You have reached the final stop";
                                    if ("zh".equalsIgnoreCase(voiceLanguage)) {
                                        tts.speak(new String[]{zh});
                                        Log.d(TAG, "[NLP] 已到達終點播報: " + zh);
                                    } else {
                                        tts.speak(new String[]{en});
                                        Log.d(TAG, "[NLP] 已到達終點播報: " + en);
                                    }
                                }
                                break;
                            default:
                                Log.d(TAG, "[NLP] case default");
                                tts.speak(new String[]{"Sorry, I didn't understand.", "抱歉，我不明白你的指令"});
                        }
                    } catch (Exception e) {
                        tts.speak(new String[]{"Error parsing NLP command.", "解析語音指令時發生錯誤"});
                        Toast.makeText(BusArrivalAlertPage.this, "語音指令解析失敗，請再說一次", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    tts.speak(new String[]{"NLP服務異常，請重試。", "NLP服務異常，請重試。"});
                    Toast.makeText(BusArrivalAlertPage.this, "語音指令解析失敗，請再試一次", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // --- 全局提醒控制變數 ---
    private boolean reminderEnabled = false;
    private boolean reminderHasSpoken = false;
    private int reminderStopIndex = 1;
    // --- 自動到站追蹤 ---
    private int currentTargetStopIndex = 1; // 0=起點, 1=第一站
    // --- 定位回調 ---
    private com.google.android.gms.location.LocationCallback locationCallback = new com.google.android.gms.location.LocationCallback() {
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

                    // 根據 busStopItems 追蹤距離目標站
                    if (reminderEnabled && busStopItems != null && busStopItems.size() > 1 && currentTargetStopIndex < busStopItems.size()) {
                        BusStopOverlayItem targetStop = busStopItems.get(currentTargetStopIndex);
                        double stopLat = targetStop.point.getLatitude();
                        double stopLng = targetStop.point.getLongitude();
                        double dist = calculateDistance(latitude, longitude, stopLat, stopLng) * 1000; // m
                        if (dist < 30 && !reminderHasSpoken) {
                            // 到站語音
                            String zh = "已到達" + targetStop.title;
                            String en = "Arrived at " + targetStop.title;
                            tts.speak(new String[]{zh, en});
                            reminderHasSpoken = true;
                            // 切換下一站
                            if (currentTargetStopIndex < busStopItems.size() - 1) {
                                currentTargetStopIndex++;
                                reminderHasSpoken = false; // 準備下個站
                                // 播報新目標站
                                BusStopOverlayItem nextStop = busStopItems.get(currentTargetStopIndex);
                                String zhNext = "下一站：" + nextStop.title;
                                String enNext = "Next stop: " + nextStop.title;
                                tts.speak(new String[]{zhNext, enNext});
                            } else {
                                // 終點
                                String zhEnd = "已到達終點，旅程完成";
                                String enEnd = "Arrived at destination. Journey complete.";
                                tts.speak(new String[]{zhEnd, enEnd});
                            }
                        } else if (dist >= 100) {
                            reminderHasSpoken = false; // 離開範圍可再次提醒
                        }
                    }

                    // 顯示定位資訊
                    double distanceToEnd = calculateDistance(latitude, longitude,
                            Double.parseDouble(DESTINATION_LAT != null ? DESTINATION_LAT : "0"),
                            Double.parseDouble(DESTINATION_LONG != null ? DESTINATION_LONG : "0"));
                    String locationText = String.format(Locale.getDefault(),
                            "Current Location:\n%.6f, %.6f\nDistance to end point (route):\n%.2f km",
                            latitude, longitude, distanceToEnd);
                    currentLocationTextView.setText(locationText);
                    // 這裡可加自動到站提醒邏輯
                }
            }
        }
    };

    private void getCurrentLocation() {
        // ...原有內容...
        // 當位置更新時自動語音播報距離與下一站
        // 可在 locationCallback 裡加入：
        // if (ttsEnabled) tts.speak(new String[]{locationText, locationTextZh});

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Toast.makeText(this, "Location permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void getDBLocation() {
        try (Cursor cursor = busRouteRepo.getLatestBusRoute()) {
            if (cursor != null && cursor.moveToFirst()) {
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                routeBound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
                START_POINT_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LAT));
                START_POINT_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LONG));
                DESTINATION_LAT = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LAT));
                DESTINATION_LONG = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LONG));

                Log.d(TAG, "Loaded route=" + routeNumber + ", bound=" + routeBound);
                Log.d(TAG, "Rendering route: " + routeNumber + ", bound: " + routeBound);

                clearRouteOverlays();

                busRouteRepo.fetchAndStoreBusStops(routeNumber, routeBound,
                        new BusStopInfomationHelper.OnFetchCompleteListener() {
                            @Override
                            public void onFetchComplete(boolean success) {
                                if (success) {
                                    Log.d(TAG, "Bus stops fetched and stored successfully for route=" + routeNumber
                                            + ", bound=" + routeBound);
                                    addBusStopsToMap();
                                } else {
                                    Log.e(TAG, "Failed to fetch bus stops for route=" + routeNumber + ", bound="
                                            + routeBound);
                                    Toast.makeText(BusArrivalAlertPage.this, "無法獲取巴士站資料", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Log.d(TAG, "No bus route data found.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading DB Location data: " + e);
            Toast.makeText(this, "Error loading DB Location data", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearRouteOverlays() {
        mapView.getOverlays().removeIf(overlay -> overlay instanceof BusStopOverlay || overlay == routeLine);
        busStopItems.clear();
        waypoints.clear();
        if (routeLine != null) {
            routeLine.setPoints(new ArrayList<>());
        }
        if (!mapView.getOverlays().contains(routeLine)) { // 確保 routeLine 在清除後重新加入
            mapView.getOverlays().add(routeLine);
        }
        Log.d(TAG, "Cleared all route overlays.");
    }

    private void addBusStopsToMap() {
        // ...原有內容...
        // 加入語音播報下一站範例：
        // if (ttsEnabled && busStopItems.size() > 1) {
        // String en = "Next stop: " + busStopItems.get(1).title;
        // String zh = "下一站：" + busStopItems.get(1).title;
        // tts.speak(new String[]{en, zh});
        // }

        GeoPoint startPoint = null;
        if (START_POINT_LAT != null && START_POINT_LONG != null) {
            try {
                startPoint = new GeoPoint(Double.parseDouble(START_POINT_LAT), Double.parseDouble(START_POINT_LONG));
                busStopItems.add(new BusStopOverlayItem("start", startPoint, "Start Point", R.drawable.start_point));
                Log.d(TAG, "Start point added at: lat=" + START_POINT_LAT + ", lng=" + START_POINT_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding START point: " + e);
            }
        }

        try (Cursor cursor = busRouteRepo.getAllStopsForRoute(routeNumber, routeBound)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double lat = cursor
                            .getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LAT));
                    double lng = cursor
                            .getDouble(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_LONG));
                    String stopNameEn = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME));
                    String stopNameZh = cursor
                            .getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_NAME_ZH));
                    GeoPoint busStopPoint = new GeoPoint(lat, lng);
                    String title = stopNameEn + " (" + stopNameZh + ")";

                    if (startPoint != null &&
                            Math.abs(busStopPoint.getLatitude() - startPoint.getLatitude()) < 1e-6 &&
                            Math.abs(busStopPoint.getLongitude() - startPoint.getLongitude()) < 1e-6) {
                        busStopItems.get(0).title = title;
                        continue;
                    }

                    String stopId = cursor.getString(cursor.getColumnIndexOrThrow(BusStopInfomationHelper.COLUMN_BUS_STOP_ID));
                    busStopItems.add(new BusStopOverlayItem(stopId, busStopPoint, title, R.drawable.bus_stop));
                    waypoints.add(busStopPoint);
                    Log.d(TAG, "Bus stop added at: lat=" + lat + ", lng=" + lng);
                } while (cursor.moveToNext());
            } else {
                Log.w(TAG, "No bus stops found for route=" + routeNumber + ", bound=" + routeBound);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding bus stops: " + e);
        }

        if (DESTINATION_LAT != null && DESTINATION_LONG != null) {
            try {
                GeoPoint destPoint = new GeoPoint(Double.parseDouble(DESTINATION_LAT),
                        Double.parseDouble(DESTINATION_LONG));
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
                    busStopItems.add(new BusStopOverlayItem("destination", destPoint, "Destination", R.drawable.end_point));
                }
                Log.d(TAG, "Destination point added at: lat=" + DESTINATION_LAT + ", lng=" + DESTINATION_LONG);
            } catch (Exception e) {
                Log.e(TAG, "Error adding DESTINATION point: " + e);
            }
        }

        Log.d(TAG, "Waypoints size: " + waypoints.size());
        if (waypoints.size() >= 2) {
            GeoPoint redLineStart = waypoints.get(0);
            GeoPoint redLineEnd = waypoints.get(waypoints.size() - 1);
            List<GeoPoint> intermediate = new ArrayList<>();
            if (waypoints.size() > 2) {
                intermediate.addAll(waypoints.subList(1, waypoints.size() - 1));
            }
            Log.d(TAG, "Fetching route from OSRM: start=" + redLineStart + ", end=" + redLineEnd + ", intermediates="
                    + intermediate.size());
            fetchRouteFromOSRM(redLineStart, redLineEnd, intermediate);
        } else {
            Log.w(TAG, "Not enough bus stops to draw red line. Waypoints: " + waypoints.size());
            Toast.makeText(this, "巴士站數量不足，無法繪製路線", Toast.LENGTH_SHORT).show();
        }

        mapView.getOverlays().add(new BusStopOverlay(this, busStopItems));
        mapView.invalidate();
    }

    private void fetchRouteFromOSRM(GeoPoint startPoint, GeoPoint endPoint, List<GeoPoint> intermediate) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String coordinates = startPoint.getLongitude() + "," + startPoint.getLatitude();
                for (GeoPoint point : intermediate) {
                    coordinates += ";" + point.getLongitude() + "," + point.getLatitude();
                }
                coordinates += ";" + endPoint.getLongitude() + "," + endPoint.getLatitude();

                String url = "https://router.project-osrm.org/route/v1/driving/" + coordinates
                        + "?overview=full&geometries=geojson";
                Log.d(TAG, "OSRM URL: " + url);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new Exception("OSRM API request failed with code: " + response.code());
                }

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
                        Toast.makeText(this, "OSRM 返回的路線點不足", Toast.LENGTH_SHORT).show();
                    }
                    mapView.invalidate(); // 確保地圖重繪
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
                    double lng = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    points.add(new GeoPoint(lat, lng));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing OSRM JSON: " + e.getMessage());
        }
        return points;
    }

    private double computeRouteDistance(GeoPoint current, List<GeoPoint> routePoints) {
        if (routePoints == null || routePoints.size() == 0)
            return -1;
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
        // 回傳最近點到終點的距離
        double totalDist = 0.0;
        for (int i = nearestIndex; i < routePoints.size() - 1; i++) {
            totalDist += calculateDistance(
                    routePoints.get(i).getLatitude(), routePoints.get(i).getLongitude(),
                    routePoints.get(i + 1).getLatitude(), routePoints.get(i + 1).getLongitude());
        }
        return totalDist;
    }

    // --- 其他業務邏輯與方法 ---
    // 計算兩點間距離（單位：公里）
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
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
        final double R = 6371.0; // 地球半徑，單位：公里
        return R * c;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void handleConfirmButtonClick() {
        Toast.makeText(this, "Confirm button clicked", Toast.LENGTH_SHORT).show();
    }

    private void handleNextButtonClick() {
        reminderEnabled = !reminderEnabled;
        String zh = reminderEnabled ? "已啟動到站提醒" : "已關閉到站提醒";
        String en = reminderEnabled ? "Bus stop reminder activated" : "Bus stop reminder deactivated";
        tts.speak(new String[]{zh, en});
        Toast.makeText(this, zh, Toast.LENGTH_SHORT).show();
        // 重置提醒狀態
        if (reminderEnabled) reminderHasSpoken = false;
    }

    // 查詢目前到下一站的距離，並語音播報
    // 使用 OSRM API 查詢駕車距離
    private void getDistanceToNextStop() {
        try {
            // 1. 取得目前目標站 stopId（第一次為起點，之後為下一站）
            String stopId;
            if (currentTargetStopIndex == 0) {
                // 第一次：查 database 起點
                stopId = busDatabaseHelper.getStartPointStopId();
                Log.d(TAG, "[Distance] Using startPoint stopId=" + stopId);
            } else {
                // 之後：查 busStopItems 的下一站
                if (busStopItems != null && currentTargetStopIndex < busStopItems.size()) {
                    stopId = busStopItems.get(currentTargetStopIndex).stopId;
                    Log.d(TAG, "[Distance] Using next stopId=" + stopId + " (index=" + currentTargetStopIndex + ")");
                } else {
                    tts.speak(new String[]{"已到達終點", "Arrived at destination"});
                    Log.d(TAG, "[Distance] 已到達終點，無下一站");
                    return;
                }
            }
            // 2. 查 database 拿經緯度
            Cursor stopCursor = busStopInfoHelper.getStopById(stopId);
            if (stopCursor != null && stopCursor.moveToFirst()) {
                double stopLat = stopCursor.getDouble(stopCursor.getColumnIndexOrThrow("bus_stop_lat"));
                double stopLng = stopCursor.getDouble(stopCursor.getColumnIndexOrThrow("bus_stop_long"));
                Log.d(TAG, "[Distance] stopId=" + stopId + ", stopLat=" + stopLat + ", stopLng=" + stopLng);
                stopCursor.close();
                try {
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(this, location -> {
                                if (location != null) {
                                    double currentLat = location.getLatitude();
                                    double currentLng = location.getLongitude();
                                    int maxRetries = 2;
                                    Log.d(TAG, "[Distance] Querying OSRM: from (" + currentLat + ", " + currentLng + ") to (" + stopLat + ", " + stopLng + ")");
                                    makeOsrmRequestWithRetry(currentLat, currentLng, stopLat, stopLng, maxRetries);
                                } else {
                                    tts.speak(new String[]{"無法取得目前位置", "Cannot get current location"});
                                    Log.d(TAG, "[Distance] 無法取得目前位置");
                                }
                            })
                            .addOnFailureListener(this, e -> {
                                tts.speak(new String[]{"定位失敗", "Failed to get location"});
                                Log.d(TAG, "[Distance] 定位失敗");
                            });
                } catch (SecurityException e) {
                    tts.speak(new String[]{"無定位權限", "No location permission"});
                    Log.d(TAG, "[Distance] 無定位權限");
                }
            } else {
                tts.speak(new String[]{"查無該站資料", "Stop info not found"});
                Log.d(TAG, "[Distance] 查無該站資料 stopId=" + stopId);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDistanceToNextStop error: " + e.getMessage(), e);
            Toast.makeText(BusArrivalAlertPage.this, "查詢距離時發生錯誤", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeOsrmRequestWithRetry(double currentLat, double currentLng, double stopLat, double stopLng, int retriesLeft) {
        String osrmUrl = String.format(
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                currentLng, currentLat, stopLng, stopLat
        );
        Log.d(TAG, "[Distance] OSRM URL: " + osrmUrl);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(osrmUrl).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Error fetching OSRM route: " + e.getMessage());
                if (retriesLeft > 0) {
                    Log.d(TAG, "Retrying OSRM request, retries left: " + (retriesLeft - 1));
                    makeOsrmRequestWithRetry(currentLat, currentLng, stopLat, stopLng, retriesLeft - 1);
                } else {
                    tts.speak(new String[]{"路線查詢逾時，改用直線距離", "Route timeout, fallback to straight-line"});
                    fallbackDistance(currentLat, currentLng, stopLat, stopLng, "OSRM timeout");
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "OSRM response not successful: " + response.code());
                    tts.speak(new String[]{"路線查詢失敗，改用直線距離", "Route failed, fallback to straight-line"});
                    fallbackDistance(currentLat, currentLng, stopLat, stopLng, "OSRM failed");
                    return;
                }
                String jsonData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray routes = jsonObject.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        double distance = route.getDouble("distance"); // meters
                        double distanceKm = distance / 1000.0;
                        String zh = String.format("距離下一站約 %.1f 公里", distanceKm);
                        String en = String.format("About %.1f kilometers to next stop", distanceKm);
                        String speakText = (voiceLanguage != null && (voiceLanguage.equalsIgnoreCase("zh") || voiceLanguage.startsWith("zh"))) ? zh : en;
                        tts.speak(new String[]{speakText});
                        Toast.makeText(BusArrivalAlertPage.this, speakText, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "[Distance] OSRM route distance: " + distanceKm + " km");
                    } else {
                        String zhNoRoute = "查無路線資料，改用直線距離";
                        String enNoRoute = "No route data, fallback to straight-line";
                        String speakTextNoRoute = (voiceLanguage != null && (voiceLanguage.equalsIgnoreCase("zh") || voiceLanguage.startsWith("zh"))) ? zhNoRoute : enNoRoute;
                        tts.speak(new String[]{speakTextNoRoute});
                        Toast.makeText(BusArrivalAlertPage.this, speakTextNoRoute, Toast.LENGTH_SHORT).show();
                        fallbackDistance(currentLat, currentLng, stopLat, stopLng, "OSRM no data");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing OSRM response: " + e.getMessage());
                }
            }
        });
    }

    // 查詢第一班巴士到站時間，並語音播報
    private void getBusArrivalTime() {
        try {
            // 1. 取得 routeNumber, bound, start_point_seq
            Cursor cursor = busDatabaseHelper.getLatestBusRoute();
            if (cursor == null || !cursor.moveToFirst()) {
                tts.speak(new String[]{"查無路線或起點資料", "Route or start point not found"});
                Log.d(TAG, "[ArrivalTime] 缺少路線資料 (cursor null)");
                return;
            }
            String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
            String bound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
            String stopId = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_STOP_ID));
            String stopNameZh = busStopInfoHelper.getStopNameZh(stopId);
            String stopNameEn = busStopInfoHelper.getStopNameEn(stopId);
            String routeSeqString = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_SEQ));
            final int routeSeq = parseRouteSeq(routeSeqString);

            // DEBUG: log 查詢參數
            Log.d(TAG, "[ArrivalTime] params: routeNumber=" + routeNumber + ", stopId=" + stopId + ", bound=" + bound + ", routeSeq=" + routeSeq);

            cursor.close();

            if (routeNumber == null || bound == null || stopId == null) {
                tts.speak(new String[]{"查無路線或起點資料", "Route or start point not found"});
                Log.d(TAG, "[ArrivalTime] 缺少 stopId/routeNumber/bound");
                return;
            }

            // 2. 轉換 bound 成 KMB API 的 "I"/"O"
            final String kmbBound = "outbound".equalsIgnoreCase(bound) ? "O" : "I";

            // 3. 查詢 KMB ETA API（改用 /route-eta/{route}/1，與 UI 一致）
            OkHttpClient client = new OkHttpClient();
            String url = String.format("https://data.etabus.gov.hk/v1/transport/kmb/route-eta/%s/1", routeNumber);
            Log.d(TAG, "[ArrivalTime] KMB ETA URL: " + url);
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        tts.speak(new String[]{"查詢失敗，請檢查網絡", "Failed to query ETA, please check network"});
                        Log.e(TAG, "[ArrivalTime] KMB ETA API error: " + e.getMessage());
                    });
                }
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            tts.speak(new String[]{"查詢失敗", "Failed to query ETA"});
                            Log.e(TAG, "[ArrivalTime] KMB ETA API response not successful");
                        });
                        return;
                    }
                    String jsonData = response.body().string();
                    try {
                        JSONObject root = new JSONObject(jsonData);
                        JSONArray dataArray = root.optJSONArray("data");
                        Log.d(TAG, "[ArrivalTime] KMB raw data: " + dataArray);
                        if (dataArray == null || dataArray.length() == 0) {
                            runOnUiThread(() -> {
                                tts.speak(new String[]{"暫無到站資料", "No ETA data available"});
                                Log.d(TAG, "[ArrivalTime] No ETA data");
                            });
                            return;
                        }
                        // 跟 ShowArriveTimePage 一致：只用 seq + dir 過濾
                        JSONObject matchedEta = null;
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            String dirFromJson = item.optString("dir", "");
                            int seqVal = item.optInt("seq", -1);
                            if (seqVal == routeSeq && dirFromJson.equalsIgnoreCase(kmbBound)) {
                                matchedEta = item;
                                break;
                            }
                        }
                        // fallback: 若本方向沒資料，自動查詢另一個方向（只用 seq + dir）
                        if (matchedEta == null) {
                            String fallbackDir = kmbBound.equals("I") ? "O" : "I";
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                String dirFromJson = item.optString("dir", "");
                                int seqVal = item.optInt("seq", -1);
                                if (seqVal == routeSeq && dirFromJson.equalsIgnoreCase(fallbackDir)) {
                                    matchedEta = item;
                                    break;
                                }
                            }
                            if (matchedEta != null) {
                                final String fallbackMsg = kmbBound.equals("I") ? "僅有去程班次" : "僅有回程班次";
                                runOnUiThread(() -> {
                                    tts.speak(new String[]{fallbackMsg, "Only opposite direction buses available"});
                                });
                            }
                        }
                        if (matchedEta == null) {
                            runOnUiThread(() -> {
                                tts.speak(new String[]{"暫無到站資料", "No ETA data available"});
                                Log.d(TAG, "[ArrivalTime] No matching ETA data (seq=" + routeSeq + ", dir=" + kmbBound + ")");
                            });
                            return;
                        }
                        String etaTime = matchedEta.optString("eta", "");
                        if (etaTime.isEmpty()) {
                            runOnUiThread(() -> {
                                tts.speak(new String[]{"暫無到站資料", "No ETA data available"});
                                Log.d(TAG, "[ArrivalTime] ETA time empty");
                            });
                            return;
                        }
                        // 5. 播報第一筆 ETA（優化語句：XX分後到達/即將到達）
                        java.time.ZonedDateTime etaDateTime = java.time.ZonedDateTime.parse(etaTime);
                        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Hong_Kong"));
                        long minutes = java.time.Duration.between(now, etaDateTime).toMinutes();
                        String zh, en;
                        if (minutes <= 1) {
                            zh = stopNameZh + " 即將到達";
                            en = "Arriving soon at " + stopNameEn;
                        } else {
                            zh = String.format("%s %d 分後到達", stopNameZh, minutes);
                            en = String.format("%d min to %s", minutes, stopNameEn);
                        }
                        String speakText;
                        if (voiceLanguage != null && (voiceLanguage.equalsIgnoreCase("zh") || voiceLanguage.startsWith("zh"))) {
                            speakText = zh;
                        } else {
                            speakText = en;
                        }
                        runOnUiThread(() -> {
                            tts.speak(new String[]{speakText});
                            Toast.makeText(BusArrivalAlertPage.this, speakText, Toast.LENGTH_SHORT).show();
                        });
                        Log.d(TAG, "[ArrivalTime] ETA: " + etaTime);
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            tts.speak(new String[]{"到站資料解析失敗", "Failed to parse ETA data"});
                            Log.e(TAG, "[ArrivalTime] ETA parse error: " + e.getMessage());
                        });
                    }
                }
            });
        } catch (Exception e) {
            tts.speak(new String[]{"查詢到站時間時發生錯誤", "Error occurred while querying ETA"});
            Log.e(TAG, "[ArrivalTime] getBusArrivalTime error: " + e.getMessage());
        }
    }


    }

