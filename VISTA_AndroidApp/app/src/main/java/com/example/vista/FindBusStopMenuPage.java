    package com.example.vista;
    import android.content.Intent;
    import android.database.Cursor;
    import android.os.Bundle;
    import android.view.View;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;

    // Provide audio Speech
    import android.speech.tts.TextToSpeech;
    import android.speech.tts.TextToSpeech.OnInitListener;
    import java.util.Locale;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import android.util.Log; // For Log.d usage

    import com.example.vista.DatabaseHelper.BusDatabaseHelper;
    import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
    import com.example.vista.TextToSpeech.CustomTextToSpeech;

    public class FindBusStopMenuPage extends AppCompatActivity {

        private static final String TAG = "FindBusStopMenuPage_debug";  // For logging

        // Main action buttons
        private Button btnEditDestination;
        private Button btnShowArriveTime;
        private Button btnStartDetectBusStop;
        private Button btnBusArrivalAlert;

        // Swap buttons
        private Button btnConfirm;
        private Button btnNext;

        private Button[] actionButtons;  // Array to hold main action buttons
        private String[] zhButtonLabel = {"修改目的地","顯示到站時間","開始偵測巴士站","巴士到站提醒"}; //Array to hold the action label chinese name
        private String[] enButtonLabel = {"Edit Destination", "Show Arrive Time", "Start Dectect Bus Stop", "Bus Arrival Alert"}; //Array to hold the action label english name
        private int selectedButtonIndex = 0;  // Index to keep track of the selected button

        private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class
        private TextView txtBusRoute;  // TextView to display bus route

        private BusDatabaseHelper dbHelper;  // Database helper instance

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

            Log.d(TAG, "onCreate: FindBusStopMenuPage initialized");

            // Initialize TextToSpeech
            initializeTextToSpeech();

            // Initialize buttons
            btnEditDestination       = findViewById(R.id.btnFindBusStopMenuPageEditDestination);
            btnShowArriveTime        = findViewById(R.id.btnFindBusStopMenuPageShowArriveTIme);
            btnStartDetectBusStop    = findViewById(R.id.btnFindBusStopMenuPageStartDetectBusStop);
            btnBusArrivalAlert       = findViewById(R.id.btnFindBusStopMenuPageBusArrivalAlert);
            btnConfirm               = findViewById(R.id.btnFindBusStopMenuPageConfirm);
            btnNext                  = findViewById(R.id.btnFindBusStopMenuPageNext);
            txtBusRoute              = findViewById(R.id.txtFindBusStopMenuPageBusRoute);
            // Add all main action buttons to an array for easy cycling
            actionButtons = new Button[]{btnEditDestination, btnShowArriveTime, btnStartDetectBusStop, btnBusArrivalAlert};


            // Initial setup: Highlight the first button
            updateButtonHighlight();

            // Announce the initially selected button
            announceButtonLabel(new String[]{enButtonLabel[selectedButtonIndex], zhButtonLabel[selectedButtonIndex]});

            // Set onClickListeners for main action buttons
            setMainActionButtonsListeners();


            // Set onClickListener for "Next" button
            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cycleSelectedButton();
                }
            });

            // Set onClickListener for "Confirm" button
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmSelectedButton();
                }
            });


            // Initialize DatabaseHelper as a singleton
            dbHelper = BusDatabaseHelper.getInstance(this);

            txtBusRoute = findViewById(R.id.txtFindBusStopMenuPageBusRoute);
            if (txtBusRoute == null) {
                Log.e(TAG, "onCreate: TextView txtFindBusStopMenuPageBusRoute not found");
            }

            // testing
    //        dbHelper.insertBusRoute("43A", "佐敦(西九龍站)", "2站", "2站");
    //        Log.d(TAG, "onCreate: Inserted or Updated bus route 42A");

            // Load and display the latest bus route
            loadAndDisplayLatestBusRoute();
        }

        /**
         * Initialize the TextToSpeech engine.
         */
        private void initializeTextToSpeech() {
            customTextToSpeech = new CustomTextToSpeech(FindBusStopMenuPage.this);
        }

        private void setButtonLabel(){

        }

        /**
         * Set onClickListeners for main action buttons.
         */
        private void setMainActionButtonsListeners() {
            btnEditDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleButtonAction(btnEditDestination, "Edit Destination");
                }
            });

            btnShowArriveTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleButtonAction(btnShowArriveTime, "Show Arrive Time");
                }
            });

            btnStartDetectBusStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleButtonAction(btnStartDetectBusStop, "Start Detect Bus Stop");
                }
            });

            btnBusArrivalAlert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleButtonAction(btnBusArrivalAlert, "Bus Arrival Alert");
                }
            });
        }

        /**
         * Handle the action when a main button is clicked.
         *
         * @param button The button that was clicked.
         * @param label  The label associated with the button.
         */
        private void handleButtonAction(Button button, String label) {

            Toast.makeText(FindBusStopMenuPage.this, label + " clicked!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "handleButtonAction: " + label + " clicked");
            announceButtonLabel(new String[]{"You have selected " + enButtonLabel[selectedButtonIndex] + ". Confirming.", "你選擇了" +zhButtonLabel[selectedButtonIndex] + "功能"});


            //open activity
            Intent intent;
            int id = button.getId();

            if (id == R.id.btnFindBusStopMenuPageEditDestination) {
                intent = new Intent(FindBusStopMenuPage.this, FindBusEditMenuPage.class);
                startActivity(intent);
            } else if (id == R.id.btnFindBusStopMenuPageShowArriveTIme) {
                intent = new Intent(FindBusStopMenuPage.this, ShowArriveTimePage.class);
                startActivity(intent);
            } else if (id == R.id.btnFindBusStopMenuPageStartDetectBusStop) {
                intent = new Intent(FindBusStopMenuPage.this, StartDetectBusStopPage.class);
                startActivity(intent);
            } else if (id == R.id.btnFindBusStopMenuPageBusArrivalAlert) {
                intent = new Intent(FindBusStopMenuPage.this, BusArrivalAlertPage.class);
                startActivity(intent);
            } else {
                Log.d(TAG, "handleButtonAction: Unknown button clicked");
            }
        }

        /**
         * Cycle to the next button in the actionButtons array.
         */
        private void cycleSelectedButton() {
            selectedButtonIndex = (selectedButtonIndex + 1) % actionButtons.length;
            updateButtonHighlight();

            announceButtonLabel(new String[]{enButtonLabel[selectedButtonIndex], zhButtonLabel[selectedButtonIndex]});
            Log.d(TAG, "cycleSelectedButton: New selectedButtonIndex=" + selectedButtonIndex);
        }

        /**
         * Confirm and perform the action of the currently selected button.
         */
        private void confirmSelectedButton() {
            Button selectedButton = actionButtons[selectedButtonIndex];
            String label = selectedButton.getText().toString();
            Log.d(TAG, "confirmSelectedButton: Confirming " + label);
            announceButtonLabel(new String[]{"You have selected " + enButtonLabel[selectedButtonIndex] + ". Confirming.", "你選擇了" +zhButtonLabel[selectedButtonIndex] + "功能"});

            // Simulate button click
            selectedButton.performClick();
        }

        /**
         * Update the highlight (background color) of the action buttons based on the selected index.
         */
        private void updateButtonHighlight() {
            for (int i = 0; i < actionButtons.length; i++) {
                if (i == selectedButtonIndex) {
                    // Highlight selected button (e.g., set to yellow)
                    actionButtons[i].setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                    actionButtons[i].setTextColor(ContextCompat.getColor(this, android.R.color.white));
                } else {
                    // Reset to default color
                    actionButtons[i].setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                    actionButtons[i].setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
            }
        }


        /**
         * Load the latest bus route from the database and display it in the TextView.
         */
        private void loadAndDisplayLatestBusRoute() {
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
                    Log.d(TAG, "loadAndDisplayLatestBusRoute: Route information updated in TextView");
                } else {
                    Log.d(TAG, "loadAndDisplayLatestBusRoute: No bus route data found");
                    txtBusRoute.setText("No bus route data available.");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadAndDisplayLatestBusRoute: Exception occurred", e);
                txtBusRoute.setText("Error loading bus route data.");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        /**
         * Use TextToSpeech to announce the given label.
         *
         * @param label The text to announce.
         */
        private void announceButtonLabel(String[] label) {
            // Announce the label based on the selected language
            customTextToSpeech.speak(label);
        }

        @Override
        protected void onDestroy() {
            // Release the TextToSpeech resources when the activity is destroyed
            if (customTextToSpeech != null) {
                customTextToSpeech.shutdown();
                Log.d(TAG, "onDestroy: TextToSpeech resources released");
            }
            super.onDestroy();
        }

        @Override
        protected void onRestart(){
            // Display current BusRoute data
            super.onRestart();
            loadAndDisplayLatestBusRoute();
        }
    }