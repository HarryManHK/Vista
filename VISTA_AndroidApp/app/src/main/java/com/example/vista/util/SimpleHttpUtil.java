package com.example.vista.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * SimpleHttpUtil: Minimal example for an HTTP GET request, returning a String response.
 * In production, prefer OkHttp / Retrofit with asynchronous usage.
 */
public class SimpleHttpUtil {

    public static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error code: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
