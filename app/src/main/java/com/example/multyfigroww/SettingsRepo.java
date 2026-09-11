package com.example.multyfigroww;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsRepo {
    private static final String PREFS = "trade_settings";
    private static final String K_ENABLED = "enabled";
    private static final String K_CAPITAL = "capital";
    private static final String K_UTIL = "utilization";
    private static final String K_TOL = "tolerance";
    private static final String K_STRICT = "strict_intraday";
    private static final String K_EXCHANGE = "exchange";

    private final SharedPreferences prefs;

    public SettingsRepo(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() { return prefs.getBoolean(K_ENABLED, true); }
    public double capital() { return Double.longBitsToDouble(prefs.getLong(K_CAPITAL, Double.doubleToLongBits(25000.0))); }
    public double utilizationPct() { return Double.longBitsToDouble(prefs.getLong(K_UTIL, Double.doubleToLongBits(99.5))); }
    public double tolerancePct() { return Double.longBitsToDouble(prefs.getLong(K_TOL, Double.doubleToLongBits(0.20))); }
    public boolean strictIntraday() { return prefs.getBoolean(K_STRICT, true); }
    public String exchange() { return prefs.getString(K_EXCHANGE, "NSE"); }

    public void save(boolean enabled, double capital, double utilization, double tolerance, boolean strict, String exchange) {
        prefs.edit()
                .putBoolean(K_ENABLED, enabled)
                .putLong(K_CAPITAL, Double.doubleToLongBits(capital))
                .putLong(K_UTIL, Double.doubleToLongBits(utilization))
                .putLong(K_TOL, Double.doubleToLongBits(tolerance))
                .putBoolean(K_STRICT, strict)
                .putString(K_EXCHANGE, exchange)
                .apply();
    }
}
