package com.example.vista.ChatBot;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vista.R;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;

import android.location.Geocoder;
import android.location.Address;
import java.util.Locale;

public class ChatBotPage extends AppCompatActivity {
    private com.example.vista.TextToSpeech.CustomTextToSpeech customTextToSpeech;

    private Deepseek_R1_siliconflow_Client chatGPTClient;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private Button voiceInputButton;
    private ProgressBar loadingProgressBar;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    private static final int VOICE_INPUT_REQUEST_CODE = 1011;
    private static final int PERMISSION_REQUEST_CODE = 1022;

    private static final String TAG = "ChatBotPage_debug";  // For logging

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        customTextToSpeech = new com.example.vista.TextToSpeech.CustomTextToSpeech(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_bot_page);

        // Enable immersive mode (hide navigation bar for fullscreen chat)
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        // Initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        voiceInputButton.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                return;
            }
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "請說話...");
            try {
                startActivityForResult(intent, VOICE_INPUT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "此裝置不支援語音輸入", Toast.LENGTH_SHORT).show();
            }
        });

        // 啟動時自動檢查權限
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatGPTClient = new Deepseek_R1_siliconflow_Client();

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                try {
                    sendUserMessage(message);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "未授權麥克風權限，語音輸入無法使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_INPUT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                messageInput.setText(results.get(0));
                messageInput.setSelection(messageInput.getText().length());
            }
        }
    }

    private void sendUserMessage(String message) throws JSONException {
        // Always show the user message in the chat UI
        messageList.add(new Message(message, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
        // Play TTS for user message
        if (customTextToSpeech != null) {
            customTextToSpeech.speak(new String[]{message});
        }
        // Clear the input box
        messageInput.setText("");

        // Check for weather keywords
        if (message.contains("天氣") || message.toLowerCase().contains("weather")) {
            fetchWeatherAndRespond();
            return;
        }

        // Show the loading indicator
        loadingProgressBar.setVisibility(ProgressBar.VISIBLE);

        // Log the user message
        Log.d(TAG, "User message: " + message);

        // Add a "thinking" placeholder message
        final int thinkingMessagePosition = messageList.size();
        messageList.add(new Message("Bot is thinking...", false));
        chatAdapter.notifyItemInserted(thinkingMessagePosition);
        chatRecyclerView.scrollToPosition(thinkingMessagePosition);

        // Send the message to the bot
        chatGPTClient.sendMessage(message, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    messageList.set(thinkingMessagePosition, new Message("Error: " + e.getMessage(), false));
                    chatAdapter.notifyItemChanged(thinkingMessagePosition);
                    loadingProgressBar.setVisibility(ProgressBar.GONE);  // Hide the loading indicator
                });

                // Log the error
                Log.e(TAG, "Error during API call: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Read the response and log it
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                            String line;
                            StringBuilder partialResponse = new StringBuilder();

                            while ((line = reader.readLine()) != null) {
                                // Log the streamed response
                                Log.d(TAG, "Bot response stream: " + line);

                                // Check if the line indicates the end of the stream
                                if (line.contains("[DONE]")) {
                                    // Stream ended, stop showing the loading indicator
                                    runOnUiThread(() -> loadingProgressBar.setVisibility(ProgressBar.GONE));
                                    break;
                                }

                                // Process and append the response chunk
                                if (line.startsWith("data: ")) {
                                    // Extract the actual content from the "data: " part
                                    String jsonResponse = line.substring(6); // Remove the 'data: ' part
                                    JSONObject jsonObject = new JSONObject(jsonResponse);
                                    String content = jsonObject.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("delta")
                                            .optString("content", "");

                                    // Only process and display non-null and non-empty content
                                    if (!content.equals("null") && !content.trim().isEmpty()) {
                                        // Append the content gradually
                                        for (char c : content.toCharArray()) {
                                            partialResponse.append(c);
                                            final String currentText = partialResponse.toString();
                                            // Update the UI with each new chunk
                                            runOnUiThread(() -> {
                                                messageList.set(thinkingMessagePosition, new Message(currentText, false));
                                                chatAdapter.notifyItemChanged(thinkingMessagePosition);
                                                chatRecyclerView.scrollToPosition(thinkingMessagePosition);
                                                // 自動語音播報
                                                if (customTextToSpeech != null) {
                                                    customTextToSpeech.speak(new String[]{currentText, currentText});
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                    }
                } else {
                    runOnUiThread(() -> {
                        messageList.set(thinkingMessagePosition, new Message("Request failed", false));
                        chatAdapter.notifyItemChanged(thinkingMessagePosition);
                        loadingProgressBar.setVisibility(ProgressBar.GONE);  // Hide the loading indicator
                    });

                    // Log the failed response
                    Log.e(TAG, "Request failed: " + response.message());
                }
            }
        });
    }

    private void fetchWeatherAndRespond() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            addBotMessage("請授權定位權限以查詢天氣。");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    fetchWeatherFromAPI(lat, lon);
                } else {
                    addBotMessage("無法取得您的位置，請確保已開啟定位權限。");
                }
            }
        });
    }

    private void fetchWeatherFromAPI(double lat, double lon) {
        // Get language setting from database
        SettingDatabaseHelper dbHelper = SettingDatabaseHelper.getInstance(this);
        String lang = "zh";
        String country = "HK";
        String[] langSetting = dbHelper.getLanguageSetting();
        if (langSetting != null && langSetting.length >= 2) {
            lang = langSetting[0];
            country = langSetting[1];
        }
        boolean isChinese = lang.toLowerCase().startsWith("zh");

        // Reverse geocode to get location name
        final String locationName;
        {
            String tmpLocation = isChinese ? "未知地點" : "Unknown location";
            try {
                Geocoder geocoder = new Geocoder(this, isChinese ? Locale.TRADITIONAL_CHINESE : Locale.ENGLISH);
                java.util.List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    if (isChinese) {
                        if (address.getLocality() != null) {
                            tmpLocation = address.getLocality();
                        } else if (address.getSubAdminArea() != null) {
                            tmpLocation = address.getSubAdminArea();
                        } else if (address.getAdminArea() != null) {
                            tmpLocation = address.getAdminArea();
                        }
                    } else {
                        if (address.getLocality() != null) {
                            tmpLocation = address.getLocality();
                        } else if (address.getSubAdminArea() != null) {
                            tmpLocation = address.getSubAdminArea();
                        } else if (address.getAdminArea() != null) {
                            tmpLocation = address.getAdminArea();
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore geocoder errors, fallback to default locationName
            }
            locationName = tmpLocation;
        }

        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addBotMessage(isChinese ? "取得天氣資訊失敗：" + e.getMessage() : "Failed to get weather info: " + e.getMessage()));
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> addBotMessage(isChinese ? "天氣API回應失敗：" + response.code() : "Weather API failed: " + response.code()));
                    return;
                }
                String resp = response.body().string();
                try {
                    JSONObject obj = new JSONObject(resp);
                    JSONObject current = obj.getJSONObject("current_weather");
                    int weatherCode = current.has("weathercode") ? current.getInt("weathercode") : -1;
                    String weatherDesc = getWeatherDescription(weatherCode, isChinese);
                    String weatherInfo;
                    if (isChinese) {
                        weatherInfo = "地點：" + locationName + "\n" +
                                "天氣狀況：" + weatherDesc + "，" +
                                "目前氣溫：" + current.getDouble("temperature") + "°C，" +
                                "風速：" + current.getDouble("windspeed") + " km/h";
                    } else {
                        weatherInfo = "Location: " + locationName + "\n" +
                                "Weather: " + weatherDesc + ", " +
                                "Temperature: " + current.getDouble("temperature") + "°C, " +
                                "Windspeed: " + current.getDouble("windspeed") + " km/h";
                    }
                    runOnUiThread(() -> addBotMessage(weatherInfo));
                } catch (Exception e) {
                    runOnUiThread(() -> addBotMessage(isChinese ? "解析天氣資料失敗" : "Failed to parse weather data"));
                }
            }
        });
    }

    // Weather code mapping helper
    private String getWeatherDescription(int code, boolean isChinese) {
        if (isChinese) {
            switch (code) {
                case 0: return "晴朗";
                case 1: return "主要晴朗";
                case 2: return "部分多雲";
                case 3: return "多雲";
                case 45: return "有霧";
                case 48: return "有霧霜";
                case 51: return "輕微毛毛雨";
                case 53: return "中等毛毛雨";
                case 55: return "強毛毛雨";
                case 56: return "輕微凍雨";
                case 57: return "強凍雨";
                case 61: return "小雨";
                case 63: return "中雨";
                case 65: return "大雨";
                case 66: return "輕微凍雨";
                case 67: return "強凍雨";
                case 71: return "小雪";
                case 73: return "中雪";
                case 75: return "大雪";
                case 77: return "雪粒";
                case 80: return "陣雨";
                case 81: return "中陣雨";
                case 82: return "強陣雨";
                case 85: return "小陣雪";
                case 86: return "大陣雪";
                case 95: return "雷暴";
                case 96: return "雷暴伴有小冰雹";
                case 99: return "雷暴伴有大冰雹";
                default: return "未知天氣";
            }
        } else {
            switch (code) {
                case 0: return "Clear sky";
                case 1: return "Mainly clear";
                case 2: return "Partly cloudy";
                case 3: return "Overcast";
                case 45: return "Fog";
                case 48: return "Depositing rime fog";
                case 51: return "Light drizzle";
                case 53: return "Moderate drizzle";
                case 55: return "Dense drizzle";
                case 56: return "Light freezing drizzle";
                case 57: return "Dense freezing drizzle";
                case 61: return "Slight rain";
                case 63: return "Moderate rain";
                case 65: return "Heavy rain";
                case 66: return "Light freezing rain";
                case 67: return "Heavy freezing rain";
                case 71: return "Slight snow fall";
                case 73: return "Moderate snow fall";
                case 75: return "Heavy snow fall";
                case 77: return "Snow grains";
                case 80: return "Slight rain showers";
                case 81: return "Moderate rain showers";
                case 82: return "Violent rain showers";
                case 85: return "Slight snow showers";
                case 86: return "Heavy snow showers";
                case 95: return "Thunderstorm";
                case 96: return "Thunderstorm with slight hail";
                case 99: return "Thunderstorm with heavy hail";
                default: return "Unknown weather";
            }
        }
    }

    private void addBotMessage(String text) {
        messageList.add(new Message(text, false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
        // Play TTS for bot message (especially for weather reply)
        if (customTextToSpeech != null) {
            customTextToSpeech.speak(new String[]{text});
        }
    }
}