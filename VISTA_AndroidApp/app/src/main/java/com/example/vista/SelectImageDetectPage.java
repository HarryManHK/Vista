package com.example.vista;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

import com.example.vista.TextToSpeech.CustomTextToSpeech;

public class SelectImageDetectPage extends AppCompatActivity {

    private static final String TAG = "SelectImageDetectPage_debug";

    ImageView imgResult;
    TextView txtResult;

    private OkHttpClient httpClient;
    private CustomTextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_image_detect_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtResult = findViewById(R.id.txtResult);
        imgResult = findViewById(R.id.imgResult);

        // Initialize OkHttpClient
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();

        // Initialize CustomTextToSpeech
        tts = new CustomTextToSpeech(this);

        // Get image URI from Intent
        Intent intent = getIntent();
        String imageUriString = intent.getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            Log.d(TAG, "onCreate: Received image URI: " + imageUri);

            // Display the image
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                imgResult.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Error loading image", e);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }

            // Start the network request
            postImageToServer(imageUri);
        } else {
            Log.e(TAG, "onCreate: No image URI found in Intent extras");
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Convert Bitmap to JPEG bytes, then do a multipart/form-data POST to the Flask server.
     * The result will update the TextView.
     */
    private void postImageToServer(Uri imageUri) {
        Log.d(TAG, "postImageToServer: started");

        // Set "Loading..." text before starting the network call
        txtResult.setText("Loading...");

        Bitmap bitmap = null;
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            bitmap = BitmapFactory.decodeStream(imageStream);
        } catch (IOException e) {
            Log.e(TAG, "postImageToServer: Error decoding image URI", e);
            txtResult.setText("Error loading image.");
            return;
        }

        if (bitmap == null) {
            Log.e(TAG, "postImageToServer: Bitmap is null");
            txtResult.setText("Error loading image.");
            return;
        }

        // Compress the bitmap to JPEG
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] imageBytes = bos.toByteArray();

        // Build multipart body
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", "image.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();

        String url = "https://d.harryman.cc/api/image2text"; // Update with your server URL
        Log.d(TAG, "postImageToServer: POST to " + url);

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Perform asynchronous network request
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "postImageToServer onFailure: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(SelectImageDetectPage.this, "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    txtResult.setText("Error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "postImageToServer onResponse: code=" + response.code());
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(SelectImageDetectPage.this, "Server Error: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        txtResult.setText("Server Error: " + response.code());
                    });
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "postImageToServer onResponse body: " + responseBody);

                try {
                    JSONObject json = new JSONObject(responseBody);

                    // The AI result is typically in json["choices"][0]["message"]["content"]
                    JSONArray choicesArray = json.optJSONArray("choices");
                    if (choicesArray != null && choicesArray.length() > 0) {
                        JSONObject firstChoice = choicesArray.getJSONObject(0);
                        JSONObject messageObj = firstChoice.optJSONObject("message");
                        if (messageObj != null) {
                            String content = messageObj.optString("content", "No content found");

                            // Update the TextView with the result
                            runOnUiThread(() -> {
                                txtResult.setText(content);
                                tts.speak(content);
                            });
                        } else {
                            Log.e(TAG, "postImageToServer: 'message' object is null");
                            runOnUiThread(() -> {
                                Toast.makeText(SelectImageDetectPage.this, "No 'message' in response", Toast.LENGTH_SHORT).show();
                                txtResult.setText("No 'message' in response");
                            });
                        }
                    } else {
                        Log.e(TAG, "postImageToServer: 'choices' is null or empty");
                        runOnUiThread(() -> {
                            Toast.makeText(SelectImageDetectPage.this, "No 'choices' in response", Toast.LENGTH_SHORT).show();
                            txtResult.setText("No 'choices' in response");
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "postImageToServer JSON parse error: ", e);
                    runOnUiThread(() -> {
                        Toast.makeText(SelectImageDetectPage.this, "JSON parse error", Toast.LENGTH_SHORT).show();
                        txtResult.setText("JSON parse error");
                    });
                }
            }
        });
    }
}