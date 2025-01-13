package com.example.vista;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
// import java.util.TimeZone; // If needed to force a specific zone

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShowArriveTimePage extends Activity {

    private static final String TAG = "ShowArriveTimePage";

    // Refresh every 5 seconds
    private static final long REFRESH_INTERVAL_MS = 5_000;

    // UI
    private TextView txtShowArriveTimePageCurrentBusStop;
    private ListView lvShowArriveTimePage;
    private Button btnShowArriveTimePageConfirm;
    private Button btnShowArriveTimePageNext;

    // ListView data
    private ArrayList<String> etaList;
    private ArrayAdapter<String> etaAdapter;

    // DB
    private DatabaseHelper dbHelper;

    // Bus route info
    private String routeNumber;  // e.g. "43A"
    private int routeSeq;        // For start point seq (e.g. 1)
    private String start_point;  //The start point (e.g., "x站").

    // Handler for auto-refresh
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Fetch API data and schedule next refresh
            fetchETADataFromKMB(routeNumber, 1);
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_arrive_time_page);

        // Initialize UI components
        txtShowArriveTimePageCurrentBusStop = findViewById(R.id.txtShowArriveTimePageCurrentBusStop);
        lvShowArriveTimePage = findViewById(R.id.lvShowArriveTimePage);
        btnShowArriveTimePageConfirm = findViewById(R.id.btnShowArriveTimePageConfirm);
        btnShowArriveTimePageNext = findViewById(R.id.btnShowArriveTimePageNext);

        // Prepare ListView and its adapter
        etaList = new ArrayList<>();
        etaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etaList);
        lvShowArriveTimePage.setAdapter(etaAdapter);

        // Get the DB instance
        dbHelper = DatabaseHelper.getInstance(this);

        // Retrieve routeNumber and start point seq from DB
        fetchRouteInfoFromDB();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-refresh when Activity is visible
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh when Activity is not visible
        handler.removeCallbacks(refreshRunnable);
    }

    /**
     * Retrieve route number and the start point seq from the DB (COLUMN_START_POINT_SEQ).
     */
    private void fetchRouteInfoFromDB() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                // 1) Route number
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_NUMBER));

                // 2) Start point seq
                String routeSeqString = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT_SEQ));

                // Start point stop name
                start_point = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_START_POINT));
                try {
                    routeSeq = Integer.parseInt(routeSeqString);
                } catch (NumberFormatException e) {
                    routeSeq = 1; // fallback if parse fails
                }

                // Update UI label
                txtShowArriveTimePageCurrentBusStop.setText(
                        "Route: " + routeNumber + ", Current Stop: " + start_point
                );
            } else {
                Toast.makeText(this, "No route info in DB.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching route info from DB", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Fetch ETA data from KMB API using OkHttp, filtering by start point seq.
     *
     * @param routeNumber The bus route number (e.g. "43A").
     * @param serviceType Usually "1" for KMB's standard service.
     */
    private void fetchETADataFromKMB(String routeNumber, int serviceType) {
        if (routeNumber == null || routeNumber.equals("---")) {
            Log.w(TAG, "Invalid routeNumber, skipping fetch.");
            return;
        }

        String url = "https://data.etabus.gov.hk/v1/transport/kmb/route-eta/"
                + routeNumber + "/" + serviceType;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchETADataFromKMB onFailure: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(ShowArriveTimePage.this,
                            "Failed to fetch ETA data.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unsuccessful HTTP response: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ShowArriveTimePage.this,
                                "Error: Unsuccessful API response (" + response.code() + ").",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Read the entire response body
                String responseBody = response.body().string();
                parseAndDisplayETA(responseBody);
            }
        });
    }

    /**
     * Parse JSON response from the KMB API, filter by the 'seq' == start point seq,
     * and display the first 3 ETAs in the ListView.
     */
    private void parseAndDisplayETA(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray dataArray = root.optJSONArray("data");
            if (dataArray == null) {
                Log.w(TAG, "No 'data' array in JSON response.");
                return;
            }

            // Clear old items
            etaList.clear();

            // We only want the first 3 results that match this routeSeq
            int count = 0;
            for (int i = 0; i < dataArray.length(); i++) {
                if (count >= 3) break;

                JSONObject item = dataArray.getJSONObject(i);
                int seqVal = item.optInt("seq", -1); // 'seq' from KMB JSON

                if (seqVal == routeSeq) {
                    String etaString = item.optString("eta", "N/A");
                    String etaDiffString = calculateTimeDifference(etaString);

                    int etaSeq = item.optInt("eta_seq", -1);
                    String displayText = "ETA #" + etaSeq + ": " + etaDiffString;
                    etaList.add(displayText);

                    count++;
                }
            }

            runOnUiThread(() -> {
                etaAdapter.notifyDataSetChanged();

                if (etaList.isEmpty()) {
                    Toast.makeText(ShowArriveTimePage.this,
                            "No matching ETA data for start seq=" + routeSeq,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "parseAndDisplayETA: JSON parse error", e);
        }
    }

    /**
     * Calculate how many minutes/seconds remain until the ETA from now.
     */
    private String calculateTimeDifference(String etaString) {
        try {
            // Example: 2025-01-13T18:25:00+08:00
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            // If needed, explicitly set timezone:
            // TimeZone tz = TimeZone.getTimeZone("Asia/Hong_Kong");
            // sdf.setTimeZone(tz);

            Date etaDate = sdf.parse(etaString);
            if (etaDate == null) {
                return "N/A";
            }

            long now = System.currentTimeMillis();
            long etaMillis = etaDate.getTime();
            long diff = etaMillis - now; // difference in ms

            // If the bus is arriving or has passed
            if (diff <= 0) {
                return "Arriving";
            }

            long diffSeconds = diff / 1000;
            long minutes = diffSeconds / 60;
            long seconds = diffSeconds % 60;

            if (minutes > 0) {
                return minutes + " min(s) " + seconds + " sec(s)";
            } else {
                return seconds + " sec(s)";
            }

        } catch (ParseException e) {
            Log.e(TAG, "calculateTimeDifference: parse error for " + etaString, e);
            return "N/A";
        }
    }
}