package com.example.vista.FindBusEditMenuFunction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.R;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * EditRouteActivity: Allows users to input a bus route and save it to the database.
 * Upon confirmation, the bus route is saved with other columns set to "---".
 * The "Next" button navigates to the EditOutboundDirectionActivity.
 */
public class EditRouteActivity extends AppCompatActivity {
    private EditText etBusRoute;
    private BusDatabaseHelper dbHelper;
    private String TAG = "EditRouteActivity";
    private CustomTextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_route);

        // Handle Edge-to-Edge UI if needed
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 TTS
        tts = new CustomTextToSpeech(this);

        // Initialize EditText and DatabaseHelper
        etBusRoute = findViewById(R.id.etBusRoute);
        dbHelper = BusDatabaseHelper.getInstance(this);

        // Set up the "Confirm" button
        Button btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm);
        btnConfirm.setOnClickListener(v -> {
            String routeNumber = etBusRoute.getText().toString().trim().toUpperCase();
            if (routeNumber.isEmpty()) {
                Toast.makeText(EditRouteActivity.this, "Please enter a bus route", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insert or update bus route into the database
            long result = dbHelper.insertBusRoute(
                    routeNumber,    // route_number
                    "---",          // to_station
                    "---",          // to_station_ZH
                    "---",          // bound
                    "---",          // start_point
                    "---",          // start_point_ZH
                    "---",          // start_point_seq
                    "---",          // start_point_stop_id
                    "---",          // start_point_lat
                    "---",          // start_point_long
                    "---",          // destination
                    "---",          // destination_ZH
                    "---",          // destination_stop_id
                    "---",          // destination_seq
                    "---",          // destination_lat
                    "---"           // destination_long
            );

            if (result != -1) {
                Toast.makeText(EditRouteActivity.this, "Bus route saved successfully", Toast.LENGTH_SHORT).show();
                // voice assistance
                tts.speak(new String[]{
                    "You've chosen Route " + routeNumber + ".",
                    "您已選擇" + routeNumber + "號線。"
                });
                Log.d(TAG, "Bus route saved with ID: " + result);
                // Optionally, clear the input field after successful save
                etBusRoute.setText("");
                // Navigate to the outbound direction selection
                Intent intent = new Intent(EditRouteActivity.this, EditOutboundDirectionActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditRouteActivity.this, "Error saving bus route", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error saving bus route");
            }
        });

        // Set up the "Next" button
        Button btnNext = findViewById(R.id.btnEditRouteActivityNext);
        btnNext.setOnClickListener(v -> {
//            // Navigate to EditOutboundDirectionActivity
//            Intent intent = new Intent(EditRouteActivity.this, EditOutboundDirectionActivity.class);
//            startActivity(intent);
//            finish(); // Optional: finish current activity to prevent back navigation

            // Request focus for the EditText
            etBusRoute.requestFocus();

            // Show the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etBusRoute, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}