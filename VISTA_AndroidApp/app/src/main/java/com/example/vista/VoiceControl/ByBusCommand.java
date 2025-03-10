package com.example.vista.VoiceControl;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.VoiceControlPage;

import org.json.JSONObject;

public class ByBusCommand implements Command{
    private String TAG = "ByBusCommand_debug";
    private Context context;
    public ByBusCommand(Context context){
        this.context = context;
    }

    public void execute(JSONObject json){
        // 如果 json 為 null，則在 tvResult 顯示提示訊息
        if (json == null) {
            if (context instanceof VoiceControlPage) {
                ((VoiceControlPage) context).updateResult("請說出搭巴士指令，例如：搭巴士 42A 從 荃灣 到 佐敦");
            }
            return;
        }

        // 解析搭巴士相關資訊
        String routeNumber = json.optString("routeNumber", "");
        String startPoint = json.optString("startPoint", "");
        String destination = json.optString("destination", "");
        Log.d(TAG, "Executing command: 搭巴士, routeNumber=" + routeNumber +
                ", startPoint=" + startPoint + ", destination=" + destination);

        BusDatabaseHelper dbHelper = BusDatabaseHelper.getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.insertOrUpdateBusRoute(db,
                routeNumber,
                destination, destination, "Outbound",
                startPoint, startPoint,
                "", "", "", "",
                destination, destination,
                "", "", "", "");
    }
}
