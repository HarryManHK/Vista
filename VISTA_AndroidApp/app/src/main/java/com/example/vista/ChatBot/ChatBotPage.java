package com.example.vista.ChatBot;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vista.R;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ChatBotPage extends AppCompatActivity {

    private Deepseek_R1_siliconflow_Client chatGPTClient;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar loadingProgressBar;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    private static final String TAG = "ChatBotPage_debug";  // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_bot_page);

        // Initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatGPTClient = new Deepseek_R1_siliconflow_Client();

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                try {
                    sendUserMessage(message);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void sendUserMessage(String message) throws JSONException {
        // Show the loading indicator
        loadingProgressBar.setVisibility(ProgressBar.VISIBLE);

        // Log the user message
        Log.d(TAG, "User message: " + message);

        // Add the user's message to the chat list
        messageList.add(new Message(message, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        // Add a "thinking" placeholder message
        final int thinkingMessagePosition = messageList.size();
        messageList.add(new Message("Bot is thinking...", false));
        chatAdapter.notifyItemInserted(thinkingMessagePosition);
        chatRecyclerView.scrollToPosition(thinkingMessagePosition);

        messageInput.setText("");

        // Send the message to the bot
        chatGPTClient.sendMessage(message, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    messageList.set(thinkingMessagePosition, new Message("Error: " + e.getMessage(), false));
                    chatAdapter.notifyItemChanged(thinkingMessagePosition);
                    loadingProgressBar.setVisibility(ProgressBar.GONE);  // Hide the loading indicator
                });

                // Log the error
                Log.e(TAG, "Error during API call: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Read the response and log it
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                            String line;
                            StringBuilder partialResponse = new StringBuilder();

                            while ((line = reader.readLine()) != null) {
                                // Log the streamed response
                                Log.d(TAG, "Bot response stream: " + line);

                                // Check if the line indicates the end of the stream
                                if (line.contains("[DONE]")) {
                                    // Stream ended, stop showing the loading indicator
                                    runOnUiThread(() -> loadingProgressBar.setVisibility(ProgressBar.GONE));
                                    break;
                                }

                                // Process and append the response chunk
                                if (line.startsWith("data: ")) {
                                    // Extract the actual content from the "data: " part
                                    String jsonResponse = line.substring(6); // Remove the 'data: ' part
                                    JSONObject jsonObject = new JSONObject(jsonResponse);
                                    String content = jsonObject.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("delta")
                                            .optString("content", "");

                                    String reasoning_content = jsonObject.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("delta")
                                            .optString("reasoning_content", "");

                                    // Only process and display non-null and non-empty content
                                    if (!content.equals("null") && !content.trim().isEmpty()) {
                                        // Append the content gradually
                                        for (char c : content.toCharArray()) {
                                            partialResponse.append(c);
                                            final String currentText = partialResponse.toString();

                                            // Update the UI with each new chunk
                                            runOnUiThread(() -> {
                                                messageList.set(thinkingMessagePosition, new Message(currentText, false));
                                                chatAdapter.notifyItemChanged(thinkingMessagePosition);
                                                chatRecyclerView.scrollToPosition(thinkingMessagePosition);
                                            });
                                        }
                                    }
                                    if (!reasoning_content.equals("null") && !reasoning_content.trim().isEmpty()) {
                                        for (char c : reasoning_content.toCharArray()) {
                                            partialResponse.append(c);
                                            final String currentText = partialResponse.toString();

                                            // Update the UI with each new chunk
                                            runOnUiThread(() -> {
                                                messageList.set(thinkingMessagePosition, new Message(currentText, false));
                                                chatAdapter.notifyItemChanged(thinkingMessagePosition);
                                                chatRecyclerView.scrollToPosition(thinkingMessagePosition);
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                    }
                } else {
                    runOnUiThread(() -> {
                        messageList.set(thinkingMessagePosition, new Message("Request failed", false));
                        chatAdapter.notifyItemChanged(thinkingMessagePosition);
                        loadingProgressBar.setVisibility(ProgressBar.GONE);  // Hide the loading indicator
                    });

                    // Log the failed response
                    Log.e(TAG, "Request failed: " + response.message());
                }
            }
        });
    }
}