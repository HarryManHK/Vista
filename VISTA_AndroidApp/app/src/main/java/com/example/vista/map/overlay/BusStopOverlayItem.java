package com.example.vista.map.overlay;

import org.osmdroid.util.GeoPoint;

public class BusStopOverlayItem {
    public String stopId;
    public GeoPoint point;
    public String title;
    public int drawableRes;

    public BusStopOverlayItem(String stopId, GeoPoint point, String title, int drawableRes) {
        this.stopId = stopId;
        this.point = point;
        this.title = title;
        this.drawableRes = drawableRes;
    }
}
