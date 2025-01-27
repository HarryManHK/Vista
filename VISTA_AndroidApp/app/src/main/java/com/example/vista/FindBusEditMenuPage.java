package com.example.vista;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vista.FindBusEditMenuFunction.*;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class FindBusEditMenuPage extends AppCompatActivity {

    private static final String TAG = "FindBusEditMenuPage_debug";

    // UI Components
    private TextView txtBusRoute;
    private Button btnEditRoute, btnEditOutboundDirection, btnEditStartPoint, btnEditDestination;

    // Language labels
    private String[] zhButtonLabel = {"修改路綫","修改開住的方向","修改起點","修改終點"};
    private String[] enButtonLabel = {"Edit Route", "Edit Outbound Direction", "Edit Start Point", "Edit Destination"};

    private CustomTextToSpeech customTextToSpeech;
    private BusDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_bus_edit_menu_page);

        // Initialize DatabaseHelper
        dbHelper = BusDatabaseHelper.getInstance(this);

        // Initialize UI Components
        txtBusRoute = findViewById(R.id.txtFindBusEditBusRoute);
        btnEditRoute = findViewById(R.id.btnFindBusEditEditRoute);
        btnEditOutboundDirection = findViewById(R.id.btnFindBusEditOutboundDirection);
        btnEditStartPoint = findViewById(R.id.btnFindBusEditStartPoint);
        btnEditDestination = findViewById(R.id.btnFindBusEditDestination);

        // Initialize TextToSpeech
        customTextToSpeech = new CustomTextToSpeech(this);

        // Display current BusRoute data
        displayBusRouteData();

        // Set direct click listeners for edit buttons
        setEditButtonListeners();
    }

    private void setEditButtonListeners() {
        btnEditRoute.setOnClickListener(v -> handleEditButtonClick("EditRoute", 0));
        btnEditOutboundDirection.setOnClickListener(v -> handleEditButtonClick("EditOutboundDirection", 1));
        btnEditStartPoint.setOnClickListener(v -> handleEditButtonClick("EditStartPoint", 2));
        btnEditDestination.setOnClickListener(v -> handleEditButtonClick("EditDestination", 3));
    }

    private void handleEditButtonClick(String fieldToEdit, int buttonIndex) {
        // Announce selection
        announceButtonLabel(new String[]{
                "Navigating to " + enButtonLabel[buttonIndex] + " page.",
                "你選擇了" + zhButtonLabel[buttonIndex] + "功能"
        });

        // Start corresponding activity
        Intent intent;
        switch (fieldToEdit) {
            case "EditRoute":
                intent = new Intent(this, EditRouteActivity.class);
                break;
            case "EditOutboundDirection":
                intent = new Intent(this, EditOutboundDirectionActivity.class);
                break;
            case "EditStartPoint":
                intent = new Intent(this, EditStartPointActivity.class);
                break;
            case "EditDestination":
                intent = new Intent(this, EditDestinationActivity.class);
                break;
            default:
                Log.e(TAG, "No corresponding activity found");
                return;
        }
        startActivity(intent);
    }

    private void displayBusRouteData() {
        try (Cursor cursor = dbHelper.getLatestBusRoute()) {
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String toStation = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO));
                String toStation_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO_ZH));
                String startPoint = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT));
                String startPoint_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_ZH));
                String destination = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION));
                String destination_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_ZH));

                // Get language setting
                String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
                String languageCode = (languageSetting != null) ? languageSetting[0] : "en";

                // Build route info
                String routeInfo;
                if ("zh".equals(languageCode)) {
                    routeInfo = String.format("路線: %s\n開往: %s\n起點: %s\n終點: %s",
                            routeNumber, toStation_ZH, startPoint_ZH, destination_ZH);
                } else {
                    routeInfo = String.format("Route: %s\nTo: %s\nStart: %s\nDestination: %s",
                            routeNumber, toStation, startPoint, destination);
                }

                txtBusRoute.setText(routeInfo);
            } else {
                txtBusRoute.setText("No bus route data available.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading bus route data", e);
            txtBusRoute.setText("Error loading data.");
        }
    }

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
        displayBusRouteData(); // Refresh data when returning
    }
}