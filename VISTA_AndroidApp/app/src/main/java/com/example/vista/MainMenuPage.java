package com.example.vista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.google.android.material.button.MaterialButton;

import com.example.vista.ChatBot.*;
public class MainMenuPage extends AppCompatActivity {

    private TextView txtTitle;
    private MaterialButton btnImageToText, btnFindBusStop, btnSetting,btnChatbot;
    private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class
    private SettingDatabaseHelper dbHelper; // Database helper instance

    // Initialize language Setting
    @Override
    protected void attachBaseContext(Context newBase) {
        // Initialize Database Helper
        dbHelper = SettingDatabaseHelper.getInstance(newBase);

        // Retrieve language settings from the database
        String[] languageSetting = dbHelper.getLanguageSetting();
        String languageCode = "en"; // Default to English
        String countryCode = "US";   // Default to US

        if (languageSetting != null) {
            languageCode = languageSetting[0];
            countryCode = languageSetting[1];
        }

        // Apply the locale before super.onCreate()
        Context context = LocaleHelper.setLocale(newBase, languageCode, countryCode);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu_page);

        // Initialize buttons
        txtTitle = findViewById(R.id.textView2);
        btnImageToText = findViewById(R.id.btnMainMenuImgToTxt);
        btnFindBusStop = findViewById(R.id.button5);
        btnSetting = findViewById(R.id.btnMainMenuSetting);
        btnChatbot = findViewById(R.id.btnMainMenuChatbot);

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(MainMenuPage.this);

        // Set onClickListener for "Image To Text" button
        btnImageToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Image To Text page.", "你將進入圖片轉文字頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the ImageToTextMenu activity
                Intent intent = new Intent(MainMenuPage.this, ImageToTextMenu.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Find Bus Stop" button
        btnFindBusStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Find Bus Stop page.", "你將進入查找巴士站頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the FindBusStopMenuPage activity
                Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Setting" button
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Setting page.", "你將進入設置頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the SettingPage activity
                Intent intent = new Intent(MainMenuPage.this, SettingPage.class);
                startActivity(intent);
            }
        });

        //Chatbot page
        btnChatbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to ChatBot page.", "你將進入ChatBot頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the SettingPage activity
                Intent intent = new Intent(MainMenuPage.this, ChatBotPage.class);
                startActivity(intent);
            }
        });
    }

    // Function to announce the button label using TextToSpeech
    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
    }

    @Override
    protected void onDestroy() {
        // Release the TextToSpeech resources when the activity is destroyed
        if (customTextToSpeech != null) {
            customTextToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();

        txtTitle.setText(getString(R.string.MainMenuPageActivity_title));
        btnSetting.setText(getString(R.string.MainMenuPageActivity_Setting));
        btnImageToText.setText(getString(R.string.MainMenuPageActivity_ImageToText));
        btnFindBusStop.setText(getString(R.string.MainMenuPageActivity_FindBusStop));
    }
}