package com.example.vista;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.widget.Toast;

public class StartDetectBusStopPage extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private JSONArray stopListData; // 存放 JSON 中 "data" 陣列

    private static final String TAG = "StartDetectBusStopPage_debug";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private SurfaceView cameraSurfaceView;
    private ImageView detectedImageView;
    private Socket socket;
    private android.hardware.Camera camera;
    private List<Detection> detections = new ArrayList<>();
    private long lastSentTime = 0;
    private int previewWidth;
    private int previewHeight;
    private int previewFormat;

    // Inner class to hold detection data
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

        // Check and request camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted");
            setupCamera();
            setupSocketIO();
        }


        // 從 assets 載入 JSON 並解析出 stop list 資料
        loadJsonData();

        // 初始化 NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "本設備不支援 NFC", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 從 assets 載入 JSON 並解析 "data" 陣列
    private void loadJsonData() {
        String jsonStr = "";
        try {
            InputStream is = getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            stopListData = jsonObject.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 啟用 NFC 前景攔截
    @Override
    protected void onResume() {
        super.onResume();
        if(nfcAdapter != null) {
            Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_MUTABLE
            );
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] intentFiltersArray = new IntentFilter[]{tagDetected};
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
    }

    // 關閉前景攔截
    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // 當偵測到 NFC 標籤時觸發
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            // 模擬讀取 NFC 卡中的資料，請根據實際需求實作讀取邏輯
            String cardData = readDataFromTag(tag);
            // 比對讀取到的卡片資料與 JSON 中的 stop 資料
            if(!checkStopData(cardData)) {
                Toast.makeText(this, "數據不匹配", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 模擬讀取 NFC 卡資料 (請根據實際卡片格式進行解析)
    private String readDataFromTag(Tag tag) {
        // 這裡僅回傳示範數據，實際應根據 NFC Tag 格式進行解析
        return "18492910339410B1";  // 請替換為實際讀取邏輯
    }

    // 檢查讀取到的 cardData 是否與 JSON 中的 stop 相符
    private boolean checkStopData(String cardData) {
        if(cardData == null || stopListData == null) {
            return false;
        }
        for (int i = 0; i < stopListData.length(); i++) {
            try {
                JSONObject stopObj = stopListData.getJSONObject(i);
                String stopId = stopObj.getString("stop");
                if(cardData.equalsIgnoreCase(stopId)) {
                    // 若比對成功，顯示站點資訊
                    String nameTC = stopObj.getString("name_tc");
                    String nameEN = stopObj.getString("name_en");
                    showStopInfo(nameTC, nameEN);
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // 使用 AlertDialog 彈出站點資訊
    private void showStopInfo(String nameTC, String nameEN) {
        new AlertDialog.Builder(this)
                .setTitle("站點資訊")
                .setMessage("繁體名稱: " + nameTC + "\nEnglish: " + nameEN)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
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
                    camera.setDisplayOrientation(90); // Rotate preview by 90 degrees
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

                    // Set preview size to 1080x1920 if supported
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
                    previewFormat = parameters.getPreviewFormat(); // Typically NV21
                    Log.d(TAG, "Preview size: " + previewWidth + "x" + previewHeight + ", format: " + previewFormat);

                    // Set focus mode to continuous
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
            socket = IO.socket("https://d.harryman.cc"); // Replace with your server URL
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "Connected to server");
                }
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Connection error: " + args[0]);
                }
            });
            socket.on("detections", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
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
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing detections: " + e.getMessage());
                    }
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

            // Convert YUV data to JPEG
            YuvImage yuvImage = new YuvImage(data, format, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, byteArrayOutputStream);
            byte[] jpegData = byteArrayOutputStream.toByteArray();

            // Decode JPEG to Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

            // Rotate Bitmap by 90 degrees
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Convert rotated Bitmap back to JPEG
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, rotatedStream); // Increased compression quality
            byte[] rotatedJpegData = rotatedStream.toByteArray();

            // Encode to Base64 and send
            String encodedImage = Base64.encodeToString(rotatedJpegData, Base64.DEFAULT);
            if (socket.connected()) {
                socket.emit("image", encodedImage);
                Log.d(TAG, "Image sent");
            } else {
                Log.e(TAG, "Socket not connected, cannot send image");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending image: " + e.getMessage());
        }
    }

    private void updateUIWithDetections() {
        Log.d(TAG, "Updating UI with detections");
        if (previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "Preview size not set");
            return;
        }
        // Swap width and height since the image is rotated 90 degrees
        Bitmap bitmap = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // Set paint properties for the rectangle (box)
        paint.setColor(Color.RED); // Set rectangle color to red
        paint.setStyle(Paint.Style.STROKE); // Make the rectangle outline only
        paint.setStrokeWidth(5); // Set border thickness

        // Set paint properties for the label text
        paint.setColor(Color.WHITE); // Set text color to white for better contrast
        paint.setTextSize(40); // Increase text size for better readability

        for (Detection detection : detections) {
            // Draw bounding box (rectangle) around detected object
            canvas.drawRect(
                    (float) detection.xmin,
                    (float) detection.ymin,
                    (float) detection.xmax,
                    (float) detection.ymax,
                    paint
            );

            // Draw label with confidence score on top of the bounding box
            String label = detection.name + " " + String.format("%.2f", detection.confidence);
            float textWidth = paint.measureText(label);
            float xPos = (float) ((detection.xmin + (detection.xmax - detection.xmin - textWidth) / 2));
            float yPos = (float) detection.ymin - 10;

            canvas.drawText(label, xPos, yPos, paint); // Draw the label
        }

        // Display the resulting image with bounding boxes and labels
        detectedImageView.setImageBitmap(bitmap);
        Log.d(TAG, "UI updated");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted");
            setupCamera();
            setupSocketIO();
        } else {
            Log.d(TAG, "Camera permission denied");
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