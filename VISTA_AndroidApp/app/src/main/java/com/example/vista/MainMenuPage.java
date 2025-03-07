package com.example.vista;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;  // Import this for runtime permissions
import androidx.core.content.ContextCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.google.android.material.button.MaterialButton;
import com.example.vista.ChatBot.*;

public class MainMenuPage extends AppCompatActivity {
    private static final String TAG = "VoiceControlPage";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private TextView txtTitle;
    private MaterialButton btnImageToText, btnFindBusStop, btnSetting, btnChatbot, btnVoiceControl;
    private CustomTextToSpeech customTextToSpeech;  // Custom TTS instance
    private SettingDatabaseHelper dbHelper;         // Database helper instance

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

        // Check and request permissions at runtime.
        checkAndRequestPermissions();

        // Initialize buttons
        txtTitle = findViewById(R.id.textView2);
        btnImageToText = findViewById(R.id.btnMainMenuImgToTxt);
        btnFindBusStop = findViewById(R.id.button5);
        btnSetting = findViewById(R.id.btnMainMenuSetting);
        btnChatbot = findViewById(R.id.btnMainMenuChatbot);
        btnVoiceControl = findViewById(R.id.btnVoiceControl);

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(MainMenuPage.this);

        // Set onClickListener for "Image To Text" button
        btnImageToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] buttonLabel = {"You will go to Image To Text page.", "你將進入圖片轉文字頁面。"};
                announceButtonLabel(buttonLabel);
                Intent intent = new Intent(MainMenuPage.this, ImageToTextMenu.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Find Bus Stop" button
        btnFindBusStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] buttonLabel = {"You will go to Find Bus Stop page.", "你將進入查找巴士站頁面。"};
                announceButtonLabel(buttonLabel);
                Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Setting" button
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] buttonLabel = {"You will go to Setting page.", "你將進入設置頁面。"};
                announceButtonLabel(buttonLabel);
                Intent intent = new Intent(MainMenuPage.this, SettingPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "ChatBot" button
        btnChatbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] buttonLabel = {"You will go to ChatBot page.", "你將進入ChatBot頁面。"};
                announceButtonLabel(buttonLabel);
                Intent intent = new Intent(MainMenuPage.this, ChatBotPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Voice Control" button
        btnVoiceControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] buttonLabel = {"You will go to Voice control page.", "你將進入語音控制頁面。"};
                announceButtonLabel(buttonLabel);
                Intent intent = new Intent(MainMenuPage.this, VoiceControlPage.class);
                startActivity(intent);
            }
        });

    }

    // Method to check and request necessary permissions.
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.RECORD_AUDIO
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // Handle the result from permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean grantedAll = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    grantedAll = false;
                    break;
                }
            }
            if (!grantedAll) {
                Toast.makeText(this, "Not all permissions were granted. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Function to announce the button label using TextToSpeech
    private void announceButtonLabel(String[] label) {
        customTextToSpeech.speak(label);
    }

    @Override
    protected void onDestroy() {
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

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        // Android 10 以上的版本，不需要寫入 app 專屬目錄的外部存儲權限
        int storagePermission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            storagePermission = PackageManager.PERMISSION_GRANTED;
        } else {
            storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        Log.d(TAG, "Permissions check: RECORD_AUDIO=" + recordPermission +
                ", INTERNET=" + internetPermission + ", WRITE_EXTERNAL_STORAGE=" + storagePermission);

        return recordPermission == PackageManager.PERMISSION_GRANTED &&
                internetPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED;
    }

}