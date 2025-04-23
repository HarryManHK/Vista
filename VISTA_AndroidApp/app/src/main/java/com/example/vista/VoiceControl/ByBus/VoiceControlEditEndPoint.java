package com.example.vista.VoiceControl.ByBus;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.BusStopListViewAdapter;
import com.example.vista.DatabaseHelper.BusDatabaseHelper;
import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.FindBusStopMenuPage;
import com.example.vista.R;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.example.vista.VoiceControl.NLPCallback;
import com.example.vista.VoiceControl.siliconflow_Deepseek_NLP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class VoiceControlEditEndPoint extends AppCompatActivity {

    private com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.DownloadTask task = null;
    private ListView lvShowAllStop;
    private ArrayList<com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop> busStops; // Custom class to hold bus stop details
    private ArrayAdapter<String> adapter;
    private BusDatabaseHelper dbHelper;
    private SettingDatabaseHelper SettingDBHelper;
    private String BusRoute;
    private String bound; // "inbound" or "outbound"
    private String TAG = "EditDestinationActivity";
    private int selectedPosition = -1; // Track the selected item in the ListView
    // Retrieve language setting (from database)
    private String[] languageSetting;
    private String languageCode; // Default to "en"
    private CustomTextToSpeech tts;
    private static final int REQ_CODE_SPEECH_INPUT = 3002;
    private siliconflow_Deepseek_NLP nlpService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control_edit_end_point);

        // Handle Edge-to-Edge UI if needed
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ListView
        lvShowAllStop = findViewById(R.id.lvShowAllStop);
        busStops = new ArrayList<>();

        // Initialize DatabaseHelpers and TTS
        dbHelper = BusDatabaseHelper.getInstance(this);
        SettingDBHelper = SettingDatabaseHelper.getInstance(this);
        tts = new CustomTextToSpeech(this);
        nlpService = new siliconflow_Deepseek_NLP();

        // Load bus route data
        getBusRouteData();

        // Set up the ListView's click listener to handle selection
        lvShowAllStop.setOnItemClickListener((parent, view, position, id) -> {
            selectItem(position, view);
            com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop selectedBusStop = busStops.get(position);
            tts.speak(new String[]{selectedBusStop.getNameEn(), selectedBusStop.getNameZH()});
        });

        // Set up the "Confirm" button
        Button btnConfirm = findViewById(R.id.btnEditDestinationActivityConfirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != -1) {
                // Get the selected bus stop
                com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop selectedBusStop = busStops.get(selectedPosition);
                updateDatabase(selectedBusStop);
                tts.speak(new String[]{"You've chosen " + selectedBusStop.getNameEn() + " as destination.","已選目的地是" + selectedBusStop.getNameZH()});
                // Show user selected option
                String displayName = languageCode.equals("en") ? selectedBusStop.getNameEn() : selectedBusStop.getNameZH();
                Toast.makeText(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.this, "Selected: " + displayName, Toast.LENGTH_SHORT).show();
                // Navigate to find bus stop menu
                Intent intent = new Intent(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.this, FindBusStopMenuPage.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.this, "Please select a destination bus stop", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the "Next" button
        Button btnNext = findViewById(R.id.btnEditDestinationActivityNext);
        btnNext.setOnClickListener(v -> {
            selectNextItem();
        });

        // Start the DownloadTask to fetch bus stops
        if (BusRoute != null && !BusRoute.isEmpty() && bound != null && !bound.isEmpty()) {
            if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
                task = new com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.DownloadTask();
                // Convert bound to lowercase as API expects "inbound" or "outbound"
                task.execute("https://data.etabus.gov.hk/v1/transport/kmb/route-stop/" + BusRoute + "/" + bound.toLowerCase() + "/1");
            }
        } else {
            Toast.makeText(this, "Bus route or bound data not available", Toast.LENGTH_SHORT).show();
        }

        // (移除 postDelayed，改由 DownloadTask.onPostExecute 負責語音啟動)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
    }

    /**
     * Handles the selection of a ListView item.
     *
     * @param position The position of the item clicked.
     * @param view     The view of the item clicked.
     */
    private void selectItem(int position, View view) {
        selectedPosition = position;
        ((BusStopListViewAdapter) adapter).setSelectedPosition(position);
        view.playSoundEffect(SoundEffectConstants.CLICK);
        com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop selectedBusStop = busStops.get(position);
        tts.speak(new String[]{selectedBusStop.getNameEn(), selectedBusStop.getNameZH()});
    }

    /**
     * Selects the next item in the ListView. Cycles back to the first item if at the end.
     */
    private void selectNextItem() {
        if (busStops == null || busStops.isEmpty()) {
            Toast.makeText(this, "No items to select", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate next position
        int nextPosition = (selectedPosition == -1)
                ? 0
                : (selectedPosition + 1) % busStops.size();

        // Update selection
        ((BusStopListViewAdapter) adapter).setSelectedPosition(nextPosition);
        selectedPosition = nextPosition;

        // Scroll to position
        lvShowAllStop.smoothScrollToPosition(nextPosition);

        // Play sound if visible
        int firstVisible = lvShowAllStop.getFirstVisiblePosition();
        int lastVisible = lvShowAllStop.getLastVisiblePosition();

        if (nextPosition >= firstVisible && nextPosition <= lastVisible) {
            View visibleItem = lvShowAllStop.getChildAt(nextPosition - firstVisible);
            if (visibleItem != null) {
                visibleItem.playSoundEffect(SoundEffectConstants.CLICK);
            }
        }
        // Speak selected destination bus stop
        com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop selectedBusStop = busStops.get(selectedPosition);
        tts.speak(new String[]{selectedBusStop.getNameEn(), selectedBusStop.getNameZH()});
    }

    /**
     * AsyncTask to download bus stops data from the API.
     */
    private class DownloadTask extends AsyncTask<String, Void, ArrayList<com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop>> {
        @Override
        protected ArrayList<com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop> doInBackground(String... values) {
            String apiUrl = values[0];
            ArrayList<com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop> stopsList = new ArrayList<>();
            try {
                // Fetch route-stop data
                URL url = new URL(apiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                // Check response code
                int responseCode = con.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "DownloadTask: HTTP error code " + responseCode);
                    return null;
                }

                InputStream inputStream = con.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder routeStopBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    routeStopBuilder.append(line);
                }
                inputStream.close();
                String routeStopJson = routeStopBuilder.toString();

                // Parse the route-stop JSON
                JSONObject jsonObject = new JSONObject(routeStopJson);
                JSONArray dataArray = jsonObject.getJSONArray("data"); // "data" is a JSONArray

                // Iterate through each stop in the data array
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject stopObj = dataArray.getJSONObject(i);
                    String stopId = stopObj.getString("stop");
                    String seqStr = stopObj.getString("seq");
                    int seq = Integer.parseInt(seqStr);

                    // Fetch stop details
                    String stopDetailsApi = "https://data.etabus.gov.hk/v1/transport/kmb/stop/" + stopId;
                    URL stopUrl = new URL(stopDetailsApi);
                    HttpURLConnection stopCon = (HttpURLConnection) stopUrl.openConnection();
                    stopCon.setRequestMethod("GET");
                    stopCon.connect();

                    int stopResponseCode = stopCon.getResponseCode();
                    if (stopResponseCode != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "DownloadTask: HTTP error code for stop " + stopId + " is " + stopResponseCode);
                        continue; // Skip this stop
                    }

                    InputStream stopInputStream = stopCon.getInputStream();
                    BufferedReader stopBufferedReader = new BufferedReader(new InputStreamReader(stopInputStream));
                    StringBuilder stopBuilder = new StringBuilder();
                    String stopLine;
                    while ((stopLine = stopBufferedReader.readLine()) != null) {
                        stopBuilder.append(stopLine);
                    }
                    stopInputStream.close();
                    String stopJson = stopBuilder.toString();

                    // Parse the stop details JSON
                    JSONObject stopJsonObj = new JSONObject(stopJson);
                    JSONObject stopData = stopJsonObj.getJSONObject("data");

                    String nameEn = stopData.getString("name_en");
                    String nameZH = stopData.getString("name_tc");
                    String lat = stopData.getString("lat");
                    String lon = stopData.getString("long"); // Note: "long" is a reserved word, better use "lon"

                    // Create a BusStop object
                    com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop busStop = new com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop(stopId, nameEn, nameZH, seq, lat, lon);
                    stopsList.add(busStop);
                }
            } catch (Exception e) {
                Log.e(TAG, "DownloadTask Exception: " + e.toString());
                return null;
            }
            return stopsList;
        }

        @Override
        protected void onPostExecute(ArrayList<com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop> result) {
            if (result == null) {
                Toast.makeText(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.this, "Failed to fetch bus stops data", Toast.LENGTH_SHORT).show();
                return;
            }

            busStops = result; // Assign the fetched stops to busStops list

            // Prepare the list of stop names for the ListView
            ArrayList<String> stopNames = new ArrayList<>();
            for (com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop stop : busStops) {
                if(languageCode.equals("en")){
                    stopNames.add(stop.getNameEn());
                }else if(languageCode.equals("zh")){
                    stopNames.add(stop.getNameZH());
                }
            }

            // Set the adapter for the ListView
            adapter = new BusStopListViewAdapter(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.this,
                    R.layout.list_item_white_text, stopNames);
            lvShowAllStop.setAdapter(adapter);

            Log.d(TAG, "DownloadTask: Bus stops loaded successfully");

            // 語音輸入自動啟動（資料載入且 UI ready 後）
            promptSpeechInput();
        }
    }

    /**
     * 啟動 Google Voice Service 進行語音輸入
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "請說出欲下車的巴士站");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (android.content.ActivityNotFoundException a) {
            Toast.makeText(this, "語音辨識服務不可用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            try {
                java.util.ArrayList<String> result = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String recognized = result.get(0);
                    tts.speak(new String[]{
                        "Input received: " + recognized + ". Finding the most relevant bus stop...",
                        "已接收語音：" + recognized + "，正在為你選擇最相關的巴士站..."
                    });
                    sendBusStopsToNLP(recognized, busStops);
                } else {
                    Toast.makeText(this, "語音辨識失敗，請再試一次", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Voice input error: ", e);
                Toast.makeText(this, "語音輸入發生錯誤", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 語音辨識結果 + 候選站名送到 Deepseek NLP，根據 NLP 結果自動選站
     */
    private void sendBusStopsToNLP(String userSpeech, ArrayList<BusStop> busStops) {
        // 將所有候選站名組成一個字串（用中文站名）
        StringBuilder sb = new StringBuilder();
        for (BusStop stop : busStops) {
            sb.append(stop.getNameZH()).append("\n");
        }
        String busStopList = sb.toString();
        String nlpPrompt = "候選站名如下：\n" + busStopList
                + "請根據以上候選站名，判斷哪一個最接近『" + userSpeech + "』。只回傳最相關的站名本身，不要回傳任何 JSON、指令或多餘文字。";
        nlpService.processText(nlpPrompt, new NLPCallback() {
            @Override
            public void onSuccess(org.json.JSONObject commandJson) {
                runOnUiThread(() -> {
                    String nlpResult = "";
                    if (commandJson != null) {
                        if (commandJson.has("content")) {
                            nlpResult = commandJson.optString("content");
                        } else if (commandJson.has("result")) {
                            nlpResult = commandJson.optString("result");
                        } else if (commandJson.has("text")) {
                            nlpResult = commandJson.optString("text");
                        } else {
                            nlpResult = commandJson.toString();
                        }
                        nlpResult = nlpResult.replaceAll("[{}\"\n]", "").trim();
                    }
                    int foundIdx = -1;
                    for (int i = 0; i < busStops.size(); i++) {
                        String stopName = busStops.get(i).getNameZH();
                        if (stopName.replaceAll("\\s", "").contains(nlpResult.replaceAll("\\s", "")) || nlpResult.replaceAll("\\s", "").contains(stopName.replaceAll("\\s", ""))) {
                            foundIdx = i;
                            break;
                        }
                    }
                    if (foundIdx != -1) {
                        View itemView = lvShowAllStop.getChildAt(foundIdx);
                        selectItem(foundIdx, itemView);
                        selectedPosition = foundIdx;
                        tts.speak(new String[]{
                            "Selected bus stop: " + busStops.get(foundIdx).getNameEn(),
                            "已自動選擇巴士站：" + busStops.get(foundIdx).getNameZH()
                        });
                    } else {
                        tts.speak(new String[]{
                            "Sorry, no matching bus stop found.",
                            "抱歉，沒有找到相符的巴士站"
                        });
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(VoiceControlEditEndPoint.this, "NLP服務異常", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Retrieves the latest bus route and bound from the database.
     */
    private void getBusRouteData() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                BusRoute = routeNumber; // Get bus route number

                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));
                if (boundValue != null) {
                    // Map bound value: assuming "O" = "outbound", "I" = "inbound"
                    if (boundValue.equalsIgnoreCase("O")) {
                        bound = "outbound";
                    } else if (boundValue.equalsIgnoreCase("I")) {
                        bound = "inbound";
                    } else {
                        bound = boundValue.toLowerCase(); // default to lowercase value
                    }
                } else {
                    bound = "";
                }
                
                // Retrieve language setting (from database)
                languageSetting = SettingDBHelper.getLanguageSetting();
                languageCode = (languageSetting != null && languageSetting.length > 0) ? languageSetting[0] : "en"; // Default to "en"
            } else {
                Log.d(TAG, "Error loading bus route data.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "getBusRouteData Error: " + e.toString());
            Toast.makeText(this, "Error loading bus route data", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Updates the database with the selected destination bus stop details.
     *
     * @param selectedBusStop The selected bus stop object.
     */
    private void updateDatabase(com.example.vista.VoiceControl.ByBus.VoiceControlEditEndPoint.BusStop selectedBusStop) {
        // Retrieve current database record
        Cursor cursor = null;
        try {
            cursor = dbHelper.getLatestBusRoute();
            if (cursor != null && cursor.moveToFirst()) {
                String routeNumber = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_ROUTE_NUMBER));
                String toStation = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO));
                String toStation_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_TO_ZH));
                String boundValue = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_BOUND));

                // Prepare all other columns, setting to existing or "---" if not being updated
                String destination = selectedBusStop.getNameEn();
                String destination_ZH = selectedBusStop.getNameZH();
                String destinationSeq = String.valueOf(selectedBusStop.getSeq());
                String destinationStopId = selectedBusStop.getStopId();
                String destinationLat = selectedBusStop.getLat();
                String destinationLong = selectedBusStop.getLon();

                // Fetch existing start point details
                String startPoint = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT));
                String startPoint_ZH = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_ZH));
                String startPointSeq = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_SEQ));
                String startPointStopId = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_STOP_ID));
                String startPointLat = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LAT));
                String startPointLong = cursor.getString(cursor.getColumnIndexOrThrow(BusDatabaseHelper.COLUMN_START_POINT_LONG));

                // Update the database with the new destination details
                long result = dbHelper.insertOrUpdateBusRoute(
                        dbHelper.getWritableDatabase(),
                        routeNumber,
                        toStation,
                        toStation_ZH,
                        boundValue,
                        startPoint,
                        startPoint_ZH,
                        startPointSeq,
                        startPointStopId,
                        startPointLat,
                        startPointLong,
                        destination,
                        destination_ZH,
                        destinationStopId,
                        destinationSeq,
                        destinationLat,
                        destinationLong
                );

                if (result != -1) {
                    Log.d(TAG, "Database updated successfully with selected destination");
                    Toast.makeText(this, "Destination updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error updating database");
                    Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "No bus route data found for updating destination.");
                Toast.makeText(this, "No bus route data found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateDatabase Error: " + e.toString());
            Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * BusStop: A class representing a bus stop with relevant details.
     */
    private class BusStop {
        private String stopId;
        private String nameEn;
        private String nameZH;
        private int seq;
        private String lat;
        private String lon;

        public BusStop(String stopId, String nameEn, String nameZH, int seq, String lat, String lon) {
            this.stopId = stopId;
            this.nameEn = nameEn;
            this.nameZH = nameZH;
            this.seq = seq;
            this.lat = lat;
            this.lon = lon;
        }

        public String getStopId() {
            return stopId;
        }

        public String getNameEn() {
            return nameEn;
        }

        public String getNameZH() {
            return nameZH;
        }

        public int getSeq() {
            return seq;
        }

        public String getLat() {
            return lat;
        }

        public String getLon() {
            return lon;
        }
    }
}