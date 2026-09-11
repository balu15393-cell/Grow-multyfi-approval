package com.example.multyfigroww;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class GrowwOrderClient {
    private GrowwOrderClient() {}

    public static boolean hasActiveExitOrder(String accessToken, String exchange, String symbol, String exitSide) throws Exception {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Groww access token is missing.");
        }
        String endpoint = "https://api.groww.in/v1/order/list?segment=CASH&page=0&page_size=100";
        JSONObject root = getJson(endpoint, accessToken);
        JSONObject payload = root.optJSONObject("payload");
        JSONArray orders = payload == null ? null : payload.optJSONArray("order_list");
        if (orders == null) return false;

        for (int i = 0; i < orders.length(); i++) {
            JSONObject o = orders.optJSONObject(i);
            if (o == null) continue;
            if (!symbol.equalsIgnoreCase(o.optString("trading_symbol"))) continue;
            if (!exchange.equalsIgnoreCase(o.optString("exchange", exchange))) continue;
            if (!"MIS".equalsIgnoreCase(o.optString("product"))) continue;
            if (!exitSide.equalsIgnoreCase(o.optString("transaction_type"))) continue;
            if (isActive(o.optString("order_status"), o.optInt("remaining_quantity", -1))) return true;
        }
        return false;
    }

    private static boolean isActive(String status, int remainingQuantity) {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return remainingQuantity > 0;
        if (s.contains("CANCEL") || s.contains("REJECT") || s.contains("COMPLETE") ||
                s.contains("EXECUT") || s.contains("FILLED") || s.contains("FAILED")) {
            return false;
        }
        return remainingQuantity != 0;
    }

    private static JSONObject getJson(String endpoint, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token.trim());
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
            throw new IllegalStateException("Groww order-list request failed (HTTP " + code + ").");
        }
        JSONObject root = new JSONObject(sb.toString());
        if (!"SUCCESS".equalsIgnoreCase(root.optString("status", "SUCCESS"))) {
            throw new IllegalStateException("Groww order-list response was not successful.");
        }
        return root;
    }
}
