package com.example.multyfigroww;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class GrowwPortfolioClient {
    private GrowwPortfolioClient() {}

    public static final class Position {
        public final String symbol;
        public final String exchange;
        public final String product;
        public final int quantity;
        public final double netPrice;

        Position(String symbol, String exchange, String product, int quantity, double netPrice) {
            this.symbol = symbol;
            this.exchange = exchange;
            this.product = product;
            this.quantity = quantity;
            this.netPrice = netPrice;
        }

        public boolean isOpen() { return quantity != 0; }
        public String originalSide() { return quantity > 0 ? "BUY" : "SELL"; }
        public String exitSide() { return quantity > 0 ? "SELL" : "BUY"; }
        public int remainingQuantity() { return Math.abs(quantity); }
    }

    public static Position fetchMisPosition(String accessToken, String exchange, String symbol) throws Exception {
        requireToken(accessToken);
        String endpoint = "https://api.groww.in/v1/positions/trading-symbol?trading_symbol=" +
                URLEncoder.encode(symbol, StandardCharsets.UTF_8.name()) + "&segment=CASH";
        JSONObject root = getJson(endpoint, accessToken);
        JSONObject payload = root.optJSONObject("payload");
        JSONArray positions = payload == null ? null : payload.optJSONArray("positions");
        if (positions == null) return null;

        int totalQty = 0;
        double netPrice = 0;
        boolean found = false;
        for (int i = 0; i < positions.length(); i++) {
            JSONObject p = positions.optJSONObject(i);
            if (p == null) continue;
            if (!symbol.equalsIgnoreCase(p.optString("trading_symbol"))) continue;
            if (!"MIS".equalsIgnoreCase(p.optString("product"))) continue;
            String pxExchange = p.optString("exchange", exchange);
            if (!exchange.equalsIgnoreCase(pxExchange)) continue;
            found = true;
            totalQty += p.optInt("quantity", 0);
            netPrice = p.optDouble("net_price", netPrice);
        }
        return found ? new Position(symbol.toUpperCase(), exchange.toUpperCase(), "MIS", totalQty, netPrice) : null;
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
            throw new IllegalStateException("Groww position request failed (HTTP " + code + ").");
        }
        JSONObject root = new JSONObject(sb.toString());
        if (!"SUCCESS".equalsIgnoreCase(root.optString("status", "SUCCESS"))) {
            throw new IllegalStateException("Groww position response was not successful.");
        }
        return root;
    }

    private static void requireToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Groww access token is missing.");
        }
    }
}
