package com.example.vista.repository;

import android.content.Context;
import android.database.Cursor;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.BusStopInfomationHelper;

public class BusRouteRepository {
    private final BusDatabaseHelper busDBHelper;
    private final BusStopInfomationHelper busStopInfoHelper;

    public BusRouteRepository(Context context) {
        this.busDBHelper = BusDatabaseHelper.getInstance(context);
        this.busStopInfoHelper = new BusStopInfomationHelper(context);
    }

    public Cursor getLatestBusRoute() {
        return busDBHelper.getLatestBusRoute();
    }

    public Cursor getAllStopsRaw() {
        return busStopInfoHelper.getAllStopsRaw();
    }

    public Cursor getAllStopsForRoute(String routeNumber, String routeBound) {
        return busStopInfoHelper.getAllStopsForRoute(routeNumber, routeBound);
    }

    public void fetchAndStoreBusStops(String routeNumber, String routeBound, BusStopInfomationHelper.OnFetchCompleteListener listener) {
        busStopInfoHelper.fetchAndStoreBusStops(routeNumber, routeBound, listener);
    }
}
