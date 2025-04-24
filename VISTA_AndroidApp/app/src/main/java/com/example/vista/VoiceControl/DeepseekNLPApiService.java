package com.example.vista.VoiceControl;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepseekNLPApiService implements NLPService {
    private static final String TAG = "DeepseekNLPApiService_debug";
    private static final String API_KEY = "sk-0f4b1f0fe31a4d5a805a465cb8ef7306"; // 請換成你自己的Key
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final int max_tokens = 2048;

    @Override
    public void processText(String text, NLPCallback callback) {
        new Thread(() -> {
            try {
                String prompt = createPrompt(text);
                JSONObject payload = createPayload(prompt);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        payload.toString()
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Deepseek API raw response: " + responseBody);
                    JSONObject responseJson = new JSONObject(responseBody);
                    JSONArray choices = responseJson.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        String content = choice.getJSONObject("message").getString("content");
                        try {
                            JSONObject commandJson = new JSONObject(content);
                            Log.d(TAG, "Deepseek API final json: " + commandJson.toString());
                            callback.onSuccess(commandJson);
                        } catch (Exception e) {
                            JSONObject textJson = new JSONObject();
                            textJson.put("content", content.trim());
                            callback.onSuccess(textJson);
                        }
                    } else {
                        callback.onFailure(new Exception("No choices in response"));
                    }
                } else {
                    callback.onFailure(new Exception("Deepseek API error: " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "processText error: " + e.getMessage());
                callback.onFailure(e);
            }
        }).start();
    }

    private String createPrompt(String text) {
    String cmd = text == null ? "" : text.trim();
    // 僅支援 action 指令，不再支援設置提醒距離
    if (cmd.contains("距離") || cmd.contains("幾遠") || cmd.toLowerCase().contains("distance")) {
        return "請嚴格只用這個 JSON 格式回覆：{\"action\": \"查距離\"}。不要有其他內容。";
    } else if (cmd.contains("下一站") || cmd.toLowerCase().contains("next stop")) {
        return "請嚴格只用這個 JSON 格式回覆：{\"action\": \"下一站\"}。不要有其他內容。";
    } else if ((cmd.contains("查巴士") && cmd.contains("到站")) || cmd.contains("到站時間") || cmd.toLowerCase().contains("arrival")) {
        return "請嚴格只用這個 JSON 格式回覆：{\"action\": \"查巴士到站時間\"}。不要有其他內容。";
    } else if (cmd.contains("提醒") || cmd.toLowerCase().contains("remind")) {
        return "請嚴格只用這個 JSON 格式回覆：{\"action\": \"巴士到站提醒\"}。不要有其他內容。";
    } else {
        return "請根據以下用戶語音轉文字結果解析出使用者的命令，並嚴格按照以下 4 種 JSON 格式返回：\n"
            + "1. 如果用戶想查距離，請返回格式：\n"
            + "{\n  \"action\": \"查距離\"\n}\n"
            + "2. 如果用戶想查詢下一站，請返回格式：\n"
            + "{\n  \"action\": \"下一站\"\n}\n"
            + "3. 如果用戶想查巴士到站時間，請返回格式：\n"
            + "{\n  \"action\": \"查巴士到站時間\"\n}\n"
            + "4. 如果用戶想啟動巴士到站提醒，請返回格式：\n"
            + "{\n  \"action\": \"巴士到站提醒\"\n}\n"
            + "請只返回 JSON 格式，不要其他額外文字，且全部使用繁體中文。\n"
            + "用戶語音轉文字結果：" + text;
    }
}

    private JSONObject createPayload(String prompt) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", "deepseek-chat");
        payload.put("stream", false);
        payload.put("max_tokens", max_tokens);
        payload.put("temperature", 0.7);
        payload.put("top_p", 0.9);
        payload.put("frequency_penalty", 0.0);
        payload.put("n", 1);

        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        JSONArray messages = new JSONArray();
        messages.put(messageObject);
        payload.put("messages", messages);

        return payload;
    }
}
