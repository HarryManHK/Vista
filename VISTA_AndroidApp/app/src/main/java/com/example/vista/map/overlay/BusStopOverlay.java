package com.example.vista.map.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import java.util.List;

public class BusStopOverlay extends Overlay {
    private Paint paint;
    private Context context;
    private List<BusStopOverlayItem> busStopItems;

    public BusStopOverlay(Context context, List<BusStopOverlayItem> busStopItems) {
        super();
        this.context = context;
        this.busStopItems = busStopItems;
        this.paint = new Paint();
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;
        for (BusStopOverlayItem item : busStopItems) {
            GeoPoint point = item.point;
            Drawable drawable = ContextCompat.getDrawable(context, item.drawableRes);
            if (drawable == null) continue;
            android.graphics.Point screenPoint = new android.graphics.Point();
            mapView.getProjection().toPixels(point, screenPoint);
            drawable.setBounds(screenPoint.x - 32, screenPoint.y - 32, screenPoint.x + 32, screenPoint.y + 32);
            drawable.draw(canvas);
        }
    }
}
