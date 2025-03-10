// File: NLPCallback.java
package com.example.vista.VoiceControl;

import org.json.JSONObject;

public interface NLPCallback {
    void onSuccess(JSONObject commandJson);
    void onFailure(Exception e);
}