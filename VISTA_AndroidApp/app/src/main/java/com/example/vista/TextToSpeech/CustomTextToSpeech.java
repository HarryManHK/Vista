package com.example.vista.TextToSpeech;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;

import java.util.Locale;

public class CustomTextToSpeech {
    private TextToSpeech textToSpeech;
    private String currentLanguage = "en"; // Default to English
    private Context context;
    private final String TAG = "CustomTTS_debug";

    // Constructor
    public CustomTextToSpeech(Context context) {
        this.context = context;
        initializeTextToSpeech();
        setSpeechRate();
    }

    // Initialize TextToSpeech object
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, new OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Get language settings from the database
                    SettingDatabaseHelper dbHelper = SettingDatabaseHelper.getInstance(context);
                    String[] languageSetting = dbHelper.getLanguageSetting();
                    if (languageSetting != null) {
                        currentLanguage = languageSetting[0];  // Set language from the settings
                    }

                    // Set the locale based on the language
                    Locale locale;
                    if ("zh".equals(currentLanguage)) {
                        // Try Cantonese (zh_HK), if not fall back to Mandarin (zh_CN)
                        locale = new Locale("zh", "HK");  // Cantonese (Hong Kong)
                        if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_MISSING_DATA ||
                                textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                            locale = Locale.CHINESE;  // Fallback to Mandarin
                        }
                    } else {
                        // Default to English
                        locale = new Locale(currentLanguage);
                    }

                    int result = textToSpeech.setLanguage(locale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "Language not supported or missing data", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Log or handle when speech starts
            }

            @Override
            public void onDone(String utteranceId) {
                // Log or handle when speech ends
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error with text to speech");
            }
        });
    }

    /**
     * Sets the speech rate for the TextToSpeech engine.
     *
     * @param rate The speech rate. 1.0 is the normal rate. Lower values slow down the speech,
     *             and higher values speed it up.
     */
    private void setSpeechRate() {
        float rate = 1.0f;

        // Get language settings from the database
        SettingDatabaseHelper dbHelper = SettingDatabaseHelper.getInstance(context);
        float VoiceSpeed = dbHelper.getVoiceSpeed();

        rate = VoiceSpeed;

        // The valid range is typically 0.1 to 2.0
        if (rate < 0.1f) {
            rate = 0.1f;
        } else if (rate > 2.0f) {
            rate = 2.0f;
        }
        textToSpeech.setSpeechRate(rate);
        Log.d(TAG, "Speech rate set to: " + rate);
    }


    // Function to speak text based on the language
    public void speak(String[] text) {
        if (text != null && text.length > 0) {
            String textToSpeak = currentLanguage.equals("en") ? text[0] : text[1];
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Function to release resources
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}