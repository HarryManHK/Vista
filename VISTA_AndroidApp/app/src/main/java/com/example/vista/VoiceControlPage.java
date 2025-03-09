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

import com.example.vista.VoiceControl.VoiceCommandFactory;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
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
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceControlPage extends AppCompatActivity {

    private boolean isRecording = false;
    private MediaRecorder recorder;
    private String audioFilePath;
    private MaterialButton btnStart;
    private TextView tvResult;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "VoiceControlPage";

    // Replace with your actual DeepSeek API key
    private static final String DEEPSEEK_API_KEY = "sk-nvapiliqrltqkcpitxkbbxuwcmdfupyimcejzjfaydxihylb";

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
     * When the ASR response is received, it sends the recognized text to DeepSeek (non-streaming)
     * and then calls VoiceCommandFactory to execute the corresponding action.
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
                JSONObject jsonPayload = new JSONObject();
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

                JSONObject responseJson = new JSONObject(response);
                // Assuming the ASR API returns the recognized text under key "res"
                String resultText = responseJson.optString("res", "No result");
                runOnUiThread(() -> tvResult.setText(resultText));

                // Send the recognized text to DeepSeek in non-streaming mode
                sendTextToDeepseekNonStreaming(resultText);

            } catch (Exception e) {
                Log.e(TAG, "sendAudioToServer error: " + e.getMessage());
                runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Sends the recognized text (from ASR) to DeepSeek (non-streaming) for NLP analysis.
     * The prompt instructs DeepSeek to return one of the following standardized JSON formats:
     *
     * 1. 如果用戶想搭巴士，請返回格式：
     * {
     *   "action": "搭巴士",
     *   "routeNumber": "42A",
     *   "startPoint": "荃灣",
     *   "destination": "佐敦"
     * }
     *
     * 2. 如果用戶想查巴士到站時間，請返回格式：
     * {
     *   "action": "查巴士到站時間"
     * }
     *
     * 請只返回 JSON 格式，不要其他額外文字，且全部使用繁體中文。
     */
    private void sendTextToDeepseekNonStreaming(String text) {
        new Thread(() -> {
            try {
                String prompt = "請根據以下用戶語音轉文字結果解析出使用者的命令，並嚴格按照以下兩種 JSON 格式返回：\n" +
                        "1. 如果用戶想搭巴士，請返回格式：\n" +
                        "{\n  \"action\": \"搭巴士\",\n  \"routeNumber\": \"42A\",\n  \"startPoint\": \"荃灣\",\n  \"destination\": \"佐敦\"\n}\n" +
                        "2. 如果用戶想查巴士到站時間，請返回格式：\n" +
                        "{\n  \"action\": \"查巴士到站時間\"\n}\n" +
                        "請只返回 JSON 格式，不要其他額外文字，且全部使用繁體中文。\n" +
                        "用戶語音轉文字結果：" + text;

                JSONObject payload = new JSONObject();
                payload.put("model", "deepseek-ai/DeepSeek-R1-Distill-Llama-8B");
                payload.put("stream", false);
                payload.put("max_tokens", 5000);
                payload.put("temperature", 0.7);
                payload.put("top_p", 0.9);
                payload.put("frequency_penalty", 0.0);
                payload.put("n", 1);

                JSONObject messageObject = new JSONObject();
                messageObject.put("role", "user");
                messageObject.put("content", prompt);
                payload.put("messages", new JSONArray().put(messageObject));

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(90, TimeUnit.SECONDS)
                        .readTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(90, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        payload.toString()
                );

                Request request = new Request.Builder()
                        .url("https://api.siliconflow.cn/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "DeepSeek Non-Streaming Response: " + responseBody);
                    String correctedJson = responseBody.replace("“", "\"").replace("”", "\"");

                    // Locate the "content" key and extract the JSON command using brace matching.
                    int contentIndex = correctedJson.indexOf("\"content\":");
                    if (contentIndex != -1) {
                        int firstBrace = correctedJson.indexOf("{", contentIndex);
                        if (firstBrace != -1) {
                            int braceCount = 0;
                            int i = firstBrace;
                            for (; i < correctedJson.length(); i++) {
                                char c = correctedJson.charAt(i);
                                if (c == '{') {
                                    braceCount++;
                                } else if (c == '}') {
                                    braceCount--;
                                    if (braceCount == 0) {
                                        break;
                                    }
                                }
                            }
                            if (braceCount == 0) {
                                String commandJson = correctedJson.substring(firstBrace, i + 1).trim();
                                // Remove newline and carriage return characters
                                commandJson = commandJson.replaceAll("[\\n\\r]", "").trim();

                                Log.d(TAG, "Extracted command JSON: " + commandJson);
                                final String commandToExecute = commandJson;
                                runOnUiThread(() -> {
                                    tvResult.setText(commandToExecute);
                                    VoiceCommandFactory.executeCommand(VoiceControlPage.this, commandToExecute);
                                });
                            } else {
                                Log.e(TAG, "Failed to match braces for JSON command.");
                                runOnUiThread(() -> tvResult.setText("Failed to extract JSON command."));
                            }
                        } else {
                            Log.e(TAG, "No '{' found after \"content\":");
                            runOnUiThread(() -> tvResult.setText("No command JSON found."));
                        }
                    } else {
                        Log.e(TAG, "\"content\" key not found in response.");
                        runOnUiThread(() -> tvResult.setText("\"content\" key not found."));
                    }
                } else {
                    Log.e(TAG, "DeepSeek API error: " + response.code());
                    runOnUiThread(() -> tvResult.setText("DeepSeek API error: " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "sendTextToDeepseekNonStreaming error: " + e.getMessage());
                runOnUiThread(() -> tvResult.setText("DeepSeek Error: " + e.getMessage()));
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