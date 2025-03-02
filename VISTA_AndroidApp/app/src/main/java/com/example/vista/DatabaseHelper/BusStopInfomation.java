package com.example.vista.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BusStopInfomation extends SQLiteOpenHelper {
    private static final String TAG = "BusStopInfomation";

    // Database Name and Version
    private static final String DATABASE_NAME = "BusRouteDB.db";
    private static final int DATABASE_VERSION = 3;

    // Table Name
    public static final String TABLE_BUS_ROUTE = "ShowBusStop";

    // Column Names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ROUTE_NUMBER = "route_number";
    public static final String COLUMN_BOUND = "bound";
    public static final String COLUMN_START_POINT = "start_point"; // English name
    public static final String COLUMN_START_POINT_ZH = "bus_stop_name_ZH"; // Chinese name
    public static final String COLUMN_START_POINT_SEQ = "bus_stop_seq";
    public static final String COLUMN_START_POINT_STOP_ID = "point_stop_id";
    public static final String COLUMN_START_POINT_LAT = "bus_stop_lat";
    public static final String COLUMN_START_POINT_LONG = "bus_stop_long";

    private static final String TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_BUS_ROUTE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ROUTE_NUMBER + " TEXT, " +
                    COLUMN_BOUND + " TEXT, " +
                    COLUMN_START_POINT + " TEXT, " +
                    COLUMN_START_POINT_ZH + " TEXT, " +
                    COLUMN_START_POINT_SEQ + " TEXT, " +
                    COLUMN_START_POINT_STOP_ID + " TEXT, " +
                    COLUMN_START_POINT_LAT + " TEXT, " +
                    COLUMN_START_POINT_LONG + " TEXT" +
                    ");";

    // Singleton instance
    private static BusStopInfomation instance;

    private BusStopInfomation(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized BusStopInfomation getInstance(Context context) {
        if (instance == null) {
            instance = new BusStopInfomation(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.d(TAG, "onCreate: Bus stop table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, we'll drop and recreate the table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUS_ROUTE);
        onCreate(db);
        Log.d(TAG, "onUpgrade: Table recreated for version " + newVersion);
    }

    /**
     * Fetch and store all bus stops for a given route and bound
     * @param routeNumber The bus route number (e.g., "42")
     * @param bound The direction ("outbound" or "inbound")
     */
    public void fetchAndStoreBusStops(String routeNumber, String bound) {
        new Thread(() -> {
            try {
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();

                // Clear existing data for this route and bound
                db.delete(TABLE_BUS_ROUTE,
                        COLUMN_ROUTE_NUMBER + " = ? AND " + COLUMN_BOUND + " = ?",
                        new String[]{routeNumber, bound});

                // Get all route stops
                String routeStopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/" +
                        routeNumber + "/" + bound.toLowerCase() + "/1";
                String routeStopJson = fetchJsonFromUrl(routeStopUrl);

                if (routeStopJson != null) {
                    JSONObject jsonObject = new JSONObject(routeStopJson);
                    JSONArray stopsArray = jsonObject.getJSONArray("data");

                    for (int i = 0; i < stopsArray.length(); i++) {
                        JSONObject stopObj = stopsArray.getJSONObject(i);
                        String stopId = stopObj.getString("stop");
                        String seq = stopObj.getString("seq");

                        // Get detailed stop information
                        String stopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/stop/" + stopId;
                        String stopJson = fetchJsonFromUrl(stopUrl);

                        if (stopJson != null) {
                            JSONObject stopData = new JSONObject(stopJson).getJSONObject("data");

                            ContentValues values = new ContentValues();
                            values.put(COLUMN_ROUTE_NUMBER, routeNumber);
                            values.put(COLUMN_BOUND, bound);
                            values.put(COLUMN_START_POINT, stopData.getString("name_en"));
                            values.put(COLUMN_START_POINT_ZH, stopData.getString("name_tc"));
                            values.put(COLUMN_START_POINT_SEQ, seq);
                            values.put(COLUMN_START_POINT_STOP_ID, stopId);
                            values.put(COLUMN_START_POINT_LAT, stopData.getString("lat"));
                            values.put(COLUMN_START_POINT_LONG, stopData.getString("long"));

                            db.insert(TABLE_BUS_ROUTE, null, values);
                        }
                    }

                    db.setTransactionSuccessful();
                    Log.d(TAG, "Successfully stored stops for route " + routeNumber + " " + bound);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching/storing bus stops: ", e);
            } finally {
                SQLiteDatabase db = getWritableDatabase();
                db.endTransaction();
                db.close();
            }
        }).start();
    }

    /**
     * Helper method to fetch JSON from a URL
     */
    private String fetchJsonFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            conn.disconnect();
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching JSON from " + urlString, e);
            return null;
        }
    }

    /**
     * Clear all bus stop entries for a specific route and bound
     */
    public void clearBusStops(String routeNumber, String bound) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TABLE_BUS_ROUTE,
                    COLUMN_ROUTE_NUMBER + " = ? AND " + COLUMN_BOUND + " = ?",
                    new String[]{routeNumber, bound});
            Log.d(TAG, "Cleared bus stops for route " + routeNumber + " " + bound);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing bus stops: ", e);
        } finally {
            db.close();
        }
    }

    /**
     * Retrieve all stops for a specific route and bound
     * @param routeNumber The bus route number
     * @param bound The direction ("outbound" or "inbound")
     * @return Cursor containing all stops or null if none found
     */
    public Cursor getStopsForRoute(String routeNumber, String bound) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_BUS_ROUTE +
                    " WHERE " + COLUMN_ROUTE_NUMBER + " = ? AND " +
                    COLUMN_BOUND + " = ?" +
                    " ORDER BY " + COLUMN_START_POINT_SEQ + " ASC";
            cursor = db.rawQuery(query, new String[]{routeNumber, bound});

            if (cursor != null && cursor.getCount() > 0) {
                Log.d(TAG, "Retrieved " + cursor.getCount() + " stops for route " + routeNumber + " " + bound);
                return cursor;
            } else {
                Log.d(TAG, "No stops found for route " + routeNumber + " " + bound);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving stops: ", e);
            return null;
        }
        // Note: Don't close db here as it would close the cursor; caller must close the cursor
    }


}

