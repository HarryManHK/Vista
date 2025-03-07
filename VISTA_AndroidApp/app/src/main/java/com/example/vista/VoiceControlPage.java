package com.example.vista;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class VoiceControlPage extends AppCompatActivity {

    private boolean isRecording = false;
    private MediaRecorder recorder;
    private String audioFilePath;
    private MaterialButton btnStart;
    private TextView tvResult;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "VoiaceControl_debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control_page);

        Log.d(TAG, "onCreate: Activity started");

        // Apply window insets for proper layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            Log.d(TAG, "Applied window insets: " + systemBars.toString());
            return insets;
        });

        // Check and request permissions if needed
        if (!checkPermissions()) {
            Log.d(TAG, "Permissions not granted. Requesting permissions.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All permissions already granted.");
        }

        btnStart = findViewById(R.id.btnVoiceControl_start);
        tvResult = findViewById(R.id.tvResult);
        // Set file path with appropriate extension (using 3gp for THREE_GPP format)
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/test.3gp";
        Log.d(TAG, "Audio file path: " + audioFilePath);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check permissions before proceeding
                if (!checkPermissions()) {
                    Log.d(TAG, "Permissions not granted. Cannot record audio.");
                    tvResult.setText("Permissions not granted");
                    ActivityCompat.requestPermissions(VoiceControlPage.this,
                            new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                    return;
                }

                if (!isRecording) {
                    Log.d(TAG, "Button clicked: Starting recording");
                    startRecording();
                    btnStart.setText("Stop");
                } else {
                    Log.d(TAG, "Button clicked: Stopping recording");
                    stopRecording();
                    btnStart.setText("Start");
                    sendAudioToServer();
                }
                isRecording = !isRecording;
            }
        });
    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(TAG, "Permissions check: RECORD_AUDIO=" + recordPermission + ", INTERNET=" + internetPermission + ", WRITE_EXTERNAL_STORAGE=" + storagePermission);
        return recordPermission == PackageManager.PERMISSION_GRANTED &&
                internetPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }
            if (allGranted) {
                Log.d(TAG, "Permissions granted.");
            } else {
                Log.d(TAG, "Permissions denied.");
                tvResult.setText("Permissions denied. Cannot record audio.");
            }
        }
    }

    private void startRecording() {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFilePath);
            recorder.prepare();
            recorder.start();
            Log.d(TAG, "Recording started successfully");
        } catch (Exception e) {
            Log.d(TAG, "startRecording error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                Log.d(TAG, "Recording stopped successfully");
            }
        } catch (Exception e) {
            Log.d(TAG, "stopRecording error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendAudioToServer() {
        new Thread(() -> {
            try {
                Log.d(TAG, "sendAudioToServer: Reading audio file");
                File audioFile = new File(audioFilePath);
                if (!audioFile.exists()) {
                    Log.d(TAG, "sendAudioToServer error: File does not exist");
                    runOnUiThread(() -> tvResult.setText("Error: Audio file not found."));
                    return;
                }
                byte[] audioBytes = new byte[(int) audioFile.length()];
                FileInputStream fis = new FileInputStream(audioFile);
                int bytesRead = fis.read(audioBytes);
                fis.close();
                Log.d(TAG, "Bytes read from audio file: " + bytesRead);

                // Convert the audio file to a base64 string
                String base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("wav", base64Audio);
                Log.d(TAG, "JSON payload created: " + jsonPayload.toString());

                URL url = new URL("https://speechtotextapi.harryman.cc/asr");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                Log.d(TAG, "HTTP connection set up");

                OutputStream os = connection.getOutputStream();
                os.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
                os.close();
                Log.d(TAG, "JSON payload sent");

                InputStream in = new BufferedInputStream(connection.getInputStream());
                String response = convertStreamToString(in);
                in.close();
                connection.disconnect();
                Log.d(TAG, "Response received: " + response);

                JSONObject responseJson = new JSONObject(response);
                String resultText = responseJson.optString("res", "No result");
                Log.d(TAG, "Parsed result text: " + resultText);

                runOnUiThread(() -> tvResult.setText(resultText));

            } catch (Exception e) {
                Log.d(TAG, "sendAudioToServer error: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}