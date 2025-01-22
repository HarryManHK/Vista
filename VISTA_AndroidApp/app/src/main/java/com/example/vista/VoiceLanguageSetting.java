package com.example.vista;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.R;

public class VoiceLanguageSetting extends AppCompatActivity {

    private ListView lvVoiceLanguageSetting;

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

        // A sample list of language codes you want to display
        final String[] languages = {"en", "zh"};

        // Populate the ListView using an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                languages
        );
        lvVoiceLanguageSetting.setAdapter(adapter);

        // When a user taps an item in the ListView, update the voice language in the database
        lvVoiceLanguageSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = adapter.getItem(position);
                // Call your custom function to update the DB
                setVoiceLanguage(selectedLanguage);
            }
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