package com.example.vista;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;
import android.util.Log;

/**
 * DatabaseHelper: Manages SQLite database creation and version management.
 * Implements the Singleton pattern to ensure a single instance throughout the app.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database Name and Version
    private static final String DATABASE_NAME = "BusRouteDB.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    public static final String TABLE_BUS_ROUTE = "BusRoute";

    // Column Names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ROUTE_NUMBER = "route_number";
    public static final String COLUMN_TO = "to_station";
    public static final String COLUMN_START_POINT = "start_point";
    public static final String COLUMN_DESTINATION = "destination";

    // Create Table SQL Statement with IF NOT EXISTS
    private static final String TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_BUS_ROUTE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ROUTE_NUMBER + " TEXT, " +
                    COLUMN_TO + " TEXT, " +
                    COLUMN_START_POINT + " TEXT, " +
                    COLUMN_DESTINATION + " TEXT" +
                    ");";

    // Singleton instance
    private static DatabaseHelper instance;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param context The application context.
     */
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper: Constructor called");
    }

    /**
     * Get the singleton instance of DatabaseHelper.
     *
     * @param context The application context.
     * @return The singleton instance of DatabaseHelper.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
            Log.d(TAG, "getInstance: DatabaseHelper instance created");
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(TABLE_CREATE);
            Log.d(TAG, "onCreate: Database table created");

            // Insert initial bus route data using the provided db instance
            insertOrUpdateBusRoute(db, "---", "---", "---", "---");
            Log.d(TAG, "onCreate: Initial bus route inserted");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For simplicity, drop the existing table and create a new one on upgrade
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUS_ROUTE);
            Log.d(TAG, "onUpgrade: Dropped existing table");

            onCreate(db);
            Log.d(TAG, "onUpgrade: Recreated database table");
        } catch (Exception e) {
            Log.e(TAG, "onUpgrade: Error upgrading database", e);
        }
    }

    /**
     * Insert or Update a bus route in the database.
     * If a route exists, it updates the existing record; otherwise, it inserts a new one.
     *
     * @param db          The SQLiteDatabase instance to use.
     * @param routeNumber The route number (e.g., "43A").
     * @param toStation   The destination station (e.g., "佐敦(西九龍站)").
     * @param startPoint  The start point (e.g., "2站").
     * @param destination The destination point (e.g., "2站").
     * @return The row ID of the newly inserted or updated row, or -1 if an error occurred.
     */
    public long insertOrUpdateBusRoute(SQLiteDatabase db, String routeNumber, String toStation, String startPoint, String destination) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUTE_NUMBER, routeNumber);
        values.put(COLUMN_TO, toStation);
        values.put(COLUMN_START_POINT, startPoint);
        values.put(COLUMN_DESTINATION, destination);

        long rowId = -1;
        try {
            // Assuming there's only one record, fetch its ID
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_BUS_ROUTE + " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                cursor.close();

                // Update the existing record
                int rowsAffected = db.update(TABLE_BUS_ROUTE, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    Log.d(TAG, "insertOrUpdateBusRoute: Updated existing bus route with ID " + id);
                    rowId = id;
                } else {
                    Log.e(TAG, "insertOrUpdateBusRoute: Failed to update existing bus route");
                }
            } else {
                // Insert a new record since none exists
                if (cursor != null) {
                    cursor.close();
                }
                rowId = db.insert(TABLE_BUS_ROUTE, null, values);
                if (rowId == -1) {
                    Log.e(TAG, "insertOrUpdateBusRoute: Failed to insert new bus route");
                } else {
                    Log.d(TAG, "insertOrUpdateBusRoute: Inserted new bus route with ID " + rowId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "insertOrUpdateBusRoute: Exception occurred", e);
        }

        return rowId;
    }

    /**
     * Insert a new bus route into the database.
     * This method uses getWritableDatabase(), so it should not be called within onCreate() to avoid recursion.
     *
     * @param routeNumber The route number (e.g., "43A").
     * @param toStation   The destination station (e.g., "佐敦(西九龍站)").
     * @param startPoint  The start point (e.g., "2站").
     * @param destination The destination point (e.g., "2站").
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertBusRoute(String routeNumber, String toStation, String startPoint, String destination) {
        SQLiteDatabase db = null;
        long newRowId = -1;
        try {
            db = this.getWritableDatabase();
            newRowId = insertOrUpdateBusRoute(db, routeNumber, toStation, startPoint, destination);
        } catch (Exception e) {
            Log.e(TAG, "insertBusRoute: Exception occurred while getting writable database", e);
        }
        // Do not close the database here; SQLiteOpenHelper manages the database lifecycle
        return newRowId;
    }

    /**
     * Clear all entries from the BusRoute table.
     *
     * @param db The SQLiteDatabase instance to use.
     */
    public void clearBusRouteTable(SQLiteDatabase db) {
        try {
            db.execSQL("DELETE FROM " + TABLE_BUS_ROUTE + ";");
            Log.d(TAG, "clearBusRouteTable: All entries from BusRoute table have been deleted");
        } catch (Exception e) {
            Log.e(TAG, "clearBusRouteTable: Exception occurred while deleting data", e);
        }
    }

    /**
     * Retrieve the latest bus route from the database.
     *
     * @return A Cursor pointing to the latest bus route, or null if no data exists.
     */
    public Cursor getLatestBusRoute() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_BUS_ROUTE + " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            if (cursor != null) {
                Log.d(TAG, "getLatestBusRoute: Retrieved latest bus route");
                // No need to moveToFirst() here; the calling method handles it
            } else {
                Log.d(TAG, "getLatestBusRoute: No data found");
            }
        } catch (Exception e) {
            Log.e(TAG, "getLatestBusRoute: Exception occurred while retrieving data", e);
        }
        return cursor;
    }
}