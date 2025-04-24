package com.example.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.R;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.example.vista.SettingPage;

public class VoiceLanguageSetting extends AppCompatActivity {

    private ListView lvVoiceLanguageSetting;
    private Button btnConfirm, btnNext;
    private CustomTextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_language_setting);

        // Handle Edge-to-Edge insets (for devices with notches/rounded corners)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ListView
        lvVoiceLanguageSetting = findViewById(R.id.lvVoiceLanguageSetting);

        // Initialize TTS
        tts = new CustomTextToSpeech(this);
        btnConfirm = findViewById(R.id.btnLanguageSpeedConfirm);
        btnNext = findViewById(R.id.btnLanguageSpeedNext);

        // A sample list of language codes you want to display
        final String[] languages = {"en", "zh"};

        // Human-friendly labels for TTS (English first, then Chinese)
        final String[] EnglishlanguageLabels = {"English", "Traditional Chinese"};
        final String[] ChineseLanguageLabels = {"英文", "中文(香港)"};
        
        // Populate the ListView using an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_white_text,
                languages
        );
        lvVoiceLanguageSetting.setAdapter(adapter);

        // When a user taps an item in the ListView, update the voice language in the database
        lvVoiceLanguageSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = (String) parent.getItemAtPosition(position);
                setVoiceLanguage(selectedLanguage);
                // TTS 報讀選擇（依陣列順序：英文、中文）
                tts.speak(new String[]{
                    "You've chosen voice language " + EnglishlanguageLabels[position],
                    "您已選擇語音語言：" +ChineseLanguageLabels[position]
                });
            }
        });

        // Confirm button click listener
        btnConfirm.setOnClickListener(v -> {
            // 確保有選擇
            int pos = lvVoiceLanguageSetting.getCheckedItemPosition();
            if (pos != AdapterView.INVALID_POSITION) {
                String sel = (String) lvVoiceLanguageSetting.getItemAtPosition(pos);
                // TTS 報讀確認（依陣列順序：英文、中文）
                tts.speak(new String[]{
                    "Language voice set to " + EnglishlanguageLabels[pos] + ".",
                    "語音語言已設定為：" + ChineseLanguageLabels[pos]
                });
                // 導航回 SettingPage
                startActivity(new Intent(VoiceLanguageSetting.this, SettingPage.class));
                finish();
            } else {
                Toast.makeText(this, "Please select a language.", Toast.LENGTH_SHORT).show();
            }
        });

        // Next button: move list to next item
        btnNext.setOnClickListener(v -> {
            int pos = lvVoiceLanguageSetting.getCheckedItemPosition();
            if (pos == AdapterView.INVALID_POSITION) {
                pos = 0;
            } else {
                pos = (pos + 1) % languages.length;
            }
            lvVoiceLanguageSetting.setItemChecked(pos, true);
            lvVoiceLanguageSetting.smoothScrollToPosition(pos);
        });
    }

    /**
     * Updates the selected voice language in the database.
     *
     * @param language The language code that the user selected (e.g., "en", "zh").
     */
    private void setVoiceLanguage(String language) {
        // Get an instance of your SettingDatabaseHelper
        SettingDatabaseHelper dbHelper = SettingDatabaseHelper.getInstance(this);

        // Call the setVoiceLanguage(...) method to update the language in the DB
        boolean success = dbHelper.setVoiceLanguage(language);

        // Provide user feedback
        if (success) {
            Toast.makeText(this, "Voice language updated to: " + language, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update voice language.", Toast.LENGTH_SHORT).show();
        }
    }
}