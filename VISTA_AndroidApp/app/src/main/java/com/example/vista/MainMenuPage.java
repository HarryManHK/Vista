package com.example.vista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;  // Import the custom TextToSpeech class

public class MainMenuPage extends AppCompatActivity {

    private TextView txtTitle;
    private Button btnImageToText, btnFindBusStop, btnConfirm, btnNext, btnSetting;
    private Button[] buttons;  // Array to hold all buttons for cycling through them
    private int selectedButtonIndex = 0;  // Index to keep track of the selected button

    private CustomTextToSpeech customTextToSpeech;  // TextToSpeech instance using the custom class
    private SettingDatabaseHelper dbHelper; // Database helper instance

    // Initialize language Setting
    @Override
    protected void attachBaseContext(Context newBase) {
        // Initialize Database Helper
        dbHelper = SettingDatabaseHelper.getInstance(newBase);

        // Retrieve language settings from the database
        String[] languageSetting = dbHelper.getLanguageSetting();
        String languageCode = "en"; // Default to English
        String countryCode = "US";   // Default to US

        if (languageSetting != null) {
            languageCode = languageSetting[0];
            countryCode = languageSetting[1];
        }

        // Apply the locale before super.onCreate()
        Context context = LocaleHelper.setLocale(newBase, languageCode, countryCode);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu_page);
        // Initialize buttons
        txtTitle = findViewById(R.id.textView2);
        btnImageToText = findViewById(R.id.btnMainMenuImgToTxt);
        btnFindBusStop = findViewById(R.id.button5);
        btnSetting = findViewById(R.id.btnMainMenuSetting);
        btnConfirm = findViewById(R.id.btnMainMenuConfirm);
        btnNext = findViewById(R.id.btnMainMenuNext);

        // Add all buttons to an array for easy cycling
        buttons = new Button[]{btnImageToText, btnFindBusStop, btnSetting};

        // Initial setup: Make the first button yellow
        updateButtonColor();

        // Initialize CustomTextToSpeech
        customTextToSpeech = new CustomTextToSpeech(MainMenuPage.this);

        // Set onClickListener for "Image To Text" button
        btnImageToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Image To Text page.", "你將進入圖片轉文字頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the ImageToTextMenu activity
                Intent intent = new Intent(MainMenuPage.this, ImageToTextMenu.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Find Bus Stop" button
        btnFindBusStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Find Bus Stop page.", "你將進入查找巴士站頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the FindBusStopMenuPage activity
                Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Setting" button
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                String[] buttonLabel = {"You will go to Setting page.", "你將進入設置頁面。"};
                announceButtonLabel(buttonLabel);

                // Create an Intent to start the SettingPage activity
                Intent intent = new Intent(MainMenuPage.this, SettingPage.class);
                startActivity(intent);
            }
        });

        // Set onClickListener for "Next" button
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change the selected button index (cycle through buttons)
                selectedButtonIndex = (selectedButtonIndex + 1) % buttons.length;

                // Update the button color
                updateButtonColor();

                // Announce the new selected button label
                String[] buttonLabel = {buttons[selectedButtonIndex].getText().toString(), buttons[selectedButtonIndex].getText().toString()};
                announceButtonLabel(buttonLabel);
            }
        });

        // Set onClickListener for "Confirm" button
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the currently selected button and simulate a click
                Button selectedButton = buttons[selectedButtonIndex];

                // Announce the page that the user will go to
                String[] buttonLabel = new String[2];
                if (selectedButton == btnImageToText) {
                    buttonLabel[0] = "You will go to Image To Text page.";
                    buttonLabel[1] = "你將進入圖片轉文字頁面。";
                } else if (selectedButton == btnFindBusStop) {
                    buttonLabel[0] = "You will go to Find Bus Stop page.";
                    buttonLabel[1] = "你將進入查找巴士站頁面。";
                }else if (selectedButton == btnSetting) {
                    buttonLabel[0] = "You will go to Setting page.";
                    buttonLabel[1] = "你將進入設定頁面。";
                }

                announceButtonLabel(buttonLabel);

                // Simulate the button click action
                simulateButtonClick(selectedButton);
            }
        });
    }

    // Function to change the color of the buttons
    private void updateButtonColor() {
        // Reset all buttons to their original color
        for (Button button : buttons) {
            button.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));  // Original color
        }

        // Set the selected button to yellow
        buttons[selectedButtonIndex].setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));  // Yellow
    }

    // Function to simulate button click
    private void simulateButtonClick(Button selectedButton) {
        // For demonstration, we'll use a Toast to simulate the click action.
        // You can replace this with the actual logic you want to trigger.
        if (selectedButton == btnImageToText) {
            // Navigate to the ImageToTextMenu activity or perform any action
            Toast.makeText(MainMenuPage.this, "Image To Text clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainMenuPage.this, ImageToTextMenu.class);
            startActivity(intent);
        } else if (selectedButton == btnFindBusStop) {
            // Perform action for Find Bus Stop button
            Toast.makeText(MainMenuPage.this, "Find Bus Stop clicked!", Toast.LENGTH_SHORT).show();
            // Create an Intent to start the FindBusStopMenuPage activity
            Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
            startActivity(intent);

        } else if (selectedButton == btnSetting) {
            // Perform action for Find Bus Stop button
            Toast.makeText(MainMenuPage.this, "Setting clicked!", Toast.LENGTH_SHORT).show();
            // Create an Intent to start the SettingPage activity
            Intent intent = new Intent(MainMenuPage.this, SettingPage.class);
            startActivity(intent);
        }
    }

    // Function to announce the button label using TextToSpeech
    private void announceButtonLabel(String[] label) {
        // Announce the label based on the selected language
        customTextToSpeech.speak(label);
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
    protected void onRestart() {
        super.onRestart();
        recreate();

        txtTitle.setText(getString(R.string.MainMenuPageActivity_title));
        btnSetting.setText(getString(R.string.MainMenuPageActivity_Setting));
        btnImageToText.setText(getString(R.string.MainMenuPageActivity_ImageToText));
        btnFindBusStop.setText(getString(R.string.MainMenuPageActivity_FindBusStop));
        btnConfirm.setText(getString(R.string.btn_Confirm));
        btnNext.setText(getString(R.string.btn_next));
    }
}