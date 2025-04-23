package com.example.vista.VoiceControl;

import android.content.Context;
import android.content.Intent;
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
        // 直接跳轉到 VoiceControlBusRoute 頁面
        Intent intent = new Intent(context, com.example.vista.VoiceControl.ByBus.VoiceControlBusRoute.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
