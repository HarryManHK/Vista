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

import com.example.vista.DatabaseHelper;
import com.example.vista.R;

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
 * EditDestinationActivity: Allows users to select a destination bus stop for a bus route.
 * Fetches bus stops from the API and lists them in a ListView.
 * Users can select a bus stop and save its details to the database.
 */
public class EditDestinationActivity extends AppCompatActivity {
    private DownloadTask task = null;
    private ListView lvShowAllStop;
    private ArrayList<BusStop> busStops; // Custom class to hold bus stop details
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private String BusRoute;
    private String bound; // "inbound" or "outbound"
    private String TAG = "EditDestinationActivity";
    private int selectedPosition = -1; // Track the selected item in the ListView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_destination);

        // Handle Edge-to-Edge UI if needed
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ListView and DatabaseHelper
        lvShowAllStop = findViewById(R.id.lvShowAllStop);
        dbHelper = DatabaseHelper.getInstance(this);
        busStops = new ArrayList<>();

        // Load bus route data
        getBusRouteData();

        // Set up the ListView's click listener to handle selection
        lvShowAllStop.setOnItemClickListener((parent, view, position, id) -> {
            selectItem(position, view);
        });

        // Set up the "Confirm" button
        Button btnConfirm = findViewById(R.id.btnEditDestinationActivityConfirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != -1) {
                // Get the selected bus stop
                BusStop selectedBusStop = busStops.get(selectedPosition);
                updateDatabase(selectedBusStop);
                finish();
            } else {
                Toast.makeText(EditDestinationActivity.this, "Please select a destination bus stop", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the "Next" button
        Button btnNext = findViewById(R.id.btnEditDestinationActivityNext);
        btnNext.setOnClickListener(v -> {
            // Navigate to the next activity, e.g., SummaryActivity or any other desired activity
            // Intent intent = new Intent(EditDestinationActivity.this, SummaryActivity.class);
            // startActivity(intent);
            // finish(); // Optional: finish current activity
            Toast.makeText(this, "Next button clicked. Implement navigation as needed.", Toast.LENGTH_SHORT).show();
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
        // Reset the background of the previously selected item
        if (selectedPosition != -1) {
            View prevView = lvShowAllStop.getChildAt(selectedPosition);
            if (prevView != null) {
                prevView.setBackgroundColor(getResources().getColor(android.R.color.transparent)); // reset to transparent
            }
        }

        // Set the background of the currently selected item
        view.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // selected color
        selectedPosition = position; // Update selected position

        // Play click sound
        view.playSoundEffect(SoundEffectConstants.CLICK);
    }

    /**
     * Selects the next item in the ListView. Cycles back to the first item if at the end.
     */
    private void selectNextItem() {
        if (busStops == null || busStops.size() == 0) {
            Toast.makeText(this, "No items to select", Toast.LENGTH_SHORT).show();
            return;
        }

        int nextPosition;
        if (selectedPosition == -1) {
            nextPosition = 0; // Select first item if none selected
        } else {
            nextPosition = (selectedPosition + 1) % busStops.size(); // Cycle to next item
        }

        // Get the view for the next position
        View nextView = lvShowAllStop.getChildAt(nextPosition);
        if (nextView != null) {
            selectItem(nextPosition, nextView);
            lvShowAllStop.setSelection(nextPosition); // Scroll to the selected item if needed
        } else {
            // If the view is not visible, set the selection and let the user click to see it
            lvShowAllStop.setSelection(nextPosition);
            selectedPosition = nextPosition;
            // Optionally, you can notify the user to manually select
            Toast.makeText(this, "Please select the highlighted item", Toast.LENGTH_SHORT).show();
        }
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
                    String lat = stopData.getString("lat");
                    String lon = stopData.getString("long"); // Note: "long" is a reserved word, better use "lon"

                    // Create a BusStop object
                    BusStop busStop = new BusStop(stopId, nameEn, seq, lat, lon);
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
                Toast.makeText(EditDestinationActivity.this, "Failed to fetch bus stops data", Toast.LENGTH_SHORT).show();
                return;
            }

            busStops = result; // Assign the fetched stops to busStops list

            // Prepare the list of stop names for the ListView
            ArrayList<String> stopNames = new ArrayList<>();
            for (BusStop stop : busStops) {
                stopNames.add(stop.getNameEn());
            }

            // Set the adapter for the ListView
            adapter = new ArrayAdapter<>(EditDestinationActivity.this,
                    android.R.layout.simple_list_item_1, stopNames);
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
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_NUMBER));
                BusRoute = routeNumber; // Get bus route number

                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BOUND));
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
     * Updates the database with the selected destination bus stop details.
     *
     * @param selectedBusStop The selected bus stop object.
     */
    private void updateDatabase(BusStop selectedBusStop) {
        // Retrieve current database record
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_NUMBER));
                String toStation = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TO));
                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BOUND));

                // Prepare all other columns, setting to existing or "---" if not being updated
                String destination = selectedBusStop.getNameEn();
                String destinationSeq = String.valueOf(selectedBusStop.getSeq());
                String destinationStopId = selectedBusStop.getStopId();
                String destinationLat = selectedBusStop.getLat();
                String destinationLong = selectedBusStop.getLon();

                // Fetch existing start point details
                String startPoint = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT));
                String startPointSeq = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT_SEQ));
                String startPointStopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT_STOP_ID));
                String startPointLat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT_LAT));
                String startPointLong = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT_LONG));

                // Update the database with the new destination details
                long result = dbHelper.insertOrUpdateBusRoute(
                        dbHelper.getWritableDatabase(),
                        routeNumber,
                        toStation,
                        boundValue,
                        startPoint,
                        startPointSeq,
                        startPointStopId,
                        startPointLat,
                        startPointLong,
                        destination,
                        destinationStopId,
                        destinationSeq,
                        destinationLat,
                        destinationLong
                );

                if (result != -1) {
                    Log.d(TAG, "Database updated successfully with selected destination");
                    Toast.makeText(this, "Destination updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error updating database");
                    Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "No bus route data found for updating destination.");
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
        private int seq;
        private String lat;
        private String lon;

        public BusStop(String stopId, String nameEn, int seq, String lat, String lon) {
            this.stopId = stopId;
            this.nameEn = nameEn;
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