// File: DeepSeekNLPService.java
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

public class siliconflow_Deepseek_NLP implements NLPService {
    private static final String TAG = "DeepSeekNLPService_debug";
    // Replace with your actual DeepSeek API key
    private static final String API_KEY = "sk-nvapiliqrltqkcpitxkbbxuwcmdfupyimcejzjfaydxihylb";

    @Override
    public void processText(String text, NLPCallback callback) {
        new Thread(() -> {
            try {
                // Build prompt text
                String prompt = createPrompt(text);

                // Build API payload
                JSONObject payload = createPayload(prompt);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(90, TimeUnit.SECONDS)
                        .readTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(90, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        payload.toString()
                );

                Request request = new Request.Builder()
                        .url("https://api.siliconflow.cn/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "DeepSeek raw response: " + responseBody);

                    JSONObject responseJson = new JSONObject(responseBody);
                    JSONArray choices = responseJson.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        String content = choice.getJSONObject("message").getString("content");
                        // Parse the returned JSON command
                        JSONObject commandJson = new JSONObject(content);
                        callback.onSuccess(commandJson);
                    } else {
                        callback.onFailure(new Exception("No choices in response"));
                    }
                } else {
                    callback.onFailure(new Exception("DeepSeek API error: " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "processText error: " + e.getMessage());
                callback.onFailure(e);
            }
        }).start();
    }

    private String createPrompt(String text) {
        return "請根據以下用戶語音轉文字結果解析出使用者的命令，並嚴格按照以下五種 JSON 格式返回：\n" +
                "1. 如果用戶想搭巴士，請返回格式：\n" +
                "{\n" +
                "  \"action\": \"搭巴士\",\n" +
                "  \"routeNumber\": \"42A\",\n // 如用戶未提供目的地，請將此值設為 null\\n\" " +
                "  \"startPoint\": \"荃灣\",\n // 如用戶未提供目的地，請將此值設為 null\\n\" " +
                "  \"endPoint\": \"佐敦\",\n // 如用戶未提供目的地，請將此值設為 null\\n\" " +
                "  \"destination\": \"佐敦\"  // 如用戶未提供目的地，請將此值設為 null\n" +
                "}\n" +
                "2. 如果用戶想查巴士到站時間，請返回格式：\n" +
                "{\n" +
                "  \"action\": \"查巴士到站時間\"\n" +
                "}\n" +
                "3. 如果用戶想檢測巴士站，請返回格式：\n" +
                "{\n" +
                "  \"action\": \"檢測巴士站\"\n" +
                "}\n" +
                "4. 如果用戶想巴士到站提醒，請返回格式：\n" +
                "{\n" +
                "  \"action\": \"巴士到站提醒\"\n" +
                "}\n" +
                "5. 如果用戶希望圖片生成文字，請返回格式：\n" +
                "{\n" +
                "  \"action\": \"圖片生成文字\"\n" +
                "}\n" +
                "請只返回 JSON 格式，不要其他額外文字，且全部使用繁體中文。\n" +
                "用戶語音轉文字結果：" + text;
    }

    private JSONObject createPayload(String prompt) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", "deepseek-ai/DeepSeek-R1-Distill-Llama-8B");
        payload.put("stream", false);
        payload.put("max_tokens", 5000);
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