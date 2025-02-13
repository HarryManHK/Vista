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
import com.example.vista.DatabaseHelper.BusStopInformation;
import com.example.vista.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.vista.util.SimpleHttpUtil;

/**
 * EditRouteActivity: Allows users to input a bus route and save it to the database.
 * Then it fetches the outbound/inbound stops from KMB APIs and inserts them
 * into BusStopInformation so they can be displayed on the map later.
 */
public class EditRouteActivity extends AppCompatActivity {
    private static final String TAG = "EditRouteActivity";

    private EditText etBusRoute;
    private BusDatabaseHelper busDatabaseHelper;
    private BusStopInformation busStopInfoHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_route);

        // Handle Edge-to-Edge UI insets if needed
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etBusRoute = findViewById(R.id.etBusRoute);

        // Get database helpers
        busDatabaseHelper = BusDatabaseHelper.getInstance(this);
        busStopInfoHelper = new BusStopInformation(this);

        // Confirm button
        Button btnConfirm = findViewById(R.id.btnEditRouteActivityConfirm);
        btnConfirm.setOnClickListener(v -> {
            String routeNumber = etBusRoute.getText().toString().trim().toUpperCase();
            if (routeNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a bus route", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) Insert or update the bus route in BusDatabaseHelper (start/destination = "---" for now)
            long result = busDatabaseHelper.insertBusRoute(
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

            if (result == -1) {
                Toast.makeText(this, "Error saving bus route", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error saving bus route");
                return;
            }

            Toast.makeText(this, "Bus route saved successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Bus route saved with ID: " + result);

            // 2) Fetch OUTBOUND stops and insert them into BusStopInformation
            //    (Change "O" to whatever you prefer to store for outbound.)
            fetchAndInsertKmbStops(routeNumber, true);

            // 3) Fetch INBOUND stops and insert them into BusStopInformation
            //    (Change "I" to whatever you prefer to store for inbound.)
            fetchAndInsertKmbStops(routeNumber, false);

            // Clear the input field
            etBusRoute.setText("");
        });

        // Next button
        Button btnNext = findViewById(R.id.btnEditRouteActivityNext);
        btnNext.setOnClickListener(v -> {
            // Example: Show the keyboard for the EditText
            etBusRoute.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etBusRoute, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    /**
     * Fetch stops from KMB's route-stop API for either outbound or inbound,
     * then fetch each stop detail and insert into BusStopInformation.
     *
     * @param routeNumber The bus route, e.g. "42"
     * @param isOutbound  true if fetching outbound, false if inbound
     */
    private void fetchAndInsertKmbStops(String routeNumber, boolean isOutbound) {
        // We do this in a background thread.
        // In a real app, consider using Retrofit, OkHttp, or another networking library.
        new Thread(() -> {
            try {
                // A) Build the route-stop URL for outbound or inbound
                //    inbound = "I", outbound = "O" (KMB uses "outbound" = "O" and "inbound" = "I")
                String boundKey = isOutbound ? "outbound" : "inbound";
                String boundValue = isOutbound ? "O" : "I"; // This is how we store it in the DB

                String routeStopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/"
                        + routeNumber + "/" + boundKey + "/1";

                String routeStopJson = SimpleHttpUtil.httpGet(routeStopUrl);  // see below for example

                // Parse JSON to get the array of stops
                JSONObject jsonObj = new JSONObject(routeStopJson);
                JSONArray dataArray = jsonObj.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject stopObj = dataArray.getJSONObject(i);

                    // route = "42", bound = "I" or "O", service_type = "1", seq = "1", stop = "xxxxxxx"
                    String seq = stopObj.getString("seq");
                    String stopId = stopObj.getString("stop");

                    // B) For each stop ID, call the Stop API to get name_en, name_tc, lat, long
                    String stopApiUrl = "https://data.etabus.gov.hk/v1/transport/kmb/stop/" + stopId;
                    String stopJson = SimpleHttpUtil.httpGet(stopApiUrl);

                    JSONObject stopDetail = new JSONObject(stopJson).getJSONObject("data");
                    String stopNameEn = stopDetail.getString("name_en");
                    String stopNameZh = stopDetail.getString("name_tc");
                    String lat = stopDetail.getString("lat");
                    String lng = stopDetail.getString("long");

                    // C) Insert into BusStopInformation
                    busStopInfoHelper.insertBusStop(
                            routeNumber,        // route_number
                            boundValue,         // e.g. "O" or "I"
                            stopNameEn,         // start_point or stop_name_en
                            stopNameZh,         // bus_stop_name_ZH
                            seq,                // bus_stop_seq
                            stopId,             // point_stop_id
                            lat,                // bus_stop_lat
                            lng                 // bus_stop_long
                    );
                }

                // Notify on UI thread
                runOnUiThread(() -> {
                    String direction = isOutbound ? "outbound" : "inbound";
                    Toast.makeText(this,
                            "Inserted all " + direction + " stops for route " + routeNumber,
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "fetchAndInsertKmbStops error: " + e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Failed to fetch " + (isOutbound ? "outbound" : "inbound")
                                        + " stops: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
