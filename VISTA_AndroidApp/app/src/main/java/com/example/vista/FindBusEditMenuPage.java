// FindBusEditMenuPage.java

package com.example.vista;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// Import TextToSpeech classes if you intend to use TTS
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.FindBusEditMenuFunction.*;

import java.util.Locale;

public class FindBusEditMenuPage extends AppCompatActivity implements OnInitListener {

    private static final String TAG = "FindBusEditMenuPage_debug";

    // UI Components
    private TextView txtBusRoute;
    private Button btnEditRoute, btnEditOutboundDirection, btnEditStartPoint, btnEditDestination;
    private Button btnConfirm, btnNext;

    // Array of edit buttons for cycling
    private Button[] editButtons;
    private int selectedButtonIndex = 0; // Tracks the currently selected edit button

    // TextToSpeech instance (optional)
    private TextToSpeech textToSpeech;

    // Database Helper
    private BusDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_bus_edit_menu_page); // Ensure your layout file is named accordingly

        // Initialize DatabaseHelper
        dbHelper = BusDatabaseHelper.getInstance(this);

        // Initialize UI Components
        txtBusRoute = findViewById(R.id.txtFindBusEditBusRoute);
        btnEditRoute = findViewById(R.id.btnFindBusEditEditRoute);
        btnEditOutboundDirection = findViewById(R.id.btnFindBusEditOutboundDirection);
        btnEditStartPoint = findViewById(R.id.btnFindBusEditStartPoint);
        btnEditDestination = findViewById(R.id.btnFindBusEditDestination);
        btnConfirm = findViewById(R.id.btnFindBusEditConfirm);
        btnNext = findViewById(R.id.btnFindBusEditNext);

        // Populate the editButtons array
        editButtons = new Button[]{btnEditRoute, btnEditOutboundDirection, btnEditStartPoint, btnEditDestination};

        // Display current BusRoute data
        displayBusRouteData();

        // Initialize TextToSpeech (optional)
        textToSpeech = new TextToSpeech(this, this);

        // Highlight the first edit button initially
        highlightSelectedButton();

        // Set onClickListeners for edit buttons (optional, if you want direct access)
        for (Button btn : editButtons) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // When an edit button is clicked directly, set it as selected
                    selectedButtonIndex = getButtonIndex(v);
                    highlightSelectedButton();
                    announceSelectedButton();
                }
            });
        }

        // Set onClickListener for "Next" button
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Cycle to the next button
                selectedButtonIndex = (selectedButtonIndex + 1) % editButtons.length;

                // Highlight the newly selected button
                highlightSelectedButton();

                // Announce the newly selected button
                announceSelectedButton();
            }
        });

        // Set onClickListener for "Confirm" button
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get the currently selected button
                Button selectedButton = editButtons[selectedButtonIndex];

                // Determine which field to edit based on the selected button
                String fieldToEdit = "";
                // Determine which field to edit based on the selected button using if-else
                if (selectedButton.getId() == R.id.btnFindBusEditEditRoute) {
                    fieldToEdit = "EditRoute";
                } else if (selectedButton.getId() == R.id.btnFindBusEditOutboundDirection) {
                    fieldToEdit = "EditOutboundDirection";
                } else if (selectedButton.getId() == R.id.btnFindBusEditStartPoint) {
                    fieldToEdit = "EditStartPoint";
                } else if (selectedButton.getId() == R.id.btnFindBusEditDestination) {
                    fieldToEdit = "EditDestination";
                } else {
                    Log.e(TAG, "Unknown button selected");
                }
//                switch (selectedButton.getId()) {
//                    case R.id.btnFindBusEditEditRoute:
//                        fieldToEdit = "EditRoute"; // Define corresponding activities
//                        break;
//                    case R.id.btnFindBusEditTo:
//                        fieldToEdit = "EditOutboundDirection";
//                        break;
//                    case R.id.btnFindBusEditStartPoint:
//                        fieldToEdit = "EditStartPoint";
//                        break;
//                    case R.id.btnFindBusEditDestination:
//                        fieldToEdit = "EditDestination";
//                        break;
//                    default:
//                        Log.e(TAG, "Unknown button selected");
//                        return;
//                }

                // Announce the action (optional)
                String announceText = "Navigating to " + selectedButton.getText().toString() + " page.";
                speak(announceText);

                // Navigate to the corresponding activity via Intent
                Intent intent;
                switch (fieldToEdit) {
                    case "EditRoute":
                        intent = new Intent(FindBusEditMenuPage.this, EditRouteActivity.class);
                        break;
                    case "EditOutboundDirection":
                        intent = new Intent(FindBusEditMenuPage.this, EditOutboundDirectionActivity.class);
                        break;
                    case "EditStartPoint":
                        intent = new Intent(FindBusEditMenuPage.this, EditStartPointActivity.class);
                        break;
                    case "EditDestination":
                        intent = new Intent(FindBusEditMenuPage.this, EditDestinationActivity.class);
                        break;
                    default:
                        Log.e(TAG, "No corresponding activity found for the selected button");
                        return;
                }

                startActivity(intent);
            }
        });
    }

    /**
     * Fetches and displays the current BusRoute data in the TextView.
     */
    private void displayBusRouteData() {
        Cursor cursor = null;
        try {

            // Retrieve language setting (from database)
            String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
            String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"

            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String toStation = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO));
                String toStation_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO_ZH));
                String startPoint = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT));
                String startPoint_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_ZH));
                String destination = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION));
                String destination_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_ZH));

                String routeInfo = "";

                if(languageCode.equals("en")){
                    routeInfo = "Route:" + routeNumber + "\n" +
                            "To:" + toStation + "\n" +
                            "Start Point:" + startPoint + "\n" +
                            "Destination:" + destination;
                } else if (languageCode.equals("zh")) {
                    routeInfo = "路線:" + routeNumber + "\n" +
                            "開住方向:" + toStation_ZH + "\n" +
                            "起點:" + startPoint_ZH + "\n" +
                            "終點:" + destination_ZH;
                }

                txtBusRoute.setText(routeInfo);
                Log.d(TAG, "displayBusRouteData: Bus route data displayed");
            } else {
                txtBusRoute.setText("No bus route data available.");
                Log.d(TAG, "displayBusRouteData: No bus route data found");
            }
        } catch (Exception e) {
            Log.e(TAG, "displayBusRouteData: Exception occurred", e);
            txtBusRoute.setText("Error loading bus route data.");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Highlights the currently selected edit button and resets others.
     */
    private void highlightSelectedButton() {
        for (int i = 0; i < editButtons.length; i++) {
            if (i == selectedButtonIndex) {
                editButtons[i].setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                editButtons[i].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
        }
    }

    /**
     * Announces the currently selected button using TextToSpeech (optional).
     */
    private void announceSelectedButton() {
        String buttonText = editButtons[selectedButtonIndex].getText().toString();
        String announceText = "Selected: " + buttonText;
        speak(announceText);
    }

    /**
     * Speaks the provided text using TextToSpeech (optional).
     *
     * @param text The text to be spoken.
     */
    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Returns the index of the clicked button in the editButtons array.
     *
     * @param view The clicked view.
     * @return The index of the button, or -1 if not found.
     */
    private int getButtonIndex(View view) {
        for (int i = 0; i < editButtons.length; i++) {
            if (editButtons[i].getId() == view.getId()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * TextToSpeech initialization callback (optional).
     *
     * @param status The initialization status.
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "onInit: TTS Language not supported.");
            } else {
                Log.d(TAG, "onInit: TTS Initialized successfully.");
                // Optionally, announce the first selected button
                announceSelectedButton();
            }
        } else {
            Log.e(TAG, "onInit: TTS Initialization failed.");
        }
    }

    /**
     * Release TextToSpeech resources when the activity is destroyed (optional).
     */
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "onDestroy: TextToSpeech resources released.");
        }
        super.onDestroy();
    }


    @Override
    protected void onRestart(){
        // Display current BusRoute data
        super.onRestart();
        displayBusRouteData();
    }
}