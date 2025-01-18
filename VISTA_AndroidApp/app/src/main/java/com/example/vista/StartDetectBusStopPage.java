package com.example.vista;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class StartDetectBusStopPage extends AppCompatActivity
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "StartDetectBusStopPage";

    private SurfaceView cameraSurfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera mCamera;

    private ImageView detectedImageView;

    // Replace with your server's IP (LAN or public) + port (or domain)
    private static final String SERVER_URL = "https://d.harryman.cc";
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_detect_bus_stop_page);

        cameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        detectedImageView = findViewById(R.id.detectedImageView);

        surfaceHolder = cameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);

        // Request camera permission if not granted (Android 6.0+)
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        // Initialize Socket.IO
        try {
            mSocket = IO.socket(SERVER_URL);
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Listen for 'detections' event from server
        if (mSocket != null) {
            mSocket.on("detections", onDetections);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCamera.stopPreview();
            setCameraDisplayOrientation();
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void openCamera() {
        releaseCamera();
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

            if (mCamera != null) {
                // 1. Set 1080p if supported
                setCameraPreviewSize(mCamera, 1920, 1080);

                // 2. Set the orientation
                setCameraDisplayOrientation();

                // 3. Assign the preview holder
                mCamera.setPreviewDisplay(surfaceHolder);

                // 4. Set callback for frames
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to set camera preview size to (wantedWidth x wantedHeight).
     * If not supported, falls back to default camera parameters.
     */
    private void setCameraPreviewSize(Camera camera, int wantedWidth, int wantedHeight) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> supportedSizes = params.getSupportedPreviewSizes();

        if (supportedSizes == null || supportedSizes.isEmpty()) {
            // device doesn't list preview sizes
            return;
        }

        // Try to find a preview size that matches (or is bigger) than 1920x1080
        // or the closest we can get
        Camera.Size bestSize = null;
        for (Camera.Size size : supportedSizes) {
            if (size.width == wantedWidth && size.height == wantedHeight) {
                // Perfect match
                bestSize = size;
                break;
            }
        }

        // If we didn't find an exact match, we can look for something "close"
        // or bigger than 1080p. Or just pick the largest. Example:
        if (bestSize == null) {
            for (Camera.Size size : supportedSizes) {
                if (size.width >= wantedWidth && size.height >= wantedHeight) {
                    bestSize = size;
                    break;
                }
            }
        }

        // If still null, you can pick the largest or do some custom logic
        if (bestSize == null) {
            // fallback: pick the largest preview
            int maxArea = 0;
            for (Camera.Size size : supportedSizes) {
                int area = size.width * size.height;
                if (area > maxArea) {
                    bestSize = size;
                    maxArea = area;
                }
            }
        }

        if (bestSize != null) {
            params.setPreviewSize(bestSize.width, bestSize.height);
            // Adjust other params if needed
            camera.setParameters(params);
            Log.i(TAG, "Set preview size to: " + bestSize.width + "x" + bestSize.height);
        }
    }

    private void setCameraDisplayOrientation() {
        // Force portrait orientation for simplicity
        if (mCamera != null) {
            mCamera.setDisplayOrientation(90);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // Convert NV21 format byte array to JPEG
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                size.width, size.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height),
                70, baos); // 70% quality
        byte[] jpegData = baos.toByteArray();

        // Base64-encode the JPEG
        String base64Image = Base64.encodeToString(jpegData, Base64.DEFAULT);

        // Emit via Socket.IO
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("image", base64Image);
        }
    }

    private Emitter.Listener onDetections = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                try {
                    // 1. Parse JSON from server
                    JSONObject data = (JSONObject) args[0];
                    JSONArray detections = data.getJSONArray("detections");
                    String imageBase64 = data.getString("image");

                    // 2. Decode base64-encoded annotated image
                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    // 3. Display annotated image in ImageView
                    detectedImageView.setImageBitmap(bmp);

                    // 4. Optionally, parse detection details
                    for (int i = 0; i < detections.length(); i++) {
                        JSONObject obj = detections.getJSONObject(i);
                        String className = obj.getString("name");
                        double confidence = obj.getDouble("confidence");

                        Log.d(TAG, "Detected: " + className
                                + " (confidence: " + confidence + ")");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();

        // Close the socket when activity is destroyed
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off("detections", onDetections);
        }
    }
}