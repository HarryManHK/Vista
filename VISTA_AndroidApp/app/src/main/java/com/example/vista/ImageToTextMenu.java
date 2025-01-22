package com.example.vista;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log; // For Log.d usage
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.TextToSpeech.CustomTextToSpeech;

import java.io.File;

public class ImageToTextMenu extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity_debug";  // For logging

    private static final int CODE_ALBUM = 1;
    private static final int CODE_CAMERA = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    private Button btnRealTimeDetectPage;
    private Button btnSelectImageDetect;
    private Button btnTakePhotoDetect;

    private CustomTextToSpeech customTextToSpeech; //text to speech module

    private Uri cameraFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_to_text_menu);

        // Adjust view insets for Edge-to-Edge layouts
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request camera and storage permissions if needed
        requestPermissionsIfNeeded();

        // Initialize buttons
        btnRealTimeDetectPage = findViewById(R.id.btnGoRealTimeDetectPage);
        btnTakePhotoDetect    = findViewById(R.id.btnTakePhotoDetect);
        btnSelectImageDetect  = findViewById(R.id.btnSelectImageDetect);

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(ImageToTextMenu.this);

        // Set click listeners
        btnRealTimeDetectPage.setOnClickListener(this);
        btnTakePhotoDetect.setOnClickListener(this);
        btnSelectImageDetect.setOnClickListener(this);

        Log.d(TAG, "onCreate: MainActivity initialized");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnGoRealTimeDetectPage) {
            Toast.makeText(this, "Start Real Time Detection", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onClick: btnGoRealTimeDetectPage pressed");

            // Announce button label
            String[] buttonLabel = {"You will go to Real Time Detect page.", "你將進入實時偵測頁面。"};
            announceButtonLabel(buttonLabel);

            // TODO: navigate to RealTimeDetectPage if needed

        } else if (id == R.id.btnSelectImageDetect) {
            Toast.makeText(this, "Start Select Image Detection", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onClick: btnSelectImageDetect pressed");

            // Announce button label
            String[] buttonLabel = {"You will go to Select Image Detect page.", "你將進入選擇圖片檢測頁面。"};
            announceButtonLabel(buttonLabel);

            // Open gallery
            Intent chooseIntent = new Intent(Intent.ACTION_GET_CONTENT);
            chooseIntent.setType("image/*");
            chooseIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(chooseIntent, CODE_ALBUM);

        } else if (id == R.id.btnTakePhotoDetect) {
            Toast.makeText(this, "Start Take Photo Detection", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onClick: btnTakePhotoDetect pressed");

            // Announce button label
            String[] buttonLabel = {"You will go to Take Photo Detect page.", "你將進入拍照檢測頁面。"};
            announceButtonLabel(buttonLabel);

            //main function
            launchCamera();
        }
    }

    /**
     * Launch the camera app to capture a photo, saving the output to a file in app-specific storage.
     */
    private void launchCamera() {
        Log.d(TAG, "launchCamera: called");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Generate a unique file name
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

        // Create content Uri via FileProvider
        cameraFileUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );

        Log.d(TAG, "launchCamera: fileUri = " + cameraFileUri);

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri);
        startActivityForResult(cameraIntent, CODE_CAMERA);
    }

    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        // If the user canceled, do nothing
        if (resultCode != RESULT_OK) {
            Log.d(TAG, "onActivityResult: result not OK, user likely canceled.");
            return;
        }

        Uri selectedImageUri = null;

        if (requestCode == CODE_ALBUM && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Log.d(TAG, "onActivityResult: Selected image URI " + selectedImageUri);

        } else if (requestCode == CODE_CAMERA) {
            selectedImageUri = cameraFileUri;
            Log.d(TAG, "onActivityResult: cameraFileUri = " + selectedImageUri);
        }

        if (selectedImageUri != null) {
            // Start SelectImageDetectPage with the image URI
            Intent intent = new Intent(ImageToTextMenu.this, SelectImageDetectPage.class);
            intent.putExtra("imageUri", selectedImageUri.toString());
            startActivity(intent);
        } else {
            Log.d(TAG, "onActivityResult: selectedImageUri is null, nothing to upload");
        }
    }

    /**
     * Request camera and storage permissions if not already granted.
     */
    private void requestPermissionsIfNeeded() {
        String[] permissionsNeeded = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean shouldRequest = false;
        for (String perm : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                shouldRequest = true;
                break;
            }
        }

        if (shouldRequest) {
            Log.d(TAG, "requestPermissionsIfNeeded: requesting permissions...");
            ActivityCompat.requestPermissions(this, permissionsNeeded, REQUEST_PERMISSIONS);
        } else {
            Log.d(TAG, "requestPermissionsIfNeeded: permissions already granted");
        }
    }

    @Override
    protected void onDestroy() {
        // Release the TextToSpeech resources when the activity is destroyed
        if (customTextToSpeech != null) {
            customTextToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);
        // Optional: handle denied permissions if needed
    }
}