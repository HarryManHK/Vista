package com.example.vista;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class FindBusStopMenuPage extends AppCompatActivity {

    private static final String TAG = "FindBusStopMenuPage_debug";

    // Main action buttons
    private Button btnEditDestination;
    private Button btnShowArriveTime;
    private Button btnStartDetectBusStop;
    private Button btnBusArrivalAlert;

    // Language labels for announcements
    private String[] zhButtonLabel = {"修改目的地", "顯示到站時間", "開始偵測巴士站", "巴士到站提醒"};
    private String[] enButtonLabel = {"Edit Destination", "Show Arrive Time", "Start Detect Bus Stop", "Bus Arrival Alert"};

    private CustomTextToSpeech customTextToSpeech;
    private TextView txtBusRoute;
    private BusDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_bus_stop_menu_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "onCreate: Activity initialized");

        // Initialize TextToSpeech
        customTextToSpeech = new CustomTextToSpeech(this);

        // Initialize buttons
        btnEditDestination = findViewById(R.id.btnFindBusStopMenuPageEditDestination);
        btnShowArriveTime = findViewById(R.id.btnFindBusStopMenuPageShowArriveTIme);
        btnStartDetectBusStop = findViewById(R.id.btnFindBusStopMenuPageStartDetectBusStop);
        btnBusArrivalAlert = findViewById(R.id.btnFindBusStopMenuPageBusArrivalAlert);
        txtBusRoute = findViewById(R.id.txtFindBusStopMenuPageBusRoute);

        // Set button listeners
        setMainActionButtonsListeners();

        // Initialize database and load bus route
        dbHelper = BusDatabaseHelper.getInstance(this);
        loadAndDisplayLatestBusRoute();
    }

    /**
     * Set click listeners for all main action buttons.
     */
    private void setMainActionButtonsListeners() {
        btnEditDestination.setOnClickListener(v -> handleButtonClick(0));
        btnShowArriveTime.setOnClickListener(v -> handleButtonClick(1));
        btnStartDetectBusStop.setOnClickListener(v -> handleButtonClick(2));
        btnBusArrivalAlert.setOnClickListener(v -> handleButtonClick(3));
    }

    /**
     * Handle button clicks and trigger corresponding actions.
     *
     * @param buttonIndex Index of the button in the enButtonLabel/zhButtonLabel arrays.
     */
    private void handleButtonClick(int buttonIndex) {
        // Announce selection
        announceButtonLabel(new String[]{
                "You selected: " + enButtonLabel[buttonIndex],
                "你選擇了: " + zhButtonLabel[buttonIndex]
        });

        // Trigger action based on button index
        switch (buttonIndex) {
            case 0: // Edit Destination
                startActivity(new Intent(this, FindBusEditMenuPage.class));
                break;
            case 1: // Show Arrive Time
                startActivity(new Intent(this, ShowArriveTimePage.class));
                break;
            case 2: // Start Detect Bus Stop
                startActivity(new Intent(this, StartDetectBusStopPage.class));
                break;
            case 3: // Bus Arrival Alert
                startActivity(new Intent(this, BusArrivalAlertPage.class));
                break;
            default:
                Log.e(TAG, "handleButtonClick: Unknown button index");
        }
    }

    /**
     * Load and display the latest bus route from the database.
     */
    private void loadAndDisplayLatestBusRoute() {
        try (Cursor cursor = dbHelper.getLatestBusRoute()) {
            if (cursor != null && cursor.moveToFirst()) {
                // Extract data from cursor
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

                // Build route info string
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
            Log.e(TAG, "loadAndDisplayLatestBusRoute: Error", e);
            txtBusRoute.setText("Error loading data.");
        }
    }

    /**
     * Announce text using TextToSpeech.
     */
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
        loadAndDisplayLatestBusRoute(); // Refresh data when returning to the activity
    }
}