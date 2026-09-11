package com.example.multyfigroww;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class GrowwLtpClient {
    private GrowwLtpClient() {}

    public static double fetch(String accessToken, String exchange, String symbol) throws Exception {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Groww access token is missing.");
        }
        String key = exchange + "_" + symbol;
        String endpoint = "https://api.groww.in/v1/live-data/ltp?segment=CASH&exchange_symbols=" +
                URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken.trim());
        conn.setRequestProperty("X-API-VERSION", "1.0");

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Groww LTP request failed (HTTP " + code + ").");
        }
        JSONObject root = new JSONObject(sb.toString());
        if (!"SUCCESS".equalsIgnoreCase(root.optString("status", "SUCCESS"))) {
            throw new IllegalStateException("Groww LTP response was not successful.");
        }
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null || !payload.has(key)) {
            throw new IllegalStateException("Groww did not return LTP for " + key + ". Check the stock symbol.");
        }
        double ltp = payload.getDouble(key);
        if (ltp <= 0) throw new IllegalStateException("Groww returned an invalid LTP.");
        return ltp;
    }
}
