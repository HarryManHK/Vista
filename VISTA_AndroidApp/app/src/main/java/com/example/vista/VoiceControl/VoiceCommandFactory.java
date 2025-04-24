package com.example.vista.VoiceControl;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.example.vista.BusArrivalAlertPage;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.ImageToTextMenu;
import com.example.vista.RealTimeDetectPage;
import com.example.vista.ShowArriveTimePage;

import org.json.JSONException;
import org.json.JSONObject;

public class VoiceCommandFactory {

    private static final String TAG = "VoiceCommandFactory_debug";

    public static void executeCommand(Context context, String nlpResult) {
        try {
            // 解析 DeepSeek 回傳的 JSON
            JSONObject json = new JSONObject(nlpResult);
            String action = json.optString("action", "");

            if ("搭巴士".equals(action)) {
                Command c = new ByBusCommand(context);
                c.execute(json);
                Log.d(TAG, "Executing command: 搭巴士");
            } else if ("查巴士到站時間".equals(action)) {
                Log.d(TAG, "Executing command: 查巴士到站時間");
                // 跳轉到 ShowArriveTimePage
                context.startActivity(new Intent(context, ShowArriveTimePage.class));
            } else if ("檢測巴士站".equals(action)) {
                Log.d(TAG, "Executing command: 檢測巴士站");
                // 跳轉到 BusStopDetectionPage
                context.startActivity(new Intent(context, RealTimeDetectPage.class));
            } else if ("巴士到站提醒".equals(action)) {
                Log.d(TAG, "Executing command: 巴士到站提醒");
                // 跳轉到 BusArrivalReminderPage
                context.startActivity(new Intent(context, BusArrivalAlertPage.class));
            } else if ("圖片生成文字".equals(action)) {
                Log.d(TAG, "Executing command: 圖片生成文字");
                // 跳轉到 ImageToTextPage
                context.startActivity(new Intent(context, ImageToTextMenu.class));
                Log.d(TAG, "Executing command: 查距離");
                // TODO: Replace this with actual distance calculation or navigation if needed
                Toast.makeText(context, "正在查詢距離...", Toast.LENGTH_SHORT).show();
                // Optionally, you could navigate to a dedicated distance page or trigger distance calculation logic here
            } else {
                Log.d(TAG, "No matching command found for action: " + action);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse NLP result JSON", e);
        }
    }
}