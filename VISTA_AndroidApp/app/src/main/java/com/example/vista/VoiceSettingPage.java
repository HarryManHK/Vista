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

public class VoiceSettingPage extends AppCompatActivity {

    private TextView textViewSetting;
    private Button btnLanSetting, btnVoiceSpeedSetting,btnGender;
    private Button btnConfirm, btnNext;

    // Array of setting buttons for cycling
    private Button[] settingButtons;
    private int currentSelection = 0; // Start with the first setting

    private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class


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
        btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm);
        btnNext = findViewById(R.id.btnEditRouteActivityNext);

        // Initialize setting buttons array
        settingButtons = new Button[]{btnLanSetting, btnVoiceSpeedSetting,btnGender};

        // Highlight the initial selection
        highlightSelection();

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
        String[] buttonLabel = {"You will go to Voice Language Setting page.", "你將進入語音語言設定頁面。"};
        announceButtonLabel(buttonLabel);


        Intent intent = new Intent(VoiceSettingPage.this, VoiceLanguageSetting.class);
        startActivity(intent);
    }

    // Navigate to VoiceSettingPage
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

    // Cycle to the next setting option
    private void cycleToNextSetting() {
        // Remove highlight from current selection
        settingButtons[currentSelection].setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // Move to next selection
        currentSelection = (currentSelection + 1) % settingButtons.length;

        // Highlight the new selection
        highlightSelection();
    }

    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
    }

    // Confirm the current selection and navigate accordingly
    private void confirmSelection() {
        switch (currentSelection) {
            case 0:
                navigateToLanguageSetting();
                break;
            case 1:
                navigateToSpeedSetting();
                break;
            case 2:
                navigateToGenderSetting();
                break;
            default:
                // Handle unexpected cases
                break;
        }
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
    }
}


