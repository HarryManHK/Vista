package com.example.vista;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;  // Import this for runtime permissions
import androidx.core.content.ContextCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.google.android.material.button.MaterialButton;
import com.example.vista.ChatBot.*;

public class MainMenuPage extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    // 權限列表（如有新增請在此補上）
    private static String[] getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.INTERNET);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(Manifest.permission.ACCESS_NETWORK_STATE);
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.NFC);
        // 只有 Android 10 (API 29) 或以下才檢查儲存權限
        if (android.os.Build.VERSION.SDK_INT < 30) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return perms.toArray(new String[0]);
    }

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
        //txtTitle = findViewById(R.id.textView2);
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
        String[] requiredPermissions = getRequiredPermissions();
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    // Handle the result from permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            List<String> denied = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    denied.add(permissions[i]);
                }
            }
            if (!denied.isEmpty()) {
                StringBuilder msg = new StringBuilder("以下權限未授權：\n");
                for (String p : denied) {
                    msg.append("- ").append(getPermissionLabel(p)).append("\n");
                }
                msg.append("\n部分功能可能無法正常運作。\n\n請在設定中手動開啟權限。");
                new AlertDialog.Builder(this)
                        .setTitle("權限提示")
                        .setMessage(msg.toString())
                        .setPositiveButton("前往設定", (d, w) -> openAppSettings())
                        .setNegativeButton("取消", null)
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 權限顯示友善名稱
    private String getPermissionLabel(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA: return "相機";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                if (android.os.Build.VERSION.SDK_INT >= 30) return "（Android 11+ 不需手動授權）";
                return permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ? "讀取儲存空間" : "寫入儲存空間";
            case Manifest.permission.INTERNET: return "網路";
            case Manifest.permission.ACCESS_FINE_LOCATION: return "精確定位";
            case Manifest.permission.ACCESS_COARSE_LOCATION: return "粗略定位";
            case Manifest.permission.ACCESS_NETWORK_STATE: return "網路狀態";
            case Manifest.permission.RECORD_AUDIO: return "麥克風";
            case Manifest.permission.NFC: return "NFC";
            default: return permission;
        }
    }

    // 跳轉到 App 設定頁面
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
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
        //txtTitle.setText(getString(R.string.MainMenuPageActivity_title));
        btnSetting.setText(getString(R.string.MainMenuPageActivity_Setting));
        btnImageToText.setText(getString(R.string.MainMenuPageActivity_ImageToText));
        btnFindBusStop.setText(getString(R.string.MainMenuPageActivity_FindBusStop));
    }
}