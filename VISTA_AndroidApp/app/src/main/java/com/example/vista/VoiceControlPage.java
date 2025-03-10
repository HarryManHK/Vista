// File: VoiceControlPage.java
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

import com.example.vista.VoiceControl.siliconflow_Deepseek_NLP;
import com.example.vista.VoiceControl.NLPCallback;
import com.example.vista.VoiceControl.NLPService;
import com.example.vista.VoiceControl.VoiceCommandFactory;
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
    private static final String TAG = "VoiceControlPage_debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control_page);

        Log.d(TAG, "onCreate: Activity started");

        // Apply window insets for proper layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check and request permissions if needed
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }

        btnStart = findViewById(R.id.btnVoiceControl_start);
        tvResult = findViewById(R.id.tvResult);
        // Set file path (using .3gp format)
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/test.3gp";
        Log.d(TAG, "Audio file path: " + audioFilePath);

        btnStart.setOnClickListener(v -> {
            if (!checkPermissions()) {
                tvResult.setText("Permissions not granted");
                ActivityCompat.requestPermissions(VoiceControlPage.this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
            if (!isRecording) {
                startRecording();
                btnStart.setText("Stop");
            } else {
                stopRecording();
                btnStart.setText("Start");
                sendAudioToServer();
            }
            isRecording = !isRecording;
        });
    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return recordPermission == PackageManager.PERMISSION_GRANTED &&
                internetPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
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
            Log.e(TAG, "startRecording error: " + e.getMessage());
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
            Log.e(TAG, "stopRecording error: " + e.getMessage());
        }
    }

    /**
     * Sends the recorded audio file to an ASR service for voice-to-text conversion.
     */
    private void sendAudioToServer() {
        new Thread(() -> {
            try {
                File audioFile = new File(audioFilePath);
                if (!audioFile.exists()) {
                    runOnUiThread(() -> tvResult.setText("Error: Audio file not found."));
                    return;
                }
                byte[] audioBytes = new byte[(int) audioFile.length()];
                FileInputStream fis = new FileInputStream(audioFile);
                int bytesRead = fis.read(audioBytes);
                fis.close();
                Log.d(TAG, "Bytes read: " + bytesRead);

                // Convert audio to Base64 string
                String base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);
                org.json.JSONObject jsonPayload = new org.json.JSONObject();
                jsonPayload.put("wav", base64Audio);

                URL url = new URL("https://speechtotextapi.harryman.cc/asr");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                OutputStream os = connection.getOutputStream();
                os.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                InputStream in = new BufferedInputStream(connection.getInputStream());
                String response = convertStreamToString(in);
                in.close();
                connection.disconnect();
                Log.d(TAG, "ASR Response: " + response);

                org.json.JSONObject responseJson = new org.json.JSONObject(response);
                // Assuming the ASR API returns the recognized text under key "res"
                String resultText = responseJson.optString("res", "No result");
                runOnUiThread(() -> tvResult.setText(resultText));

                // Process the recognized text with DeepSeek NLP
                processASRResult(resultText);

            } catch (Exception e) {
                Log.e(TAG, "sendAudioToServer error: " + e.getMessage());
                runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void processASRResult(String resultText) {
        NLPService nlpService = new siliconflow_Deepseek_NLP();
        nlpService.processText(resultText, new NLPCallback() {
            @Override
            public void onSuccess(JSONObject commandJson) {
                // Update UI on the main thread
                runOnUiThread(() -> {
                    try {
                        String action = commandJson.getString("action");
                        String displayText;
                        if ("搭巴士".equals(action)) {
                            displayText = String.format("搭巴士: %s 從 %s 到 %s",
                                    commandJson.getString("routeNumber"),
                                    commandJson.getString("startPoint"),
                                    commandJson.getString("destination"));
                        } else {
                            displayText = action; // e.g., "查巴士到站時間"
                        }
                        tvResult.setText(displayText);
                        // Execute command if needed (using VoiceCommandFactory)
                        VoiceCommandFactory.executeCommand(VoiceControlPage.this, commandJson.toString());
                    } catch (Exception e) {
                        tvResult.setText("Error parsing command: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
            }
        });
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

    public void updateResult(String message) {
        runOnUiThread(() -> tvResult.setText(message));
    }
}