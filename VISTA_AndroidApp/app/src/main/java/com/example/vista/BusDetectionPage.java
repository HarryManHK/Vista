package com.example.vista;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.database.Cursor;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class BusDetectionPage extends AppCompatActivity {

    private static final String TAG = "BusDetectionPage_debug";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final long REFRESH_INTERVAL_MS = 5000;
    private SurfaceView cameraSurfaceView;
    private ImageView detectedImageView;
    private TextView tvCurrentStop;
    private ListView lvArrivalTimes;
    private Socket socket;
    private android.hardware.Camera camera;
    private List<Detection> detections = new ArrayList<>();
    private CustomTextToSpeech customTTS;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private int previewFormat = 0;
    private long lastSentTime = 0;
    private ArrayList<String> etaList;
    private ArrayAdapter<String> etaAdapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private BusDatabaseHelper dbHelper;
    private String routeNumber;
    private int routeSeq;
    private String boundValFromDB;
    private long lastBusDetectAlertTime = 0;
    private static final long BUS_DETECT_ALERT_INTERVAL_MS = 5000;
    // flag to trigger bus-detected audio when ETA#1 crosses below 1 minute
    private boolean timeDetectAlerted = false;
    private long lastEtaSec = -1;

    private static class Detection {
        double xmin, ymin, xmax, ymax, confidence;
        int classId;
        String name;

        Detection(double xmin, double ymin, double xmax, double ymax, double confidence, int classId, String name) {
            this.xmin = xmin;
            this.ymin = ymin;
            this.xmax = xmax;
            this.ymax = ymax;
            this.confidence = confidence;
            this.classId = classId;
            this.name = name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: BusDetectionPage started");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bus_detection_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        Log.d(TAG, "cameraSurfaceView initialized");
        detectedImageView = findViewById(R.id.detectedImageView);
        Log.d(TAG, "detectedImageView initialized");
        tvCurrentStop = findViewById(R.id.tv_current_stop);
        Log.d(TAG, "tvCurrentStop initialized");
        lvArrivalTimes = findViewById(R.id.lv_arrival_times);
        Log.d(TAG, "lvArrivalTimes initialized");
        etaList = new ArrayList<>();
        etaAdapter = new ArrayAdapter<>(this, R.layout.list_item_white_text, etaList);
        lvArrivalTimes.setAdapter(etaAdapter);
        Log.d(TAG, "etaAdapter set with etaList size: " + etaList.size());
        dbHelper = BusDatabaseHelper.getInstance(this);
        fetchRouteInfoFromDB();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchETADataFromKMB(routeNumber, 1);
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            setupCamera();
            setupSocketIO();
        }
        // init TTS for ETA alerts
        customTTS = new CustomTextToSpeech(this);
    }

    private void setupCamera() {
        cameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera = android.hardware.Camera.open();
                    camera.setPreviewDisplay(holder);
                    camera.setDisplayOrientation(90);
                    // match StartDetectBusStopPage: select high-res preview and continuous focus for clarity
                    android.hardware.Camera.Parameters params = camera.getParameters();
                    for (android.hardware.Camera.Size size : params.getSupportedPreviewSizes()) {
                        if (size.width == 1080 && size.height == 1920) {
                            params.setPreviewSize(size.width, size.height);
                            break;
                        }
                    }
                    params.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    camera.setParameters(params);
                    previewWidth = camera.getParameters().getPreviewSize().width;
                    previewHeight = camera.getParameters().getPreviewSize().height;
                    previewFormat = camera.getParameters().getPreviewFormat();

                    camera.setPreviewCallback((data, cam) -> {
                        long now = System.currentTimeMillis();
                        if (now - lastSentTime > 100) {
                            lastSentTime = now;
                            sendImageToServer(data, previewWidth, previewHeight, previewFormat);
                        }
                    });
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up camera: " + e.getMessage(), e);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }
        });
    }

    private void setupSocketIO() {
        try {
            Log.d(TAG, "Setting up Socket.IO connection");
            socket = IO.socket("https://d.harryman.cc");
            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Connected to server"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "Connection error: " + args[0]));
            socket.on("bus", args -> {
                try {
                    Log.d(TAG, "Received bus detections");
                    JSONObject data = (JSONObject) args[0];
                    JSONArray array = data.optJSONArray("detections");
                    detections.clear();
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject det = array.getJSONObject(i);
                            detections.add(new Detection(
                                det.getDouble("xmin"),
                                det.getDouble("ymin"),
                                det.getDouble("xmax"),
                                det.getDouble("ymax"),
                                det.getDouble("confidence"),
                                det.getInt("class_id"),
                                det.getString("name")
                            ));
                        }
                    }
                    runOnUiThread(BusDetectionPage.this::updateUIWithDetections);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing bus detections: " + e.getMessage(), e);
                }
            });
            socket.connect();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Socket.IO: " + e.getMessage(), e);
        }
    }

    private void sendImageToServer(byte[] data, int width, int height, int format) {
        try {
            YuvImage yuvImage = new YuvImage(data, format, width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, baos);
            byte[] jpegData = baos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            ByteArrayOutputStream rotatedBaos = new ByteArrayOutputStream();
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, rotatedBaos);
            String encoded = Base64.encodeToString(rotatedBaos.toByteArray(), Base64.DEFAULT);
            if (socket != null && socket.connected()) {
                socket.emit("bus", encoded);
                //Log.d(TAG, "Image sent");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending image: " + e.getMessage(), e);
        }
    }

    private void updateUIWithDetections() {
        Log.d(TAG, "updateUIWithDetections called");
        if (previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "Preview size not set");
            return;
        }
        Log.d(TAG, "updateUIWithDetections: detections size = " + detections.size());
        Bitmap bmp = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setTextSize(40);
        for (Detection det : detections) {
            Log.d(TAG, "updateUIWithDetections: Detection: " + det.toString());
            canvas.drawRect((float)det.xmin, (float)det.ymin, (float)det.xmax, (float)det.ymax, paint);
            String label = det.name + " " + String.format("%.2f", det.confidence);
            float textWidth = paint.measureText(label);
            float xPos = (float)(det.xmin + (det.xmax - det.xmin - textWidth) / 2);
            float yPos = (float)det.ymin - 10;
            canvas.drawText(label, xPos, yPos, paint);
        }
        detectedImageView.setImageBitmap(bmp);

        // Yolo bus detection alert
        boolean busDetected = false;
        for (Detection d : detections) {
            if (d.name != null && d.name.toLowerCase(Locale.ROOT).contains("bus")) {
                busDetected = true;
                break;
            }
        }
        if (timeDetectAlerted && lastEtaSec > 0) {
            long now = System.currentTimeMillis();
            if (now - lastBusDetectAlertTime > BUS_DETECT_ALERT_INTERVAL_MS) {
                lastBusDetectAlertTime = now;
                final String enSpeak;
                final String zhSpeak;
                if (busDetected) {
                    enSpeak = "Bus detected, ETA #1: " + lastEtaSec + " seconds";
                    zhSpeak = "偵測到巴士，預計第1班在" + lastEtaSec + "秒後到達";
                } else {
                    enSpeak = "ETA #1: " + lastEtaSec + " seconds";
                    zhSpeak = "預計第1班在" + lastEtaSec + "秒後到達";
                }
                runOnUiThread(() -> customTTS.speak(new String[]{enSpeak, zhSpeak}));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
            setupSocketIO();
        } else {
            Log.e(TAG, "Camera permission denied");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void fetchRouteInfoFromDB() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String routeSeqString = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_SEQ));
                String dbBound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
                String stopZh = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_ZH));
                try { routeSeq = Integer.parseInt(routeSeqString); } catch (NumberFormatException e) { routeSeq = 1; }
                boundValFromDB = ("inbound".equalsIgnoreCase(dbBound)) ? "I" : "O";
                tvCurrentStop.setText("Route: " + routeNumber + ", Stop: " + stopZh);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching route info", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void fetchETADataFromKMB(String routeNumber, int serviceType) {
        if (routeNumber == null || routeNumber.isEmpty()) return;
        String url = "https://data.etabus.gov.hk/v1/transport/kmb/route-eta/" + routeNumber + "/" + serviceType;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchETADataFromKMB onFailure", e);
                runOnUiThread(() -> Toast.makeText(BusDetectionPage.this, "Failed to fetch ETA", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unsuccessful API response: " + response.code());
                    runOnUiThread(() -> Toast.makeText(BusDetectionPage.this, "API error: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                String responseBody = response.body().string();
                parseAndDisplayETA(responseBody);
            }
        });
    }

    private void parseAndDisplayETA(String jsonString) {
        Log.d(TAG, "parseAndDisplayETA called");
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray dataArray = root.optJSONArray("data");
            Log.d(TAG, "parseAndDisplayETA: dataArray = " + dataArray);
            if (dataArray == null) {
                Log.d(TAG, "parseAndDisplayETA: dataArray is null");
                return;
            }
            etaList.clear();
            int count = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            for (int i = 0; i < dataArray.length() && count < 3; i++) {
                JSONObject item = dataArray.getJSONObject(i);
                int seq = item.optInt("seq", -1);
                String dir = item.optString("dir", "");
                if (seq == routeSeq && dir.equalsIgnoreCase(boundValFromDB)) {
                    String etaString = item.optString("eta", "");
                    Date etaDate = sdf.parse(etaString);
                    long diffSec = (etaDate.getTime() - System.currentTimeMillis()) / 1000;
                    int etaSeq = item.optInt("eta_seq", -1);
                    if (etaSeq == 1) {
                        lastEtaSec = diffSec;
                        // flag time when under 1 minute (once)
                        if (diffSec > 0 && diffSec < 60 && !timeDetectAlerted) {
                            timeDetectAlerted = true;
                        }
                    }
                    String displayText = calculateTimeDifference(etaString);
                    etaList.add("ETA #" + item.optInt("eta_seq", -1) + ": " + displayText);
                    Log.d(TAG, "parseAndDisplayETA: etaList add: ETA #" + item.optInt("eta_seq", -1) + ": " + displayText);
                    count++;  
                }
            }
            Log.d(TAG, "parseAndDisplayETA: etaList size after update = " + etaList.size());
            runOnUiThread(() -> {
                etaAdapter.notifyDataSetChanged();
                Log.d(TAG, "parseAndDisplayETA: notifyDataSetChanged called, etaList size = " + etaList.size());
            });
        } catch (Exception e) {
            Log.e(TAG, "parseAndDisplayETA error", e);
        }
    }

    private String calculateTimeDifference(String etaString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            Date etaDate = sdf.parse(etaString);
            if (etaDate == null) return "N/A";
            long diff = etaDate.getTime() - System.currentTimeMillis();
            if (diff <= 0) return "Arriving";
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long remSec = seconds % 60;
            return (minutes > 0) ? (minutes + " min " + remSec + " sec") : (remSec + " sec");
        } catch (ParseException e) {
            Log.e(TAG, "calculateTimeDifference error", e);
            return "N/A";
        }
    }
}