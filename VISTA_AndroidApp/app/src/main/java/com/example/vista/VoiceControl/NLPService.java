// File: NLPService.java
package com.example.vista.VoiceControl;

import org.json.JSONObject;

public interface NLPService {
    void processText(String text, NLPCallback callback);
}