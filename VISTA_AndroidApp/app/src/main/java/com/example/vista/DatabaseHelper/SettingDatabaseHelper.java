package com.example.vista.DatabaseHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class SettingDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "SettingDatabaseHelper_debug";

    // Database Name and Version
    private static final String DATABASE_NAME = "SettingDB.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for schema change

    // Table Names
    public static final String TABLE_LANGUAGE_SETTING = "LanguageSettingTable";
    public static final String TABLE_VOICE_SETTING = "VoiceSettingTable";

    // Language Setting Table Columns
    public static final String COLUMN_LANGUAGE_ID = "id";
    public static final String COLUMN_LANGUAGE_CODE = "language_code";
    public static final String COLUMN_COUNTRY_CODE = "country_code";

    // Voice Setting Table Columns
    public static final String COLUMN_VOICE_ID = "id";
    public static final String COLUMN_VOICE_SPEED = "voice_speed";
    public static final String COLUMN_VOICE_LANGUAGE = "voice_language";
    public static final String COLUMN_VOICE_GENDER = "voice_gender";

    // Create Table SQL Statements
    private static final String TABLE_LANGUAGE_CREATE =
            "CREATE TABLE " + TABLE_LANGUAGE_SETTING + " (" +
                    COLUMN_LANGUAGE_ID + " INTEGER PRIMARY KEY, " + // Fixed ID=1
                    COLUMN_LANGUAGE_CODE + " TEXT NOT NULL, " +
                    COLUMN_COUNTRY_CODE + " TEXT NOT NULL);";

    private static final String TABLE_VOICE_CREATE =
            "CREATE TABLE " + TABLE_VOICE_SETTING + " (" +
                    COLUMN_VOICE_ID + " INTEGER PRIMARY KEY, " + // Fixed ID=1
                    COLUMN_VOICE_SPEED + " REAL NOT NULL, " +
                    COLUMN_VOICE_LANGUAGE + " TEXT NOT NULL, " +
                    COLUMN_VOICE_GENDER + " TEXT NOT NULL);";

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
        db.execSQL(TABLE_LANGUAGE_CREATE);
        db.execSQL(TABLE_VOICE_CREATE);

        // Optionally, insert default settings
        insertDefaultSettings(db);
    }

    // onUpgrade: Called when the database needs to be upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema upgrades
        if (oldVersion < 2) {
            // Create VoiceSettingTable if upgrading from version <2
            db.execSQL(TABLE_VOICE_CREATE);
            // Optionally, insert default voice settings
            insertDefaultVoiceSettings(db);
        }
        // Future upgrades can be handled with additional if statements
    }

    /**
     * Inserts default settings into the LanguageSettingTable and VoiceSettingTable
     * This ensures that there is always a row with id=1 in both tables
     *
     * @param db The writable database
     */
    private void insertDefaultSettings(SQLiteDatabase db) {
        // Insert default Language Settings
        ContentValues langValues = new ContentValues();
        langValues.put(COLUMN_LANGUAGE_ID, 1);
        langValues.put(COLUMN_LANGUAGE_CODE, "en");
        langValues.put(COLUMN_COUNTRY_CODE, "US");
        db.insert(TABLE_LANGUAGE_SETTING, null, langValues);

        // Insert default Voice Settings
        ContentValues voiceValues = new ContentValues();
        voiceValues.put(COLUMN_VOICE_ID, 1);
        voiceValues.put(COLUMN_VOICE_SPEED, 1.0f); // Normal speed
        voiceValues.put(COLUMN_VOICE_LANGUAGE, "en");
        voiceValues.put(COLUMN_VOICE_GENDER, "male");
        db.insert(TABLE_VOICE_SETTING, null, voiceValues);
    }

    /**
     * Inserts default voice settings into the VoiceSettingTable
     * Used during onUpgrade when VoiceSettingTable is newly created
     *
     * @param db The writable database
     */
    private void insertDefaultVoiceSettings(SQLiteDatabase db) {
        ContentValues voiceValues = new ContentValues();
        voiceValues.put(COLUMN_VOICE_ID, 1);
        voiceValues.put(COLUMN_VOICE_SPEED, 1.0f); // Normal speed
        voiceValues.put(COLUMN_VOICE_LANGUAGE, "en");
        voiceValues.put(COLUMN_VOICE_GENDER, "male");
        db.insert(TABLE_VOICE_SETTING, null, voiceValues);
    }

    // ====================== Language Setting Methods ======================

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
        int rows = db.update(TABLE_LANGUAGE_SETTING, values, COLUMN_LANGUAGE_ID + " = ?", new String[]{"1"});

        if (rows == 0) {
            // No existing setting, insert new with id=1
            values.put(COLUMN_LANGUAGE_ID, 1);
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
        Cursor cursor = db.query(
                TABLE_LANGUAGE_SETTING,
                new String[]{COLUMN_LANGUAGE_CODE, COLUMN_COUNTRY_CODE},
                COLUMN_LANGUAGE_ID + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String languageCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANGUAGE_CODE));
            String countryCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_CODE));
            cursor.close();
            db.close();
            return new String[]{languageCode, countryCode};
        } else {
            if (cursor != null) cursor.close();
            db.close();
            return null;
        }
    }

    // ====================== Voice Setting Methods ======================

    /**
     * Set Voice Speed
     *
     * @param speed The desired voice speed. Typically between 0.1f (slow) and 2.0f (fast).
     * @return boolean indicating success or failure
     */
    public boolean setSpeed(float speed) {
        Log.d(TAG, "setSpeed: Attempting to set voice speed to " + speed + "x");

        try {
            // Acquire writable database
            SQLiteDatabase db = this.getWritableDatabase();

            // Prepare the values to update
            ContentValues values = new ContentValues();
            values.put(COLUMN_VOICE_SPEED, speed);

            // Attempt to update the existing voice setting with id=1
            int rowsAffected = db.update(
                    TABLE_VOICE_SETTING,
                    values,
                    COLUMN_VOICE_ID + " = ?",
                    new String[]{"1"}
            );
            Log.d(TAG, "setSpeed: Update rows affected: " + rowsAffected);

            if (rowsAffected == 0) {
                // No existing setting found; attempt to insert a new one
                Log.d(TAG, "setSpeed: No existing setting found. Inserting new setting with id=1");

                // Add default values for other columns
                values.put(COLUMN_VOICE_ID, 1);
                values.put(COLUMN_VOICE_LANGUAGE, "en"); // Default language
                values.put(COLUMN_VOICE_GENDER, "male"); // Default gender

                // Attempt to insert the new setting
                long newRowId = db.insert(TABLE_VOICE_SETTING, null, values);
                if (newRowId != -1) {
                    Log.d(TAG, "setSpeed: Insert successful. New row ID: " + newRowId);
                    return true;
                } else {
                    Log.e(TAG, "setSpeed: Insert failed. Unable to add new voice setting.");
                    return false;
                }
            } else {
                Log.d(TAG, "setSpeed: Update successful for row ID 1.");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "setSpeed: Exception occurred while setting speed.", e);
            return false;
        }
    }

    /**
     * Set Voice Language
     *
     * @param language The desired voice language code (e.g., "en", "zh")
     * @return boolean indicating success or failure
     */
    public boolean setVoiceLanguage(String language) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_VOICE_LANGUAGE, language);

        // Update existing voice setting with id=1
        int rows = db.update(TABLE_VOICE_SETTING, values, COLUMN_VOICE_ID + " = ?", new String[]{"1"});

        if (rows == 0) {
            // No existing setting, insert new with id=1
            values.put(COLUMN_VOICE_ID, 1);
            // Provide default values for other columns if necessary
            values.put(COLUMN_VOICE_SPEED, 1.0f); // Default speed
            values.put(COLUMN_VOICE_GENDER, "male"); // Default gender
            long result = db.insert(TABLE_VOICE_SETTING, null, values);
            return result != -1;
        } else {
            return rows > 0;
        }
    }

    /**
     * Retrieve Voice Language Setting
     *
     * @return The current voice language as a String. Returns "en" as default if not found.
     */
    public String getColumnVoiceLanguage() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String voiceLanguage = "en"; // 預設語言

        try {
            db = this.getReadableDatabase();

            cursor = db.query(
                    TABLE_VOICE_SETTING,                      // 表名
                    new String[]{COLUMN_VOICE_LANGUAGE},       // 要查詢的列
                    COLUMN_VOICE_ID + " = ?",                  // WHERE 子句
                    new String[]{"1"},                         // WHERE 子句的參數
                    null,                                      // GROUP BY
                    null,                                      // HAVING
                    null                                       // ORDER BY
            );

            if (cursor != null && cursor.moveToFirst()) {
                voiceLanguage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOICE_LANGUAGE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving voice language", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return voiceLanguage;
    }

    /**
     * Set Voice Gender
     *
     * @param gender The desired voice gender (e.g., "male", "female")
     * @return boolean indicating success or failure
     */
    public boolean setVoiceGender(String gender) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_VOICE_GENDER, gender);

        // Update existing voice setting with id=1
        int rows = db.update(TABLE_VOICE_SETTING, values, COLUMN_VOICE_ID + " = ?", new String[]{"1"});

        if (rows == 0) {
            // No existing setting, insert new with id=1
            values.put(COLUMN_VOICE_ID, 1);
            // Provide default values for other columns if necessary
            values.put(COLUMN_VOICE_SPEED, 1.0f); // Default speed
            values.put(COLUMN_VOICE_LANGUAGE, "en"); // Default language
            long result = db.insert(TABLE_VOICE_SETTING, null, values);
            return result != -1;
        } else {
            return rows > 0;
        }
    }

    /**
     * Retrieve Voice Speed Setting
     *
     * @return The current voice speed as a float. Returns 1.0f if not found.
     */
    public float getVoiceSpeed() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        float voiceSpeed = 1.0f; // Default speed

        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_VOICE_SETTING,
                    new String[]{COLUMN_VOICE_SPEED},
                    COLUMN_VOICE_ID + " = ?",
                    new String[]{"1"},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                voiceSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_VOICE_SPEED));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving voice speed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return voiceSpeed;
    }

    /**
     * Retrieve Voice Setting
     *
     * @return a VoiceSetting object containing voice speed, language, and gender, or null if not found
     */
    public VoiceSetting getVoiceSetting() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_VOICE_SETTING,
                new String[]{COLUMN_VOICE_SPEED, COLUMN_VOICE_LANGUAGE, COLUMN_VOICE_GENDER},
                COLUMN_VOICE_ID + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            float speed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_VOICE_SPEED));
            String language = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOICE_LANGUAGE));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOICE_GENDER));
            cursor.close();
            db.close();
            return new VoiceSetting(speed, language, gender);
        } else {
            if (cursor != null) cursor.close();
            db.close();
            return null;
        }
    }

    /**
     * Inner class to represent Voice Settings
     */
    public static class VoiceSetting {
        private float speed;
        private String language;
        private String gender;

        public VoiceSetting(float speed, String language, String gender) {
            this.speed = speed;
            this.language = language;
            this.gender = gender;
        }

        public float getSpeed() {
            return speed;
        }

        public String getLanguage() {
            return language;
        }

        public String getGender() {
            return gender;
        }
    }
}