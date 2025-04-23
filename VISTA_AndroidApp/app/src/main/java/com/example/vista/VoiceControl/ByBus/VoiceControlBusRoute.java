package com.example.vista.VoiceControl.ByBus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vista.R;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

import org.json.JSONObject;

public class VoiceControlBusRoute extends AppCompatActivity {
    private EditText etBusRoute;
    private CustomTextToSpeech tts;
    private BusDatabaseHelper dbHelper;
    private static final String TAG = "EditRouteActivity_debug";
    private static final int REQ_CODE_SPEECH_INPUT = 1001;
    // 防止語音回來後重複觸發，僅允許第一次自動語音
    private boolean hasPromptedSpeech = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control_bus_route);

        etBusRoute = findViewById(R.id.etBusRoute);
        tts = new CustomTextToSpeech(this);
        // 初始化 BusDatabaseHelper
        dbHelper = BusDatabaseHelper.getInstance(this);

        // 僅保留手動輸入與確認，移除 Google 語音相關功能
        etBusRoute.setEnabled(true);
        etBusRoute.setFocusable(true);
        etBusRoute.setFocusableInTouchMode(true);
        etBusRoute.setVisibility(android.view.View.VISIBLE);
        Button btnConfirm = null;
        try { btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm); } catch (Exception ignored) {}
        if (btnConfirm != null) btnConfirm.setVisibility(android.view.View.VISIBLE);
        // 隱藏 Next 語音按鈕
        Button btnNext = findViewById(R.id.btnEditRouteActivityNext);
        btnNext.setVisibility(android.view.View.GONE);

        // 監聽 EditText 鍵盤開啟事件，僅第一次自動啟動 Google Voice Service
        etBusRoute.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !hasPromptedSpeech) {
                promptSpeechInput();
                hasPromptedSpeech = true;
            }
        });

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String userInput = etBusRoute.getText().toString().trim().toUpperCase();
                if (!userInput.isEmpty()) {
                    long insertResult = dbHelper.insertBusRoute(
                        userInput,    // route_number
                        "---",         // to_station
                        "---",         // to_station_ZH
                        "---",         // bound
                        "---",         // start_point
                        "---",         // start_point_ZH
                        "---",         // start_point_seq
                        "---",         // start_point_stop_id
                        "---",         // start_point_lat
                        "---",         // start_point_long
                        "---",         // destination
                        "---",         // destination_ZH
                        "---",         // destination_stop_id
                        "---",         // destination_seq
                        "---",         // destination_lat
                        "---"          // destination_long
                    );
                    Log.d(TAG, "insertBusRoute result: " + insertResult);
                    if (tts != null) {
                        tts.speak(new String[]{
                            "You have chosen route " + userInput + ". Please select the outbound direction.",
                            "你選擇了" + userInput + "路線，為你顯示出站選擇"
                        });
                    }
                    Intent intent = new Intent(VoiceControlBusRoute.this, VoiceControlEditDestination.class);
                    intent.putExtra("BusRoute", userInput);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "請輸入巴士路線號碼", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 頁面一打開自動啟動 Google Voice Service
        promptSpeechInput();
    }

    /**
     * 啟動 Google Voice Service 進行語音輸入
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "請說出巴士路線號碼");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (android.content.ActivityNotFoundException a) {
            Toast.makeText(this, "語音辨識服務不可用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            try {
                java.util.ArrayList<String> result = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty() && etBusRoute != null) {
                    String recognized = result.get(0);
                    etBusRoute.setText(recognized);
                    if (tts != null) {
                        tts.speak(new String[]{
                            "Input received: " + recognized + ". Please confirm and press the confirm button.",
                            "已輸入" + recognized + "，請確認後按下確認按鈕"
                        });
                    }
                } else {
                    Toast.makeText(this, "語音辨識失敗，請再試一次", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Voice input error: ", e);
                Toast.makeText(this, "語音輸入發生錯誤", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
