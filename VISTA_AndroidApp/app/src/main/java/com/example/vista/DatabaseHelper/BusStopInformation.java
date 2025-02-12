package com.example.vista.DatabaseHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;
import android.util.Log;

public class BusStopInformation extends SQLiteOpenHelper {

    private static final String TAG = "BusStopInformation";

    // Database Name and Version
    private static final String DATABASE_NAME = "BusRouteDB.db";
    private static final int DATABASE_VERSION = 3; // Must match or exceed your existing DB version

    // Table Name
    // (Note: You called it "Show BusStop", but for clarity, you might consider "BusStopTable" or similar)
    public static final String TABLE_BUS_STOP = "Show_BusStop";

    // Column Names
    public static final String COLUMN_ID               = "id";
    public static final String COLUMN_ROUTE_NUMBER     = "route_number";
    public static final String COLUMN_BOUND            = "bound";

    // For each individual bus stop:
    // Using "start_point" and "bus_stop_name_ZH" as the English/Chinese name of the stop
    public static final String COLUMN_STOP_NAME_EN     = "start_point";         // or "stop_name_en"
    public static final String COLUMN_STOP_NAME_ZH     = "bus_stop_name_ZH";    // or "stop_name_zh"
    public static final String COLUMN_STOP_SEQ         = "bus_stop_seq";
    public static final String COLUMN_STOP_ID          = "point_stop_id";
    public static final String COLUMN_STOP_LAT         = "bus_stop_lat";
    public static final String COLUMN_STOP_LONG        = "bus_stop_long";

    // SQL to create the table
    private static final String TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_BUS_STOP + " (" +
                    COLUMN_ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ROUTE_NUMBER     + " TEXT, " +
                    COLUMN_BOUND            + " TEXT, " +
                    COLUMN_STOP_NAME_EN     + " TEXT, " +
                    COLUMN_STOP_NAME_ZH     + " TEXT, " +
                    COLUMN_STOP_SEQ         + " TEXT, " +
                    COLUMN_STOP_ID          + " TEXT, " +
                    COLUMN_STOP_LAT         + " TEXT, " +
                    COLUMN_STOP_LONG        + " TEXT" +
                    ");";

    public BusStopInformation(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * If the database already exists (and the version matches), this is *not* called.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(TABLE_CREATE);
            Log.d(TAG, "onCreate: Table '" + TABLE_BUS_STOP + "' created or already exists.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error creating table", e);
        }
    }

    /**
     * Called when the database needs to be upgraded (version number changed).
     * Adjust this logic if you add/remove columns in the future.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            // If you add columns or make structural changes, handle them here.
            // For now, we’ll just ensure the table exists.
            if (oldVersion < 3) {
                // Example: Add missing columns in older versions, etc.
                // db.execSQL("ALTER TABLE " + TABLE_BUS_STOP + " ADD COLUMN ...");
            }
            Log.d(TAG, "onUpgrade: Upgraded table from version " + oldVersion + " to " + newVersion);
        } catch (Exception e) {
            Log.e(TAG, "onUpgrade: Error upgrading database", e);
        }
    }

    /**
     * Inserts a single Bus Stop record for a given route & bound.
     * Typically called for each stop in the route-stop API.
     *
     * @param routeNumber  e.g. "42"
     * @param bound        e.g. "O" (outbound) or "I" (inbound)
     * @param stopNameEn   English name of the stop
     * @param stopNameZh   Chinese name of the stop
     * @param stopSeq      Sequence of the stop on this route
     * @param stopId       Unique stop ID from KMB API
     * @param stopLat      Latitude
     * @param stopLong     Longitude
     *
     * @return Row ID of the newly inserted row, or -1 on error
     */
    public long insertBusStop(String routeNumber,
                              String bound,
                              String stopNameEn,
                              String stopNameZh,
                              String stopSeq,
                              String stopId,
                              String stopLat,
                              String stopLong) {

        long rowId = -1;
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_ROUTE_NUMBER,  routeNumber);
            values.put(COLUMN_BOUND,         bound);
            values.put(COLUMN_STOP_NAME_EN,  stopNameEn);
            values.put(COLUMN_STOP_NAME_ZH,  stopNameZh);
            values.put(COLUMN_STOP_SEQ,      stopSeq);
            values.put(COLUMN_STOP_ID,       stopId);
            values.put(COLUMN_STOP_LAT,      stopLat);
            values.put(COLUMN_STOP_LONG,     stopLong);

            rowId = db.insert(TABLE_BUS_STOP, null, values);
            if (rowId == -1) {
                Log.e(TAG, "insertBusStop: Failed to insert row for stopId=" + stopId);
            } else {
                Log.d(TAG, "insertBusStop: Inserted stopId=" + stopId + " at row " + rowId);
            }

        } catch (Exception e) {
            Log.e(TAG, "insertBusStop: Exception occurred", e);
        }
        // We do NOT close the db here because SQLiteOpenHelper manages it.
        return rowId;
    }

    /**
     * Retrieve all stops for a given route and bound, ordered by sequence.
     *
     * @param routeNumber e.g. "42"
     * @param bound       e.g. "O" or "I"
     * @return Cursor with the result set. Remember to close the cursor when done.
     */
    public Cursor getStopsForRoute(String routeNumber, String bound) {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_ROUTE_NUMBER + " = ? AND " + COLUMN_BOUND + " = ?";
        String[] selectionArgs = {routeNumber, bound};
        String orderBy = COLUMN_STOP_SEQ + " ASC";  // or CAST(...) if needed numerically

        return db.query(
                TABLE_BUS_STOP,        // table name
                null,                  // columns (null = all)
                selection,             // selection (WHERE clause)
                selectionArgs,         // selection args
                null,                  // groupBy
                null,                  // having
                orderBy                // order by stop sequence
        );
    }

    /**
     * Clears all bus stops in the table. Use carefully!
     */
    public void clearBusStopTable() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_BUS_STOP);
            Log.d(TAG, "clearBusStopTable: Table '" + TABLE_BUS_STOP + "' is now empty.");
        } catch (Exception e) {
            Log.e(TAG, "clearBusStopTable: Exception occurred", e);
        }
    }
}
