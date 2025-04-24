package com.example.vista;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Locale;

public class SpeechRecognizerHelper {
    private SpeechRecognizer speechRecognizer;
    private Activity activity;
    private RecognitionListener listener;

    public SpeechRecognizerHelper(Activity activity, RecognitionListener listener) {
        this.activity = activity;
        this.listener = listener;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(listener);
    }

    public void startListening(String languageCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "請說話...");
        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) speechRecognizer.stopListening();
    }

    public void destroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
