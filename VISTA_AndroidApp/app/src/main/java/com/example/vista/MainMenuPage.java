package com.example.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//provide audio Speech
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.Engine;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuPage extends AppCompatActivity {

    private Button btnImageToText, btnFindBusStop, btnConfirm, btnNext;
    private Button[] buttons;  // Array to hold all buttons for cycling through them
    private int selectedButtonIndex = 0;  // Index to keep track of the selected button

    private TextToSpeech textToSpeech;  // TextToSpeech instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu_page);

        // Initialize buttons
        btnImageToText = findViewById(R.id.btnMainMenuImgToTxt);
        btnFindBusStop = findViewById(R.id.button5);
        btnConfirm = findViewById(R.id.btnMainMenuConfirm);
        btnNext = findViewById(R.id.btnMainMenuNext);

        // Add all buttons to an array for easy cycling
        buttons = new Button[]{btnImageToText, btnFindBusStop};

        // Initial setup: Make the first button yellow
        updateButtonColor();

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(MainMenuPage.this, new OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Set language to US English
                    int langResult = textToSpeech.setLanguage(Locale.US);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainMenuPage.this, "Text to Speech language not supported", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainMenuPage.this, "Text to Speech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set onClickListener for "Image To Text" button
        btnImageToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Announce button label
                announceButtonLabel("You will go to Image To Text page.");

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
                announceButtonLabel("You will go to Find Bus Stop page.");

                // Create an Intent to start the ImageToTextMenu activity
                Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
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
                announceButtonLabel(buttons[selectedButtonIndex].getText().toString());
            }
        });

        // Set onClickListener for "Confirm" button
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the currently selected button and simulate a click
                Button selectedButton = buttons[selectedButtonIndex];

                // Announce the page that the user will go to
                if (selectedButton == btnImageToText) {
                    announceButtonLabel("You will go to Image To Text page.");
                } else if (selectedButton == btnFindBusStop) {
                    announceButtonLabel("You will go to Find Bus Stop page.");
                }

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
            // Create an Intent to start the ImageToTextMenu activity
            Intent intent = new Intent(MainMenuPage.this, FindBusStopMenuPage.class);
            startActivity(intent);

        }
    }

    // Function to announce the button label using TextToSpeech
    private void announceButtonLabel(String label) {
        if (textToSpeech != null) {
            textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        // Release the TextToSpeech resources when the activity is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}