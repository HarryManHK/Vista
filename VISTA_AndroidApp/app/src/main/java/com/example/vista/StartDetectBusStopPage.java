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
import java.util.ArrayList;
import java.util.List;

public class StartDetectBusStopPage extends AppCompatActivity {

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
                    android.hardware.Camera.Size previewSize = parameters.getPreviewSize();
                    previewWidth = previewSize.width;
                    previewHeight = previewSize.height;
                    previewFormat = parameters.getPreviewFormat(); // Typically NV21
                    Log.d(TAG, "Preview size: " + previewWidth + "x" + previewHeight + ", format: " + previewFormat);

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
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, byteArrayOutputStream);
            byte[] jpegData = byteArrayOutputStream.toByteArray();

            // Decode JPEG to Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

            // Rotate Bitmap by 90 degrees
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Convert rotated Bitmap back to JPEG
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, rotatedStream);
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
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setTextSize(20);
        for (Detection detection : detections) {
            canvas.drawRect((float) detection.xmin, (float) detection.ymin, (float) detection.xmax, (float) detection.ymax, paint);
            canvas.drawText(detection.name + " " + String.format("%.2f", detection.confidence),
                    (float) detection.xmin, (float) detection.ymin - 10, paint);
        }
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