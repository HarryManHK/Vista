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

public class VoiceSettingPage extends AppCompatActivity {

    private TextView textViewSetting;
    private Button btnLanSetting, btnVoiceSpeedSetting, btnGender;

    private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class

    // Retrieve language setting (from database)
    private String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
    private String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_setting_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        customTextToSpeech = new CustomTextToSpeech(this);

        // Initialize UI elements
        textViewSetting = findViewById(R.id.textView2);
        btnLanSetting = findViewById(R.id.btnLanSetting);
        btnVoiceSpeedSetting = findViewById(R.id.btnVoiceSpeedSetting);
        btnGender = findViewById(R.id.btnGender);

        // Set up button click listeners
        btnLanSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToLanguageSetting();
            }
        });

        btnVoiceSpeedSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToSpeedSetting();
            }
        });

        btnGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToGenderSetting();
            }
        });
    }

    // Navigate to LanguageSettingPage
    private void navigateToLanguageSetting() {

        // Announce button label
        String[] buttonLabel = {"You will go to Voice Language Setting page.", "你將進入語音語言設定頁面。"};
        announceButtonLabel(buttonLabel);


        Intent intent = new Intent(VoiceSettingPage.this, VoiceLanguageSetting.class);
        startActivity(intent);
    }

    // Navigate to VoiceSpeedSettingPage
    private void navigateToSpeedSetting() {

        // Announce button label
        String[] buttonLabel = {"You will go to Voice Speed Setting page.", "你將進入語音速度設定頁面。"};
        announceButtonLabel(buttonLabel);

        Intent intent = new Intent(VoiceSettingPage.this, VoiceSpeedSetting.class);
        startActivity(intent);
    }

    private void navigateToGenderSetting() {

        // Announce button label
        String[] buttonLabel = {"You will go to Voice Gender Setting page.", "你將進入配音性別設定頁面。"};
        announceButtonLabel(buttonLabel);

        Intent intent = new Intent(VoiceSettingPage.this, VoiceGenderSetting.class);
        startActivity(intent);
    }

    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (languageCode.equals("en")){
            textViewSetting.setText("Voice Setting");
            btnLanSetting.setText("Voice Language Setting");
            btnVoiceSpeedSetting.setText("Voice Speed Setting");
            btnGender.setText("Voice Gender Setting");
        }else if(languageCode.equals("zh")){
            textViewSetting.setText("語音設定");
            btnLanSetting.setText("語音語言設定");
            btnVoiceSpeedSetting.setText("語音速度設定");
            btnGender.setText("配音性別設定");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }
}