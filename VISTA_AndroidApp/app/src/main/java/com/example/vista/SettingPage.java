package com.example.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class SettingPage extends AppCompatActivity {

    private TextView textViewSetting;
    private Button btnLanSetting, btnVoiceSetting;
    private CustomTextToSpeech customTextToSpeech;

    // Retrieve language setting (from database)
    private String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
    private String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"

    String[] buttonLabel = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        textViewSetting = findViewById(R.id.textView2);
        btnLanSetting = findViewById(R.id.btnLanSetting);
        btnVoiceSetting = findViewById(R.id.btnVoiceSetting);

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(SettingPage.this);

        // Set up button click listeners
        btnLanSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { navigateToLanguageSetting(); }
        });

        btnVoiceSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToVoiceSetting();
            }
        });
    }

    private void navigateToLanguageSetting() {
        String[] buttonLabel = {"You will go to Language Setting page.", "你將進入語言設置頁面。"};
        announceButtonLabel(buttonLabel);
        startActivity(new Intent(SettingPage.this, LanguageSettingPage.class));
    }

    private void navigateToVoiceSetting() {
        String[] buttonLabel = {"You will go to Voice Setting page.", "你將進入語音設置頁面。"};
        announceButtonLabel(buttonLabel);
        startActivity(new Intent(SettingPage.this, VoiceSettingPage.class));
    }

    private void announceButtonLabel(String[] label) {
        customTextToSpeech.speak(label);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (languageCode.equals("en")){
            textViewSetting.setText("Setting");
            btnLanSetting.setText("Language Setting");
            btnVoiceSetting.setText("Voice Setting");
        }else if(languageCode.equals("zh")){
            textViewSetting.setText("設定");
            btnLanSetting.setText("語言設定");
            btnVoiceSetting.setText("語音設定");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();

        if (languageCode.equals("en")){
            textViewSetting.setText("Setting");
            btnLanSetting.setText("Language Setting");
            btnVoiceSetting.setText("Voice Setting");
        }else if(languageCode.equals("zh")){
            textViewSetting.setText("設定");
            btnLanSetting.setText("語言設定");
            btnVoiceSetting.setText("語音設定");
        }
    }
}
