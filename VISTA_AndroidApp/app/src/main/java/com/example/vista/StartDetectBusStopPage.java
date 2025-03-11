package com.example.vista;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StartDetectBusStopPage extends AppCompatActivity {

    private static final String TAG = "StartDetectBusStopPage_debug";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int NFC_PERMISSION_CODE = 101;
    private SurfaceView cameraSurfaceView;
    private ImageView detectedImageView;
    private Socket socket;
    private android.hardware.Camera camera;
    private List<Detection> detections = new ArrayList<>();
    private long lastSentTime = 0;
    private int previewWidth;
    private int previewHeight;
    private int previewFormat;
    private NfcAdapter nfcAdapter;
    private List<BusStop> busStops = new ArrayList<>();
    private PendingIntent nfcPendingIntent;
    private BusDatabaseHelper dbHelper;  // 添加這行

    // BusStop內部類
    private static class BusStop {
        String stop;
        String nameEn;
        String nameTc;
        String nameSc;
        double lat;
        double lon;

        BusStop(String stop, String nameEn, String nameTc, String nameSc, double lat, double lon) {
            this.stop = stop;
            this.nameEn = nameEn;
            this.nameTc = nameTc;
            this.nameSc = nameSc;
            this.lat = lat;
            this.lon = lon;
        }
    }

    // Detection內部類
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
        setContentView(R.layout.activity_start_detect_bus_stop_page);

        cameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        detectedImageView = findViewById(R.id.detectedImageView);

        // 初始化 DatabaseHelper
        dbHelper = BusDatabaseHelper.getInstance(this);  // 添加這行

        // 初始化NFC適配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "此設備不支持NFC", Toast.LENGTH_LONG).show();
            //finish();
            //return;
        }

        // 創建PendingIntent用於NFC前台分發
        nfcPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
        );

        // 檢查和請求相機權限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted");
            setupCamera();
            setupSocketIO();
        }

        // 檢查和請求NFC權限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting NFC permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NFC}, NFC_PERMISSION_CODE);
        } else {
            Log.d(TAG, "NFC permission already granted");
        }

        // 讀取busStop.json數據
        loadBusStopsFromJson();
    }

    private void setupCamera() {
        Log.d(TAG, "Setting up camera");
        camera = android.hardware.Camera.open();
        SurfaceHolder holder = cameraSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    Log.d(TAG, "Surface created, starting camera preview");
                    camera.setPreviewDisplay(holder);
                    camera.setDisplayOrientation(90);
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Error setting camera preview: " + e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                if (camera != null) {
                    Log.d(TAG, "Surface changed, setting preview callback");
                    android.hardware.Camera.Parameters parameters = camera.getParameters();
                    List<android.hardware.Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

                    for (android.hardware.Camera.Size size : supportedPreviewSizes) {
                        if (size.width == 1080 && size.height == 1920) {
                            parameters.setPreviewSize(size.width, size.height);
                            break;
                        }
                    }

                    camera.setParameters(parameters);

                    android.hardware.Camera.Size previewSize = parameters.getPreviewSize();
                    previewWidth = previewSize.width;
                    previewHeight = previewSize.height;
                    previewFormat = parameters.getPreviewFormat();
                    Log.d(TAG, "Preview size: " + previewWidth + "x" + previewHeight + ", format: " + previewFormat);

                    if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        camera.setParameters(parameters);
                    }

                    camera.setPreviewCallback((data, camera) -> {
                        if (System.currentTimeMillis() - lastSentTime > 500) {
                            sendImageToServer(data, previewWidth, previewHeight, previewFormat);
                            lastSentTime = System.currentTimeMillis();
                        }
                    });
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (camera != null) {
                    Log.d(TAG, "Surface destroyed, stopping camera preview");
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
            socket.on("detections", args -> {
                try {
                    Log.d(TAG, "Received detections from server");
                    JSONObject data = (JSONObject) args[0];
                    JSONArray detectionsArray = data.getJSONArray("detections");
                    detections.clear();
                    for (int i = 0; i < detectionsArray.length(); i++) {
                        JSONObject detection = detectionsArray.getJSONObject(i);
                        detections.add(new Detection(
                                detection.getDouble("xmin"),
                                detection.getDouble("ymin"),
                                detection.getDouble("xmax"),
                                detection.getDouble("ymax"),
                                detection.getDouble("confidence"),
                                detection.getInt("class_id"),
                                detection.getString("name")
                        ));
                    }
                    Log.d(TAG, "Parsed " + detections.size() + " detections");
                    runOnUiThread(() -> updateUIWithDetections());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing detections: " + e.getMessage());
                }
            });
            socket.connect();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Socket.IO: " + e.getMessage());
        }
    }

    private void sendImageToServer(byte[] data, int width, int height, int format) {
        try {
            Log.d(TAG, "Sending image to server");
            YuvImage yuvImage = new YuvImage(data, format, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, byteArrayOutputStream);
            byte[] jpegData = byteArrayOutputStream.toByteArray();

            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, rotatedStream);
            byte[] rotatedJpegData = rotatedStream.toByteArray();

            String encodedImage = Base64.encodeToString(rotatedJpegData, Base64.DEFAULT);
            if (socket != null && socket.connected()) {
                socket.emit("image", encodedImage);
                Log.d(TAG, "Image sent");
            } else {
                Log.e(TAG, "Socket not connected, cannot send image");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending image: " + e.toString(), e);
        }
    }

    private void updateUIWithDetections() {
        Log.d(TAG, "Updating UI with detections");
        if (previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "Preview size not set");
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setTextSize(40);

        for (Detection detection : detections) {
            canvas.drawRect(
                    (float) detection.xmin,
                    (float) detection.ymin,
                    (float) detection.xmax,
                    (float) detection.ymax,
                    paint
            );
            String label = detection.name + " " + String.format("%.2f", detection.confidence);
            float textWidth = paint.measureText(label);
            float xPos = (float) ((detection.xmin + (detection.xmax - detection.xmin - textWidth) / 2));
            float yPos = (float) detection.ymin - 10;
            canvas.drawText(label, xPos, yPos, paint);

            if (detection.name != null && detection.name.contains("CircleBusStopRoute")) {
                camera = null;
            }
        }
        detectedImageView.setImageBitmap(bitmap);
        Log.d(TAG, "UI updated");
    }

    private void loadBusStopsFromJson() {
        try {
            InputStream inputStream = getAssets().open("busStop.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, "UTF-8");

            JSONObject jsonObject = new JSONObject(json);
            JSONArray dataArray = jsonObject.getJSONArray("data");
            busStops.clear();

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject stop = dataArray.getJSONObject(i);
                busStops.add(new BusStop(
                        stop.getString("stop"),
                        stop.getString("name_en"),
                        stop.getString("name_tc"),
                        stop.getString("name_sc"),
                        stop.getDouble("lat"),
                        stop.getDouble("long")
                ));
            }
            Log.d(TAG, "Loaded " + busStops.size() + " bus stops from busStop.json");
        } catch (Exception e) {
            Log.e(TAG, "Error loading busStop.json: " + e.toString(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            Log.d(TAG, "Enabling foreground dispatch");
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            Log.d(TAG, "Disabling foreground dispatch");
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called with action: " + intent.getAction());
        if (intent.getAction() != null && (
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                        NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                        NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()))) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                Log.d(TAG, "Tag detected");
                handleNfcTag(tag);
            } else {
                Log.e(TAG, "No tag found in NFC intent");
            }
        } else {
            Log.d(TAG, "Non-NFC intent or action is null, checking for tag anyway");
            if (nfcAdapter != null) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag != null) {
                    Log.d(TAG, "Tag detected from intent extras");
                    handleNfcTag(tag);
                } else {
                    Log.e(TAG, "No tag found in intent extras");
                }
            }
        }
    }

    private void handleNfcTag(Tag tag) {
        Log.d(TAG, "Handling NFC tag: " + tag.toString());
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null && ndefMessage.getRecords().length > 0) {
                    byte[] payload = ndefMessage.getRecords()[0].getPayload();
                    // 第一個字節低6位表示語言碼長度
                    int languageCodeLength = payload[0] & 0x3F;
                    // 判斷編碼：如果第7位為0，則使用 UTF-8，否則使用 UTF-16
                    String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
                    // 從 payload 中跳過狀態位和語言碼，剩餘部分為真正的文本
                    String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                    Log.d(TAG, "NFC Data (NDEF): " + text);
                    checkNfcDataWithJson(text);
                } else {
                    Log.d(TAG, "No NDEF message found");
                }
            } catch (IOException | FormatException e) {
                Log.e(TAG, "Error reading NDEF tag: " + e.toString(), e);
                // 若發生 IOException，嘗試直接讀取標籤的 ID 作 fallback
                byte[] id = tag.getId();
                if (id != null && id.length > 0) {
                    String tagId = bytesToHex(id);
                    Log.d(TAG, "Tag ID: " + tagId);
                    checkNfcDataWithJson(tagId);
                }
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing NDEF connection: " + e.toString(), e);
                }
            }
        } else {
            Log.d(TAG, "Tag is not NDEF, checking raw bytes");
            byte[] id = tag.getId();
            if (id != null && id.length > 0) {
                String tagId = bytesToHex(id);
                Log.d(TAG, "Tag ID: " + tagId);
                checkNfcDataWithJson(tagId);
            } else {
                Log.e(TAG, "No tag ID available");
            }
        }
    }



    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void checkNfcDataWithJson(String nfcData) {
        Log.d(TAG, "Checking NFC data: " + nfcData);

        // 獲取資料庫中的 start_point_stop_id
        String startPointStopId = dbHelper.getStartPointStopId();

        for (BusStop busStop : busStops) {
            if (busStop.stop.equals(nfcData)) {
                String message;
                if (startPointStopId != null && startPointStopId.equals(nfcData)) {
                    message = "已到達起點站！\n站點: " + busStop.nameTc;
                    // 如果需要，可以在這裡添加額外的起點到達邏輯
                } else {
                    message = "目前的車站不是起點站\n或是同一車站的不同等待位置\n" + busStop.nameTc;
                }

                String finalMessage = message;
                runOnUiThread(() -> Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show());
                Log.d(TAG, "NFC data matched: " + busStop.nameTc +
                        ", isStartPoint: " + (startPointStopId != null && startPointStopId.equals(nfcData)));
                return;
            }
        }

        // 如果沒有匹配的站點
        runOnUiThread(() -> Toast.makeText(this, "NFC數據不匹配", Toast.LENGTH_LONG).show());
        Log.d(TAG, "No matching bus stop found for NFC data");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted");
            setupCamera();
            setupSocketIO();
        } else if (requestCode == NFC_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "NFC permission granted");
        } else {
            Log.d(TAG, "Permission denied: " + requestCode);
        }
    }

    private void stopCameraPreview() {
        if (camera != null) {
            Log.d(TAG, "Stopping camera preview");
            camera.setPreviewCallback(null); // 停止預覽回調
            camera.stopPreview(); // 停止預覽
            camera.release(); // 釋放相機資源
            camera = null; // 清空相機對象
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            Log.d(TAG, "Disconnecting Socket.IO");
            socket.disconnect();
        }
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}