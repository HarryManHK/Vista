package com.example.vista.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/*從Bus DatabaseHelper獲取指定路線的資訊
 * https://data.etabus.gov.hk/v1/transport/kmb/route/" + BusRoute + "/outbound/1 中獲取該巴士線的所有站的Stop ID
 * 獲取bus route number(e.g. 42)後，使用以下網址來獲取該站的所有站的stopID
 * 使用for loop loop https://data.etabus.gov.hk/v1/transport/kmb/route-stop/" + BusRoute + "/" + bound.toLowerCase() + "/1 api的stop
 * 在每次loop都使用下面的api以及把它們的經緯度(lat & long),seq number及巴士名（繁中及英）紀錄
 * https://data.etabus.gov.hk/v1/transport/kmb/stop/" + StopID + "

 *
 */

public class BusStopInfomationHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "BusRouteInfoDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "bus_stops";

    // 欄位名稱
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ROUTE_NUMBER = "route_number";
    public static final String COLUMN_BOUND = "bound";
    public static final String COLUMN_BUS_STOP_NAME = "bus_stop_name";
    public static final String COLUMN_BUS_STOP_NAME_ZH = "bus_stop_name_ZH";
    public static final String COLUMN_BUS_STOP_SEQ = "bus_stop_seq";
    public static final String COLUMN_BUS_STOP_ID = "bus_stop_id"; // 新增欄位
    public static final String COLUMN_BUS_STOP_LAT = "bus_stop_lat";
    public static final String COLUMN_BUS_STOP_LONG = "bus_stop_long";

    // 創建資料表的 SQL 語句（包含新欄位）
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ROUTE_NUMBER + " TEXT NOT NULL, " +
            COLUMN_BOUND + " TEXT NOT NULL, " +
            COLUMN_BUS_STOP_NAME + " TEXT NOT NULL, " +
            COLUMN_BUS_STOP_NAME_ZH + " TEXT NOT NULL, " +
            COLUMN_BUS_STOP_SEQ + " INTEGER NOT NULL, " +
            COLUMN_BUS_STOP_ID + " TEXT NOT NULL, " + // 新增 bus_stop_id
            COLUMN_BUS_STOP_LAT + " REAL NOT NULL, " +
            COLUMN_BUS_STOP_LONG + " REAL NOT NULL)";

    public BusStopInfomationHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 非同步獲取並儲存巴士站資訊
    public void fetchAndStoreBusStops(String busRoute, String bound, OnFetchCompleteListener listener) {
        new FetchBusStopsTask(busRoute, bound, listener).execute();
    }

    private class FetchBusStopsTask extends AsyncTask<Void, Void, Boolean> {
        private final String busRoute;
        private final String bound;
        private final OnFetchCompleteListener listener;

        FetchBusStopsTask(String busRoute, String bound, OnFetchCompleteListener listener) {
            this.busRoute = busRoute;
            this.bound = bound;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            String routeStopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/" + busRoute + "/" + bound.toLowerCase() + "/1";
            Request request = new Request.Builder().url(routeStopUrl).build();

            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to fetch route stops: " + response.code());
                    return false;
                }

                String jsonData = response.body().string();
                JSONObject jsonObject = new JSONObject(jsonData);
                JSONArray stopsArray = jsonObject.getJSONArray("data");

                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();

                try {
                    // 清空該路線和方向的舊資料
                    db.delete(TABLE_NAME,
                            COLUMN_ROUTE_NUMBER + "=? AND " + COLUMN_BOUND + "=?",
                            new String[]{busRoute, bound});

                    for (int i = 0; i < stopsArray.length(); i++) {
                        JSONObject stop = stopsArray.getJSONObject(i);
                        String stopId = stop.getString("stop");
                        int seq = stop.getInt("seq");

                        String stopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/stop/" + stopId;
                        Request stopRequest = new Request.Builder().url(stopUrl).build();
                        Response stopResponse = client.newCall(stopRequest).execute();

                        if (stopResponse.isSuccessful()) {
                            JSONObject stopData = new JSONObject(stopResponse.body().string()).getJSONObject("data");
                            String nameEn = stopData.getString("name_en");
                            String nameZh = stopData.getString("name_tc");
                            double lat = stopData.getDouble("lat");
                            double lon = stopData.getDouble("long");

                            ContentValues values = new ContentValues();
                            values.put(COLUMN_ROUTE_NUMBER, busRoute);
                            values.put(COLUMN_BOUND, bound);
                            values.put(COLUMN_BUS_STOP_NAME, nameEn);
                            values.put(COLUMN_BUS_STOP_NAME_ZH, nameZh);
                            values.put(COLUMN_BUS_STOP_SEQ, seq);
                            values.put(COLUMN_BUS_STOP_ID, stopId); // 儲存 API 的 stopID
                            values.put(COLUMN_BUS_STOP_LAT, lat);
                            values.put(COLUMN_BUS_STOP_LONG, lon);

                            db.insert(TABLE_NAME, null, values);
                        } else {
                            Log.e(TAG, "Failed to fetch stop details for ID: " + stopId);
                        }
                        stopResponse.close();
                    }
                    db.setTransactionSuccessful();
                    return true;
                } finally {
                    db.endTransaction();
                    db.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching bus stops: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (listener != null) {
                listener.onFetchComplete(success);
            }
        }
    }

    // 查詢指定路線的所有站點
    public Cursor getAllStopsForRoute(String routeNumber, String bound) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME,
                null, // 所有欄位
                COLUMN_ROUTE_NUMBER + "=? AND " + COLUMN_BOUND + "=?",
                new String[]{routeNumber, bound},
                null, null,
                COLUMN_BUS_STOP_SEQ + " ASC"); // 按順序排序
    }

    // 根據 bus_stop_id 查詢單個站點
    public Cursor getStopById(String busStopId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME,
                null,
                COLUMN_BUS_STOP_ID + "=?",
                new String[]{busStopId},
                null, null, null);
    }

    // 回呼介面
    public interface OnFetchCompleteListener {
        void onFetchComplete(boolean success);
    }

    // Retrieves all records from the bus_stops table
    public Cursor getAllStops() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_NAME, // The table to query
                null,       // Columns (null = all columns)
                null,       // Selection (null = no selection)
                null,       // Selection args (null = no selection)
                null,       // GroupBy
                null,       // Having
                null        // OrderBy
        );
    }
    public Cursor getAllStopsRaw() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }
}