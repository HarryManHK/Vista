package com.example.vista.DatabaseHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class SettingDatabaseHelper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "SettingDB.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    public static final String TABLE_LANGUAGE_SETTING = "LanguageSettingTable";

    // Column Names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LANGUAGE_CODE = "language_code";
    public static final String COLUMN_COUNTRY_CODE = "country_code";

    // Create Table SQL Statement
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_LANGUAGE_SETTING + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " + // Fixed ID=1
                    COLUMN_LANGUAGE_CODE + " TEXT NOT NULL, " +
                    COLUMN_COUNTRY_CODE + " TEXT NOT NULL);";

    // Singleton instance
    private static SettingDatabaseHelper instance;

    /**
     * Get the singleton instance of SettingDatabaseHelper
     *
     * @param context Application context
     * @return Singleton instance of SettingDatabaseHelper
     */
    public static synchronized SettingDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SettingDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    // Private constructor to enforce singleton pattern
    private SettingDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // onCreate: Called when the database is created for the first time
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    // onUpgrade: Called when the database needs to be upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LANGUAGE_SETTING);
        // Create tables again
        onCreate(db);
    }

    /**
     * Insert or Update Language Setting
     * If a setting already exists (id=1), it updates it. Otherwise, it inserts a new setting.
     *
     * @param languageCode Language code (e.g., "en", "zh")
     * @param countryCode  Country code (e.g., "US", "HK")
     * @return boolean indicating success or failure
     */
    public boolean insertOrUpdateLanguageSetting(String languageCode, String countryCode) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_LANGUAGE_CODE, languageCode);
        values.put(COLUMN_COUNTRY_CODE, countryCode);

        // Update existing setting with id=1
        int rows = db.update(TABLE_LANGUAGE_SETTING, values, COLUMN_ID + " = ?", new String[]{"1"});

        if (rows == 0) {
            // No existing setting, insert new with id=1
            values.put(COLUMN_ID, 1);
            long result = db.insert(TABLE_LANGUAGE_SETTING, null, values);
            return result != -1;
        } else {
            return rows > 0;
        }
    }

    /**
     * Retrieve Language Setting
     *
     * @return a String array containing language code and country code, or null if not found
     */
    public String[] getLanguageSetting() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LANGUAGE_SETTING + " WHERE " + COLUMN_ID + " = 1", null);

        if (cursor.moveToFirst()) {
            String languageCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANGUAGE_CODE));
            String countryCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_CODE));
            cursor.close();
            db.close();
            return new String[]{languageCode, countryCode};
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }


}