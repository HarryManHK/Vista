package com.example.vista.FindBusEditMenuFunction;

import android.content.Context;
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

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.R;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * EditOutboundDirectionActivity: Allows users to select an outbound direction for a bus route.
 * Users can select an item manually or use the "Next" button to cycle through options.
 * Upon confirmation, the selected outbound direction is updated in the database.
 */
public class EditOutboundDirectionActivity extends AppCompatActivity {
    private DownloadTask task = null;
    private ListView lvShowRouteOutbound;
    private String[] listItems;
    private BusDatabaseHelper dbHelper;
    private SettingDatabaseHelper SettingDBHelper;
    private String BusRoute;
    private String TAG = "EditOutboundDirectionActivity";
    private int selectedPosition = -1; // Track the selected item in the ListView
    private String[] origMultiLan;
    private String[] destMultiLan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_outbound_direction);

        lvShowRouteOutbound = findViewById(R.id.lvShowRouteOutbound);
        dbHelper = BusDatabaseHelper.getInstance(this); // Use Singleton instance
        // Load bus route data and call the API
        getBusRouteData();

        // Set up the ListView's click listener to change background color and play sound
        lvShowRouteOutbound.setOnItemClickListener((parent, view, position, id) -> {
            selectItem(position, view);
        });

        // Set up the "Confirm" button
        Button btnConfirm = findViewById(R.id.btnMainMenuConfirm);
        btnConfirm.setOnClickListener(v -> {


            if (selectedPosition != -1) {
                // Update database with the selected route information
                String selectedOutbound = "";
                String selectedOutbound_ZH = "";

                if (selectedPosition == 0){
                    selectedOutbound_ZH = destMultiLan[1];
                    selectedOutbound = destMultiLan[0];
                }else if(selectedPosition == 1){
                    selectedOutbound_ZH = destMultiLan[1];
                    selectedOutbound = destMultiLan[0];
                }



                updateDatabase(selectedOutbound, selectedOutbound_ZH);
            } else {
                Toast.makeText(EditOutboundDirectionActivity.this, "Please select an outbound direction", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the "Next" button
        Button btnNext = findViewById(R.id.btnMainMenuNext);
        btnNext.setOnClickListener(v -> {
            selectNextItem();
        });

        // Start the DownloadTask after BusRoute is set
        if (BusRoute != null && !BusRoute.isEmpty()) {
            if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
                task = new DownloadTask(this);
                task.execute("https://data.etabus.gov.hk/v1/transport/kmb/route/" + BusRoute + "/outbound/1");
            }
        } else {
            Toast.makeText(this, "Bus route data not available", Toast.LENGTH_SHORT).show();
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
            View prevView = lvShowRouteOutbound.getChildAt(selectedPosition);
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
        if (listItems == null || listItems.length == 0) {
            Toast.makeText(this, "No items to select", Toast.LENGTH_SHORT).show();
            return;
        }

        int nextPosition;
        if (selectedPosition == -1) {
            nextPosition = 0; // Select first item if none selected
        } else {
            nextPosition = (selectedPosition + 1) % listItems.length; // Cycle to next item
        }

        // Get the view for the next position
        View nextView = lvShowRouteOutbound.getChildAt(nextPosition);
        if (nextView != null) {
            selectItem(nextPosition, nextView);
            lvShowRouteOutbound.setSelection(nextPosition); // Scroll to the selected item if needed
        } else {
            // If the view is not visible, set the selection and let the user click to see it
            lvShowRouteOutbound.setSelection(nextPosition);
            selectedPosition = nextPosition;
            // Optionally, you can notify the user to manually select
            Toast.makeText(this, "Please select the highlighted item", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * AsyncTask to download bus route data from the API.
     */
    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context mContext;
        // Constructor to pass context to the AsyncTask
        public DownloadTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected String doInBackground(String... values) {
            InputStream inputStream = null;
            String result = "";
            try {
                URL url = new URL(values[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                inputStream = con.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
                result = stringBuilder.toString();
            } catch (Exception e) {
                Log.e(TAG, "DownloadTask Error: " + e.toString());
                result = null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(mContext, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Parse the result as a JSONObject
                JSONObject jsonObject = new JSONObject(result);

                // Extract the "data" JSONObject
                JSONObject dataObject = jsonObject.getJSONObject("data");

                // Retrieve language setting (from database)
                String[] languageSetting = SettingDatabaseHelper.getInstance(mContext).getLanguageSetting();
                String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"

                // Determine which fields to show based on the language setting
                String orig = languageCode.equals("zh") ? dataObject.getString("orig_tc") : dataObject.getString("orig_en");
                String dest = languageCode.equals("zh") ? dataObject.getString("dest_tc") : dataObject.getString("dest_en");

                destMultiLan = new String[2];
                destMultiLan[0] = dataObject.getString("dest_en");
                destMultiLan[1] = dataObject.getString("dest_tc");

                origMultiLan = new String[2];
                origMultiLan[0] = dataObject.getString("orig_en");
                origMultiLan[1] = dataObject.getString("orig_tc");

                // Set these values to listItems array
                listItems = new String[2]; // Only need 2 items for now
                listItems[0] = dest;  // "dest_en" to listItems[0] with label
                listItems[1] = orig;  // "orig_en" to listItems[1] with label

                // Set the adapter for the ListView
                ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                        android.R.layout.simple_list_item_1, listItems);
                lvShowRouteOutbound.setAdapter(adapter);
            } catch (Exception e) {
                Log.e("EditOutboundDirection", "onPostExecute Error: " + e.toString());
                Toast.makeText(mContext, "Error parsing data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Retrieves the latest bus route data from the database.
     */
    private void getBusRouteData() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                BusRoute = routeNumber; // Get bus route number
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
     * Updates the database with the selected outbound direction.
     *
     * @param selectedOutbound The selected outbound direction.
     */
    private void updateDatabase(String selectedOutbound,String selectedOutbound_ZH) {
        // Determine the bound based on the selected position
        String bound;
        if (selectedPosition == 1) {
            bound = "inbound";
        } else if (selectedPosition == 0) {
            bound = "outbound";
        } else {
            bound = "---"; // Default or unknown bound
        }

        // Placeholder values for the other columns
        String startPoint = "---"; // Replace with actual start point if available
        String startPoint_ZH = "---"; // Replace with actual start point if available
        String startPointSeq = "---"; // Replace with actual sequence if available
        String startPointStopId = "---"; // Replace with actual stop ID if available
        String startPointLat = "---"; // Replace with actual latitude if available
        String startPointLong = "---"; // Replace with actual longitude if available
        String destination = "---"; // Replace with actual destination if available
        String destination_ZH = "---"; // Replace with actual destination if available
        String destinationStopId = "---"; // Replace with actual stop ID if available
        String destinationSeq = "---"; // Replace with actual sequence if available
        String destinationLat = "---"; // Replace with actual latitude if available
        String destinationLong = "---"; // Replace with actual longitude if available

        // Update the database with the selected information
        long result = dbHelper.insertOrUpdateBusRoute(
                dbHelper.getWritableDatabase(),
                BusRoute,
                selectedOutbound,    // to_station
                selectedOutbound_ZH, // to_station_ZH
                bound,               // bound
                startPoint,          // start_point
                startPoint_ZH,          // start_point
                startPointSeq,       // start_point_seq
                startPointStopId,    // start_point_stop_id
                startPointLat,       // start_point_lat
                startPointLong,      // start_point_long
                destination,         // destination
                destination_ZH,         // destination
                destinationStopId,   // destination_stop_id
                destinationSeq,      // destination_seq
                destinationLat,      // destination_lat
                destinationLong      // destination_long
        );

        if (result != -1) {
            Log.d(TAG, "Database updated successfully with selected bus route");
            Toast.makeText(this, "Outbound direction updated", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Error updating database");
            Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
        }
    }
}