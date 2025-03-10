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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                // 定義提示詞，要求 DeepSeek 返回特定格式的 JSON
                String prompt = "請根據以下用戶語音轉文字結果解析出使用者的命令，並嚴格按照以下兩種 JSON 格式返回：\n" +
                        "1. 如果用戶想搭巴士，請返回格式：\n" +
                        "{\n  \"action\": \"搭巴士\",\n  \"routeNumber\": \"42A\",\n  \"startPoint\": \"荃灣\",\n  \"destination\": \"佐敦\"\n}\n" +
                        "2. 如果用戶想查巴士到站時間，請返回格式：\n" +
                        "{\n  \"action\": \"查巴士到站時間\"\n}\n" +
                        "請只返回 JSON 格式，不要其他額外文字，且全部使用繁體中文。\n" +
                        "用戶語音轉文字結果：" + text;

                // 構建 API 請求的 JSON 負載
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

                // 設置 HTTP 客戶端，增加超時時間
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

                // 發送請求並獲取回應
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "DeepSeek 原始回應: " + responseBody);

                    // 解析 API 回應
                    JSONObject responseJson = new JSONObject(responseBody);
                    JSONArray choices = responseJson.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        String content = choice.getJSONObject("message").getString("content");
                        Log.d(TAG, "提取的內容: " + content);

                        // 將 content 解析為 JSON 物件
                        JSONObject commandJson = new JSONObject(content);
                        String action = commandJson.getString("action");
                        Log.d(TAG, "解析出的動作: " + action);

                        // 檢查 action 是否為空
                        if (action.isEmpty()) {
                            Log.e(TAG, "動作字段為空");
                            runOnUiThread(() -> tvResult.setText("錯誤：動作字段為空"));
                        } else {
                            // 根據 action 格式化顯示文字
                            String displayText;
                            if ("搭巴士".equals(action)) {
                                displayText = String.format("搭巴士: %s 從 %s 到 %s",
                                        commandJson.getString("routeNumber"),
                                        commandJson.getString("startPoint"),
                                        commandJson.getString("destination"));
                            } else {
                                displayText = action; // 例如 "查巴士到站時間"
                            }
                            final String finalDisplayText = displayText;
                            runOnUiThread(() -> {
                                tvResult.setText(finalDisplayText);
                                VoiceCommandFactory.executeCommand(VoiceControlPage.this, content);
                            });
                        }
                    } else {
                        Log.e(TAG, "回應中沒有 choices");
                        runOnUiThread(() -> tvResult.setText("錯誤：回應中沒有選擇"));
                    }
                } else {
                    Log.e(TAG, "DeepSeek API 錯誤碼: " + response.code());
                    runOnUiThread(() -> tvResult.setText("DeepSeek API 錯誤: " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "sendTextToDeepseekNonStreaming 錯誤: " + e.getMessage());
                runOnUiThread(() -> tvResult.setText("DeepSeek 錯誤: " + e.getMessage()));
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
