package com.example.vista;

import android.content.Context;
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

import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.SettingPage;

public class LanguageSettingPage extends AppCompatActivity {

    private ListView lvLanguageSetting;
    private Button btnConfirm, btnNext;
    private int selectedPosition = -1; // No selection initially

    private String[] languages;
    private String[] languageCodes; // To map selections to locale codes

    private SettingDatabaseHelper dbHelper; // Database helper instance

    private CustomTextToSpeech customTextToSpeech;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_language_setting_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        customTextToSpeech = new CustomTextToSpeech(this);

        // Initialize UI elements
        lvLanguageSetting = findViewById(R.id.lvLanguageSetting);
        btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm);
        btnNext = findViewById(R.id.btnEditRouteActivityNext);

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(LanguageSettingPage.this);

        // Define language options and corresponding locale codes
        languages = new String[]{
                getString(R.string.language_english),
                getString(R.string.language_traditional_chinese_hk)
        };

        languageCodes = new String[]{
                "en",    // English
                "zh"     // Traditional Chinese (Hong Kong)
        };

        // Define TTS labels for interface selection
        final String[] EnglishInterfaceLabels = {"English interface", "Traditional Chinese interface"};
        final String[] ChineseInterfaceLabels = {"英文介面", "繁體中文介面"};

        // Set up the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_white_text,
                languages
        );

        lvLanguageSetting.setAdapter(adapter);
        lvLanguageSetting.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Restore previous selection if any
        restoreSelection();

        // Handle item selection
        lvLanguageSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedPosition = position;
                // TTS 報讀選擇
                customTextToSpeech.speak(new String[]{
                    "Language set to " + EnglishInterfaceLabels[selectedPosition] + ".",
                    "語言已設定為 " + ChineseInterfaceLabels[selectedPosition]
                });
            }
        });

        // Handle Next button click
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateNext();
            }
        });

        // Handle Confirm button click
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmSelection();
            }
        });
    }

    /**
     * Restore the previously selected language from the database
     */
    private void restoreSelection() {
        String[] setting = dbHelper.getLanguageSetting();
        if (setting != null) {
            String languageCode = setting[0];
            String countryCode = setting[1];

            // Determine the index based on language code
            int index = 0; // Default to English
            if (languageCode.equals("zh") && countryCode.equals("HK")) {
                index = 1;
            }

            selectedPosition = index;
            lvLanguageSetting.setItemChecked(selectedPosition, true);
        } else {
            // No setting found, default to English
            selectedPosition = 0;
            lvLanguageSetting.setItemChecked(selectedPosition, true);
        }
    }

    /**
     * Save the selected language to the database
     *
     * @param languageCode Language code (e.g., "en", "zh")
     * @param countryCode  Country code (e.g., "US", "HK")
     * @return boolean indicating success or failure
     */
    private boolean saveLanguageSetting(String languageCode, String countryCode) {
        return dbHelper.insertOrUpdateLanguageSetting(languageCode, countryCode);
    }

    /**
     * Cycle to the next language option in the list
     */
    private void navigateNext() {
        if (languages.length == 0) return;

        if (selectedPosition == -1) {
            // If no selection, select the first item
            selectedPosition = 0;
        } else {
            // Move to next item
            selectedPosition = (selectedPosition + 1) % languages.length;
        }

        lvLanguageSetting.setItemChecked(selectedPosition, true);
        lvLanguageSetting.setSelection(selectedPosition);
    }

    /**
     * Confirm the selected language and apply the setting
     */
    private void confirmSelection() {
        if (selectedPosition == -1) {
            Toast.makeText(this, "Please select a language.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the button to prevent multiple clicks
        btnConfirm.setEnabled(false);

        String selectedLanguageCode = languageCodes[selectedPosition];
        String language;
        String country;

        if (selectedLanguageCode.equals("en")) {
            language = "en";
            country = "US"; // You can set your desired default country code
        } else if (selectedLanguageCode.equals("zh")) {
            language = "zh";
            country = "HK";
        } else {
            language = "en";
            country = "US";
        }

        // Save the language setting
        boolean isSaved = saveLanguageSetting(language, country);
        if (isSaved) {
            // Apply the locale immediately
            LocaleHelper.setLocale(this, language, country);

            // Inform the user that the language has been changed
            Toast.makeText(this, "Language changed to " + languages[selectedPosition], Toast.LENGTH_SHORT).show();

            // TTS 播報確認並跳轉至設置頁面
            final String[] EnglishInterfaceLabels = {"English interface", "Traditional Chinese interface"};
            final String[] ChineseInterfaceLabels = {"英文介面", "繁體中文介面"};
            customTextToSpeech.speak(new String[]{
                "Language set to " + EnglishInterfaceLabels[selectedPosition] + ".",
                "語言已設定為 " + ChineseInterfaceLabels[selectedPosition]
            });
            startActivity(new Intent(LanguageSettingPage.this, SettingPage.class));
            finish();
        } else {
            Toast.makeText(this, "Failed to save language setting.", Toast.LENGTH_SHORT).show();
        }

        // Re-enable the button after the action is complete
        btnConfirm.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close(); // Close the database connection
        super.onDestroy();
    }
}