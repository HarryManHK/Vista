// File: VoiceControlPage.java
package com.example.vista;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class VoiceControlPage extends AppCompatActivity {

    private MaterialButton btnStart;
    private TextView tvResult;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int SPEECH_REQUEST_CODE = 300;
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
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET},
                    PERMISSION_REQUEST_CODE);
        }

        btnStart = findViewById(R.id.btnVoiceControl_start);
        tvResult = findViewById(R.id.tvResult);

        btnStart.setOnClickListener(v -> {
            if (!checkPermissions()) {
                tvResult.setText("Permissions not granted");
                ActivityCompat.requestPermissions(VoiceControlPage.this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET},
                        PERMISSION_REQUEST_CODE);
                return;
            }
            startGoogleVoiceRecognition();
        });
    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        return recordPermission == PackageManager.PERMISSION_GRANTED &&
                internetPermission == PackageManager.PERMISSION_GRANTED;
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

    private void startGoogleVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-HK"); // Or Locale.getDefault()
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "請開始說話...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            tvResult.setText("Google 語音服務不可用: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String resultText = results.get(0);
                tvResult.setText(resultText);
                processASRResult(resultText);
            } else {
                tvResult.setText("未能識別語音");
            }
        }
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

    public void updateResult(String message) {
        runOnUiThread(() -> tvResult.setText(message));
    }
}