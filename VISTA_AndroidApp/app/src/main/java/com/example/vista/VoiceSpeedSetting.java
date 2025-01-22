package com.example.vista;

import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;

public class VoiceSpeedSetting extends AppCompatActivity {

    private static final String TAG = "VoiceSpeedSetting";

    private ListView lvLanguageSpeedSetting;
    private Button btnConfirm;
    private Button btnNext;

    // Example speeds to choose from
    private final float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};

    // Keep track of the user's selection
    private float selectedSpeed = 1.0f; // default

    private SettingDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_speed_setting);

        Log.d(TAG, "onCreate: Starting VoiceSpeedSetting activity");

        // Acquire database helper
        dbHelper = SettingDatabaseHelper.getInstance(getApplicationContext());

        // Adjust for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            Log.d(TAG, "onCreate: Applied window insets padding");
            return insets;
        });

        // Initialize UI components
        try {
            lvLanguageSpeedSetting = findViewById(R.id.lvLanguageSpeedSetting);
            btnConfirm = findViewById(R.id.btnLanguageSpeedConfirm);
            btnNext = findViewById(R.id.btnLanguageSpeedNext);
            Log.d(TAG, "onCreate: UI components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error initializing UI components", e);
            Toast.makeText(this, "Error initializing UI components.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create labels (e.g., "0.5x", "0.75x", etc.) to display in the ListView
        String[] speedLabels = new String[speeds.length];
        for (int i = 0; i < speeds.length; i++) {
            speedLabels[i] = speeds[i] + "x";
        }

        // Set up ListView with a simple ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                speedLabels
        );

        try {
            lvLanguageSpeedSetting.setAdapter(adapter);
            lvLanguageSpeedSetting.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            Log.d(TAG, "onCreate: ListView adapter set successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error setting ListView adapter", e);
            Toast.makeText(this, "Error setting ListView adapter.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Handle user selection from the ListView
        lvLanguageSpeedSetting.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            selectedSpeed = speeds[position];
            Log.d(TAG, "onItemClick: Selected speed = " + selectedSpeed + "x");
        });

        // Confirm button: saves the selected speed to the database
        btnConfirm.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Confirm button clicked");
            boolean success = dbHelper.setSpeed(selectedSpeed);
            Log.d(TAG, "onClick: Attempting to update voice speed to " + selectedSpeed + "x");

            if (success) {
                Toast.makeText(VoiceSpeedSetting.this,
                        "Voice speed updated to " + selectedSpeed + "x",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onClick: Voice speed successfully updated");
            } else {
                Toast.makeText(VoiceSpeedSetting.this,
                        "Failed to update voice speed.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onClick: Failed to update voice speed");
            }
        });

        // Next button: optional navigation (commented out by default)
        /*
        btnNext.setOnClickListener(v -> {
            // e.g., navigate to another activity
        });
        */
    }
}