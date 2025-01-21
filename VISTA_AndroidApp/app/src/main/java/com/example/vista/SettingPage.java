package com.example.vista;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private Button btnConfirm, btnNext;

    // Array of setting buttons for cycling
    private Button[] settingButtons;
    private int currentSelection = 0; // Start with the first setting
    private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class

    String[] buttonLabel = new String[2]; // button label for text to speech

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
        btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm);
        btnNext = findViewById(R.id.btnEditRouteActivityNext);

        // Initialize setting buttons array
        settingButtons = new Button[]{btnLanSetting, btnVoiceSetting};

        // Highlight the initial selection
        highlightSelection();

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

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cycleToNextSetting();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmSelection();
            }
        });
    }

    // Navigate to LanguageSettingPage
    private void navigateToLanguageSetting() {
        // Announce button label
        String[] buttonLabel = {"You will go to Language Setting page.", "你將進入語言設置頁面。"};
        announceButtonLabel(buttonLabel);

        Intent intent = new Intent(SettingPage.this, LanguageSettingPage.class);
        startActivity(intent);
    }

    // Navigate to VoiceSettingPage
    private void navigateToVoiceSetting() {
        // Announce button label
        String[] buttonLabel = {"You will go to Voice Setting page.", "你將進入語音設置頁面。"};
        announceButtonLabel(buttonLabel);

        Intent intent = new Intent(SettingPage.this, VoiceSettingPage.class);
        startActivity(intent);
    }

    // Cycle to the next setting option
    private void cycleToNextSetting() {
        // Remove highlight from current selection
        settingButtons[currentSelection].setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // Move to next selection
        currentSelection = (currentSelection + 1) % settingButtons.length;

        String[] buttonLabel = {settingButtons[currentSelection].getText().toString(), settingButtons[currentSelection].getText().toString()};
        announceButtonLabel(buttonLabel);
        // Highlight the new selection
        highlightSelection();
    }

    // Confirm the current selection and navigate accordingly
    private void confirmSelection() {
        switch (currentSelection) {
            case 0:
                navigateToLanguageSetting();
                break;
            case 1:
                navigateToVoiceSetting();
                break;
            default:
                // Handle unexpected cases
                break;
        }
    }

    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
    }

    // Highlight the currently selected setting
    private void highlightSelection() {
        for (int i = 0; i < settingButtons.length; i++) {
            if (i == currentSelection) {
                // Highlight selected button (e.g., change background color)
                settingButtons[i].setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                // Reset background color for other buttons
                settingButtons[i].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();

        textViewSetting.setText(getString(R.string.SettingActivity_Setting));
        btnLanSetting.setText(getString(R.string.SettingActivity_LanguageSetting));
        btnVoiceSetting.setText(getString(R.string.SettingActivity_VoiceSetting));
        btnConfirm.setText(getString(R.string.btn_Confirm));
        btnNext.setText(getString(R.string.btn_next));
    }
}