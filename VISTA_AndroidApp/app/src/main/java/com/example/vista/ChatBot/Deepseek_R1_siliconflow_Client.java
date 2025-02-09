package com.example.vista.ChatBot;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class Deepseek_R1_siliconflow_Client {

    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private static final String API_KEY = "sk-nvapiliqrltqkcpitxkbbxuwcmdfupyimcejzjfaydxihylb";  // Replace with your actual API key

    private OkHttpClient client = new OkHttpClient();

    public Deepseek_R1_siliconflow_Client() {
        // Increase the timeout duration for connection, read, and write
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // Set connection timeout to 30 seconds
                .readTimeout(60, TimeUnit.SECONDS)     // Set read timeout to 30 seconds
                .writeTimeout(60, TimeUnit.SECONDS)    // Set write timeout to 30 seconds
                .build();
    }

    public void sendMessage(String message, Callback callback) throws JSONException {
        // Simplified JSON request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-ai/DeepSeek-R1-Distill-Llama-8B");  // Example model, adjust to a valid one
        requestBody.put("stream", true);  // Enable streaming
        requestBody.put("max_tokens", 5000);  // Set a reasonable token limit
        requestBody.put("temperature", 0.7);
        requestBody.put("top_p", 0.9);
        requestBody.put("frequency_penalty", 0.0);
        requestBody.put("n", 1);

        // Build the user message
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", message);

        // Add the messages array
        requestBody.put("messages", new org.json.JSONArray().put(messageObject));

        // Create the request body
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toString()
        );

        // Build the request
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // Execute the request
        client.newCall(request).enqueue(callback);
    }
}