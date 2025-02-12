package com.example.vista.DatabaseHelper;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*從Bus DatabaseHelper獲取指定路線的資訊
* https://data.etabus.gov.hk/v1/transport/kmb/route/" + BusRoute + "/outbound/1 中獲取該巴士線的所有站的Stop ID
* 獲取bus route number(e.g. 42)後，使用以下網址來獲取該站的所有站的stopID
* 使用for loop loop https://data.etabus.gov.hk/v1/transport/kmb/route/" + BusRoute + "/outbound/1 api的stop
* 在每次loop都使用下面的api以及把它們的經緯度(lat & long),seq number及巴士名（繁中及英）紀錄
* https://data.etabus.gov.hk/v1/transport/kmb/stop/" + StopID + "
* 有這些資訊後再在BusArrivalAlertPage中顯示所有目的地的名字
*
 */
public class BusStopInfomation{

    private static final String TAG = "DatabaseHelper";

    // Database Name and Version
    private static final String DATABASE_NAME = "BusRouteDB.db";
    private static final int DATABASE_VERSION = 3; // Incremented version to handle schema changes

    // Table Name
    public static final String TABLE_BUS_ROUTE = "BusStop";

    // Column Names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ROUTE_NUMBER = "route_number";
//    public static final String COLUMN_TO = "to_station";
//    public static final String COLUMN_TO_ZH = "to_station_ZH";
    public static final String COLUMN_BOUND = "bound";
    public static final String COLUMN_START_POINT = "start_point";
    public static final String COLUMN_START_POINT_ZH = "bus_stop_name_ZH";
    public static final String COLUMN_START_POINT_SEQ = "bus_stop_seq";
    public static final String COLUMN_START_POINT_STOP_ID = "point_stop_id";
    public static final String COLUMN_START_POINT_LAT = "bus_stop_lat";
    public static final String COLUMN_START_POINT_LONG = "bus_stop_long";
//    public static final String COLUMN_DESTINATION = "destination";
//    public static final String COLUMN_DESTINATION_ZH = "destination_ZH";
//    public static final String COLUMN_DESTINATION_STOP_ID = "destination_stop_id";
//    public static final String COLUMN_DESTINATION_SEQ = "destination_seq";

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
                    COLUMN_START_POINT_LONG + " TEXT, " +
                    ");";

//    @Override
//    public void onCreate(SQLiteDatabase sqLiteDatabase) {
//
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
//
//    }
}
