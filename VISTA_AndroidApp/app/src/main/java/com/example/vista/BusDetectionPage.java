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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class BusDetectionPage extends AppCompatActivity {

    private static final String TAG = "BusDetectionPage_debug";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private SurfaceView cameraSurfaceView;
    private ImageView detectedImageView;
    private Socket socket;
    private android.hardware.Camera camera;
    private List<Detection> detections = new ArrayList<>();
    private int previewWidth = 0;
    private int previewHeight = 0;
    private int previewFormat = 0;
    private long lastSentTime = 0;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bus_detection_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        detectedImageView = findViewById(R.id.detectedImageView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            setupCamera();
            setupSocketIO();
        }
    }

    private void setupCamera() {
        cameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera = android.hardware.Camera.open();
                    camera.setPreviewDisplay(holder);
                    camera.setDisplayOrientation(90);
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
                Log.d(TAG, "Image sent");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending image: " + e.getMessage(), e);
        }
    }

    private void updateUIWithDetections() {
        if (previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "Preview size not set");
            return;
        }
        Bitmap bmp = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setTextSize(40);
        for (Detection det : detections) {
            canvas.drawRect((float)det.xmin, (float)det.ymin, (float)det.xmax, (float)det.ymax, paint);
            String label = det.name + " " + String.format("%.2f", det.confidence);
            float textWidth = paint.measureText(label);
            float xPos = (float)(det.xmin + (det.xmax - det.xmin - textWidth) / 2);
            float yPos = (float)det.ymin - 10;
            canvas.drawText(label, xPos, yPos, paint);
        }
        detectedImageView.setImageBitmap(bmp);
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
}