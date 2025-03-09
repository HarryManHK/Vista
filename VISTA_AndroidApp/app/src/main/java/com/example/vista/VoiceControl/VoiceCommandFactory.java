package com.example.vista.VoiceControl;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.ShowArriveTimePage;

import org.json.JSONException;
import org.json.JSONObject;

public class VoiceCommandFactory {

    private static final String TAG = "VoiceCommandFactory";

    public static void executeCommand(Context context, String nlpResult) {
        try {
            // 解析 DeepSeek 回傳的 JSON
            JSONObject json = new JSONObject(nlpResult);
            String action = json.optString("action", "");

            if ("搭巴士".equals(action)) {
                // 解析搭巴士相關資訊
                String routeNumber = json.optString("routeNumber", "");
                String startPoint = json.optString("startPoint", "");
                String destination = json.optString("destination", "");
                Log.d(TAG, "Executing command: 搭巴士, routeNumber=" + routeNumber +
                        ", startPoint=" + startPoint + ", destination=" + destination);

                // 更新 BusDatabase
                BusDatabaseHelper dbHelper = BusDatabaseHelper.getInstance(context);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                dbHelper.insertOrUpdateBusRoute(db,
                        routeNumber,
                        destination, destination, "Outbound",
                        startPoint, startPoint,
                        "", "", "", "",
                        destination, destination,
                        "", "", "", "");
            } else if ("查巴士到站時間".equals(action)) {
                Log.d(TAG, "Executing command: 查巴士到站時間");
                // 跳轉到 ShowArriveTimePage
                context.startActivity(new Intent(context, ShowArriveTimePage.class));
            } else {
                Log.d(TAG, "No matching command found for action: " + action);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse NLP result JSON", e);
        }
    }
}