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

import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class SettingPage extends AppCompatActivity {

    private TextView textViewSetting;
    private Button btnLanSetting, btnVoiceSetting;
    private CustomTextToSpeech customTextToSpeech;

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
    protected void onRestart() {
        super.onRestart();
        recreate();
        textViewSetting.setText(getString(R.string.SettingActivity_Setting));
        btnLanSetting.setText(getString(R.string.SettingActivity_LanguageSetting));
        btnVoiceSetting.setText(getString(R.string.SettingActivity_VoiceSetting));
    }
}
