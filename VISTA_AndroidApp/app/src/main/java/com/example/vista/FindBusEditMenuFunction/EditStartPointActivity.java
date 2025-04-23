package com.example.vista.FindBusEditMenuFunction;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.view.SoundEffectConstants;

import com.example.vista.BusStopListViewAdapter;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.R;
import com.example.vista.TextToSpeech.CustomTextToSpeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * EditStartPointActivity: Allows users to select a start point bus stop for a bus route.
 * Fetches bus stops from the API and lists them in a ListView.
 * Users can select a bus stop and save its details to the database.
 */
public class EditStartPointActivity extends AppCompatActivity {
    private DownloadTask task = null;
    private ListView lvShowAllStop;
    private ArrayList<BusStop> busStops; // Custom class to hold bus stop details
    private ArrayAdapter<String> adapter;
    private BusDatabaseHelper dbHelper;
    private String BusRoute;
    private String bound; // "inbound" or "outbound"
    private String TAG = "EditStartPointActivity";
    private int selectedPosition = -1; // Track the selected item in the ListView
    // Retrieve language setting (from database)
    private String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
    private String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"
    private CustomTextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_start_point);

        // Handle Edge-to-Edge UI if needed
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ListView and DatabaseHelper
        lvShowAllStop = findViewById(R.id.lvShowAllStop);
        dbHelper = BusDatabaseHelper.getInstance(this);
        busStops = new ArrayList<>();

        // Initialize TextToSpeech
        tts = new CustomTextToSpeech(this);

        // Load bus route data
        getBusRouteData();

        // Set up the ListView's click listener to handle selection
        lvShowAllStop.setOnItemClickListener((parent, view, position, id) -> {
            selectItem(position, view);
            BusStop selectedBusStop = busStops.get(position);
            tts.speak(new String[]{selectedBusStop.getNameEn(), selectedBusStop.getNameZH()});
        });

        // Set up the "Confirm" button
        Button btnConfirm = findViewById(R.id.btnMainMenuConfirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != -1) {
                // Get the selected bus stop
                BusStop selectedBusStop = busStops.get(selectedPosition);
                updateDatabase(selectedBusStop);
            } else {
                Toast.makeText(EditStartPointActivity.this, "Please select a start point bus stop", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the "Next" button
        Button btnNext = findViewById(R.id.btnMainMenuNext);
        btnNext.setOnClickListener(v -> {
            selectNextItem();
        });

        // Start the DownloadTask to fetch bus stops
        if (BusRoute != null && !BusRoute.isEmpty() && bound != null && !bound.isEmpty()) {
            if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
                task = new DownloadTask();
                // Convert bound to lowercase as API expects "inbound" or "outbound"
                task.execute("https://data.etabus.gov.hk/v1/transport/kmb/route-stop/" + BusRoute + "/" + bound.toLowerCase() + "/1");
            }
        } else {
            Toast.makeText(this, "Bus route or bound data not available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the selection of a ListView item.
     *
     * @param position The position of the item clicked.
     * @param view     The view of the item clicked.
     */
    private void selectItem(int position, View view) {
        selectedPosition = position;
        ((BusStopListViewAdapter) adapter).setSelectedPosition(position);
        view.playSoundEffect(SoundEffectConstants.CLICK);
    }


    /**
     * Selects the next item in the ListView. Cycles back to the first item if at the end.
     */
    private void selectNextItem() {
        if (busStops == null || busStops.isEmpty()) {
            Toast.makeText(this, "No items to select", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate next position
        int nextPosition = (selectedPosition == -1)
                ? 0
                : (selectedPosition + 1) % busStops.size();

        // Update selection
        ((BusStopListViewAdapter) adapter).setSelectedPosition(nextPosition);
        selectedPosition = nextPosition;

        // Scroll to position
        lvShowAllStop.smoothScrollToPosition(nextPosition);

        // Play sound if visible
        int firstVisible = lvShowAllStop.getFirstVisiblePosition();
        int lastVisible = lvShowAllStop.getLastVisiblePosition();

        if (nextPosition >= firstVisible && nextPosition <= lastVisible) {
            View visibleItem = lvShowAllStop.getChildAt(nextPosition - firstVisible);
            if (visibleItem != null) {
                visibleItem.playSoundEffect(SoundEffectConstants.CLICK);
            }
        }

        BusStop selectedBusStop = busStops.get(nextPosition);
        tts.speak(new String[]{selectedBusStop.getNameEn(), selectedBusStop.getNameZH()});
    }


    /**
     * AsyncTask to download bus stops data from the API.
     */
    private class DownloadTask extends AsyncTask<String, Void, ArrayList<BusStop>> {
        @Override
        protected ArrayList<BusStop> doInBackground(String... values) {
            String apiUrl = values[0];
            ArrayList<BusStop> stopsList = new ArrayList<>();
            try {
                // Fetch route-stop data
                URL url = new URL(apiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                // Check response code
                int responseCode = con.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "DownloadTask: HTTP error code " + responseCode);
                    return null;
                }

                InputStream inputStream = con.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder routeStopBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    routeStopBuilder.append(line);
                }
                inputStream.close();
                String routeStopJson = routeStopBuilder.toString();

                // Parse the route-stop JSON
                JSONObject jsonObject = new JSONObject(routeStopJson);
                JSONArray dataArray = jsonObject.getJSONArray("data"); // "data" is a JSONArray

                // Iterate through each stop in the data array
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject stopObj = dataArray.getJSONObject(i);
                    String stopId = stopObj.getString("stop");
                    String seqStr = stopObj.getString("seq");
                    int seq = Integer.parseInt(seqStr);

                    // Fetch stop details
                    String stopDetailsApi = "https://data.etabus.gov.hk/v1/transport/kmb/stop/" + stopId;
                    URL stopUrl = new URL(stopDetailsApi);
                    HttpURLConnection stopCon = (HttpURLConnection) stopUrl.openConnection();
                    stopCon.setRequestMethod("GET");
                    stopCon.connect();

                    int stopResponseCode = stopCon.getResponseCode();
                    if (stopResponseCode != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "DownloadTask: HTTP error code for stop " + stopId + " is " + stopResponseCode);
                        continue; // Skip this stop
                    }

                    InputStream stopInputStream = stopCon.getInputStream();
                    BufferedReader stopBufferedReader = new BufferedReader(new InputStreamReader(stopInputStream));
                    StringBuilder stopBuilder = new StringBuilder();
                    String stopLine;
                    while ((stopLine = stopBufferedReader.readLine()) != null) {
                        stopBuilder.append(stopLine);
                    }
                    stopInputStream.close();
                    String stopJson = stopBuilder.toString();

                    // Parse the stop details JSON
                    JSONObject stopJsonObj = new JSONObject(stopJson);
                    JSONObject stopData = stopJsonObj.getJSONObject("data");

                    String nameEn = stopData.getString("name_en");
                    String nameZH = stopData.getString("name_tc");
                    String lat = stopData.getString("lat");
                    String lon = stopData.getString("long"); // Note: "long" is a reserved word, better use "lon"

                    // Create a BusStop object
                    BusStop busStop = new BusStop(stopId, nameEn, nameZH, seq, lat, lon);
                    stopsList.add(busStop);
                }
            } catch (Exception e) {
                Log.e(TAG, "DownloadTask Exception: " + e.toString());
                return null;
            }
            return stopsList;
        }

        @Override
        protected void onPostExecute(ArrayList<BusStop> result) {
            if (result == null) {
                Toast.makeText(EditStartPointActivity.this, "Failed to fetch bus stops data", Toast.LENGTH_SHORT).show();
                return;
            }

            busStops = result; // Assign the fetched stops to busStops list

            // Prepare the list of stop names for the ListView
            ArrayList<String> stopNames = new ArrayList<>();
            for (BusStop stop : busStops) {
                if(languageCode.equals("en")){
                    stopNames.add(stop.getNameEn());
                }else if(languageCode.equals("zh")){
                    stopNames.add(stop.getNameZH());
                }
            }

            // Set the adapter for the ListView
            adapter = new BusStopListViewAdapter(EditStartPointActivity.this,
                    R.layout.list_item_white_text, stopNames);
            lvShowAllStop.setAdapter(adapter);

            Log.d(TAG, "DownloadTask: Bus stops loaded successfully");
        }
    }

    /**
     * Retrieves the latest bus route and bound from the database.
     */
    private void getBusRouteData() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                BusRoute = routeNumber; // Get bus route number

                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
                if (boundValue != null) {
                    // Map bound value: assuming "O" = "outbound", "I" = "inbound"
                    if (boundValue.equalsIgnoreCase("O")) {
                        bound = "outbound";
                    } else if (boundValue.equalsIgnoreCase("I")) {
                        bound = "inbound";
                    } else {
                        bound = boundValue.toLowerCase(); // default to lowercase value
                    }
                } else {
                    bound = "";
                }
            } else {
                Log.d(TAG, "Error loading bus route data.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "getBusRouteData Error: " + e.toString());
            Toast.makeText(this, "Error loading bus route data", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Updates the database with the selected start point bus stop details.
     *
     * @param selectedBusStop The selected bus stop object.
     */
    private void updateDatabase(BusStop selectedBusStop) {
        // Retrieve current database record
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String toStation = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO));
                String toStation_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO_ZH));
                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));

                // Prepare all other columns, setting to existing or "---" if not being updated
                String startPoint = selectedBusStop.getNameEn();
                String startPoint_ZH = selectedBusStop.getNameZH();;
                String startPointSeq = String.valueOf(selectedBusStop.getSeq());
                String startPointStopId = selectedBusStop.getStopId();
                String startPointLat = selectedBusStop.getLat();
                String startPointLong = selectedBusStop.getLon();

                // Fetch existing destination details
                String destination = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION));
                String destination_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_ZH));
                String destinationStopId = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_STOP_ID));
                String destinationSeq = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_SEQ));
                String destinationLat = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LAT));
                String destinationLong = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_DESTINATION_LONG));

                // Update the database with the new start point details
                long result = dbHelper.insertOrUpdateBusRoute(
                        dbHelper.getWritableDatabase(),
                        routeNumber,
                        toStation,
                        toStation_ZH,
                        boundValue,
                        startPoint,
                        startPoint_ZH,
                        startPointSeq,
                        startPointStopId,
                        startPointLat,
                        startPointLong,
                        destination,
                        destination_ZH,
                        destinationStopId,
                        destinationSeq,
                        destinationLat,
                        destinationLong
                );

                if (result != -1) {
                    Log.d(TAG, "Database updated successfully with selected start point");
                    Toast.makeText(this, "Start point updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error updating database");
                    Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "No bus route data found for updating start point.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateDatabase Error: " + e.toString());
            Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * BusStop: A class representing a bus stop with relevant details.
     */
    private class BusStop {
        private String stopId;
        private String nameEn;
        private String nameZH;
        private int seq;
        private String lat;
        private String lon;

        public BusStop(String stopId, String nameEn, String nameZH, int seq, String lat, String lon) {
            this.stopId = stopId;
            this.nameEn = nameEn;
            this.nameZH = nameZH;
            this.seq = seq;
            this.lat = lat;
            this.lon = lon;
        }

        public String getStopId() {
            return stopId;
        }

        public String getNameEn() {
            return nameEn;
        }

        public String getNameZH() {
            return nameZH;
        }

        public int getSeq() {
            return seq;
        }

        public String getLat() {
            return lat;
        }

        public String getLon() {
            return lon;
        }
    }
}