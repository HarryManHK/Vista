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

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
// import java.util.TimeZone; // If you want to force "Asia/Hong_Kong"

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShowArriveTimePage extends Activity {

    private static final String TAG = "ShowArriveTimePage";
    // Refresh interval: 5 seconds
    private static final long REFRESH_INTERVAL_MS = 5_000;

    // UI references
    private TextView txtShowArriveTimePageCurrentBusStop;
    private ListView lvShowArriveTimePage;
    private Button btnShowArriveTimePageConfirm;
    private Button btnShowArriveTimePageNext;

    // Data for ListView
    private ArrayList<String> etaList;
    private ArrayAdapter<String> etaAdapter;

    // Database
    private BusDatabaseHelper dbHelper;

    // Fields for route data
    private String routeNumber;      // e.g. "43A"
    private int routeSeq;            // e.g. 1
    private String start_point;      // e.g. "x站"
    private String start_point_ZH;   // e.g. "x站" Chinese
    private String boundValFromDB;   // "I" or "O" (derived from DB "inbound"/"outbound")

    // Retrieve language setting (from database)
    private String[] languageSetting = SettingDatabaseHelper.getInstance(this).getLanguageSetting();
    private String languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"

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

        // Initialize UI
        txtShowArriveTimePageCurrentBusStop = findViewById(R.id.txtShowArriveTimePageCurrentBusStop);
        lvShowArriveTimePage = findViewById(R.id.lvShowArriveTimePage);
        btnShowArriveTimePageConfirm = findViewById(R.id.btnShowArriveTimePageConfirm);
        btnShowArriveTimePageNext = findViewById(R.id.btnShowArriveTimePageNext);

        // Prepare ListView adapter
        etaList = new ArrayList<>();
        etaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etaList);
        lvShowArriveTimePage.setAdapter(etaAdapter);

        // Get DB instance
        dbHelper = BusDatabaseHelper.getInstance(this);

        // Retrieve route, seq, bound from DB
        fetchRouteInfoFromDB();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-refresh when activity is visible
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh
        handler.removeCallbacks(refreshRunnable);
    }

    /**
     * Reads the latest bus route from the DB, including bound ("inbound"/"outbound") and start_point_seq.
     */
    private void fetchRouteInfoFromDB() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String routeSeqString = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_SEQ));
                String dbBound = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
                start_point = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT));
                start_point_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_ZH));

                // Convert seq string to integer
                try {
                    routeSeq = Integer.parseInt(routeSeqString);
                } catch (NumberFormatException e) {
                    routeSeq = 1; // fallback
                }

                // Convert DB "inbound"/"outbound" to "I"/"O" for comparison with KMB JSON
                if (dbBound != null) {
                    if (dbBound.equalsIgnoreCase("inbound")) {
                        boundValFromDB = "I";
                    } else if (dbBound.equalsIgnoreCase("outbound")) {
                        boundValFromDB = "O";
                    } else {
                        // fallback if some unknown value
                        boundValFromDB = "O";
                    }
                } else {
                    boundValFromDB = "O"; // default
                }

                // Update UI label
                // e.g., "Route: 43A, Current Stop: x站 (I)"
                // so user can see it's inbound or outbound in short form
                if (languageCode.equals("en")){
                    txtShowArriveTimePageCurrentBusStop.setText(
                            "Route: " + routeNumber
                                    + ", Current Stop: " + start_point
                    );
                }else if(languageCode.equals("zh")){
                    txtShowArriveTimePageCurrentBusStop.setText(
                            "路線: " + routeNumber
                                    + ", 當前巴士站: " + start_point_ZH
                    );
                }
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
     * Fetch ETA data from KMB API using OkHttp, filtering by routeSeq and bound ("I"/"O").
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

                String responseBody = response.body().string();
                parseAndDisplayETA(responseBody);
            }
        });
    }

    /**
     * Parse JSON from KMB, filter by "seq" == routeSeq and "dir" == boundValFromDB ("I"/"O"),
     * display first 3 arrivals in the ListView.
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

            int count = 0;
            for (int i = 0; i < dataArray.length(); i++) {
                if (count >= 3) break;

                JSONObject item = dataArray.getJSONObject(i);

                // "dir" from KMB JSON is "I" or "O"
                String dirFromJson = item.optString("dir", "");
                int seqVal = item.optInt("seq", -1);

                // Must match both seq and dir
                if (seqVal == routeSeq && dirFromJson.equalsIgnoreCase(boundValFromDB)) {
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
                            "No matching ETA data (seq=" + routeSeq + ", dir=" + boundValFromDB + ")",
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "parseAndDisplayETA: JSON parse error", e);
        }
    }

    /**
     * Calculate how many minutes/seconds remain from now until the ETA.
     */
    private String calculateTimeDifference(String etaString) {
        try {
            // Sample format: 2025-01-13T18:25:00+08:00
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

            Date etaDate = sdf.parse(etaString);
            if (etaDate == null) {
                return getServiceUnavailableMessage();
            }

            long now = System.currentTimeMillis();
            long etaMillis = etaDate.getTime();
            long diff = etaMillis - now; // difference in ms

            if (diff <= 0) {
                return getArrivalMessage();
            }

            long diffSeconds = diff / 1000;
            long minutes = diffSeconds / 60;
            long seconds = diffSeconds % 60;

            if (minutes > 0) {
                return formatTime(minutes, seconds);
            } else {
                return formatSeconds(seconds);
            }

        } catch (ParseException e) {
            Log.e(TAG, "calculateTimeDifference: parse error for " + etaString, e);
            return "N/A";
        }
    }

    private String getServiceUnavailableMessage() {
        if ("zh".equals(languageCode)) {
            return "當前沒有服務";
        }
        return "N/A";  // Default to "N/A" in other cases (English or other language codes)
    }

    private String getArrivalMessage() {
        if ("zh".equals(languageCode)) {
            return "即將到達";
        }
        return "Arriving";  // Default to "Arriving" for English or other language codes
    }

    private String formatTime(long minutes, long seconds) {
        if ("zh".equals(languageCode)) {
            return minutes + " 分 " + seconds + " 秒";  // Chinese format
        }
        return minutes + " min(s) " + seconds + " sec(s)";  // English format
    }

    private String formatSeconds(long seconds) {
        if ("zh".equals(languageCode)) {
            return seconds + " 秒";  // Chinese format
        }
        return seconds + " sec(s)";  // English format
    }
}