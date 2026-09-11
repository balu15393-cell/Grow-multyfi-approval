package com.example.multyfigroww;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText capital;
    private EditText utilization;
    private EditText tolerance;
    private EditText token;
    private Switch enabled;
    private CheckBox strict;
    private TextView status;
    private EditText testAlert;
    private EditText testLtp;
    private TextView result;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApprovalNotifier.createChannels(this);
        setContentView(buildUi());
        loadSettings();
        requestNotificationPermissionIfNeeded();
        updateStatus();
    }

    private View buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = text("Multyfi → Groww Approval Assistant", 22, true);
        root.addView(title);
        TextView subtitle = text(
                "Reads Multyfi notifications, prepares MIS intraday entries, and recognizes urgent advisory exit updates. " +
                        "Before an exit ticket it verifies the current Groww MIS position. It does not submit securities orders to Groww.", 14, false);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        status = text("", 14, true);
        root.addView(status);

        Button access = button("1. Grant Notification Access");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access);

        enabled = new Switch(this);
        enabled.setText("Monitor Multyfi notifications");
        enabled.setPadding(0, dp(10), 0, dp(4));
        root.addView(enabled);

        strict = new CheckBox(this);
        strict.setText("Strict intraday only (recommended)");
        strict.setChecked(true);
        root.addView(strict);

        root.addView(label("Capital per trade (₹)"));
        capital = number("25000");
        root.addView(capital);

        root.addView(label("Capital utilization (%)"));
        utilization = decimal("99.5");
        root.addView(utilization);

        root.addView(label("Entry tolerance (%)"));
        tolerance = decimal("0.20");
        root.addView(tolerance);

        root.addView(label("Groww access token (encrypted on this device)"));
        token = new EditText(this);
        token.setSingleLine(true);
        token.setHint("Paste today's Groww Trading API access token");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(token);

        TextView tokenNote = text(
                "Groww access tokens expire daily. The app uses the token to read official Groww LTP, MIS positions, and current-day orders for duplicate-exit protection. It contains no order-submission code.",
                12, false);
        tokenNote.setPadding(0, dp(4), 0, dp(8));
        root.addView(tokenNote);

        Button save = button("2. Save Settings / Token");
        save.setOnClickListener(v -> saveSettings());
        root.addView(save);

        Button clearToken = button("Clear saved Groww token");
        clearToken.setOnClickListener(v -> {
            new TokenStore(this).clear();
            token.setText("");
            toast("Saved token cleared.");
            updateStatus();
        });
        root.addView(clearToken);

        TextView testHeader = text("Test before using real Multyfi notifications", 18, true);
        testHeader.setPadding(0, dp(18), 0, dp(4));
        root.addView(testHeader);

        testAlert = new EditText(this);
        testAlert.setMinLines(4);
        testAlert.setGravity(android.view.Gravity.TOP);
        testAlert.setText("MULTYFI INTRADAY\nBUY IRFC\nEntry: 82\nTarget: 85\nSL: 80.50");
        root.addView(testAlert);

        root.addView(label("Manual test LTP (₹)"));
        testLtp = decimal("82.05");
        root.addView(testLtp);

        Button test = button("3. Create Test Approval Notification");
        test.setOnClickListener(v -> runTest());
        root.addView(test);

        result = text("", 14, false);
        result.setTypeface(Typeface.MONOSPACE);
        result.setTextIsSelectable(true);
        result.setPadding(0, dp(12), 0, dp(16));
        root.addView(result);

        Button openGroww = button("Open Groww");
        openGroww.setOnClickListener(v -> TradeActionReceiverOpen.open(this));
        root.addView(openGroww);

        TextView safety = text(
                "Safety rules: Multyfi package only • entries require BUY/SELL + stop-loss • MIS only • duplicate alerts blocked • " +
                        "urgent exit updates verify the remaining Groww MIS quantity first • already-closed and already-pending exits are blocked • " +
                        "partial-exit wording requires manual review.",
                12, false);
        safety.setPadding(0, dp(14), 0, dp(20));
        root.addView(safety);
        return scroll;
    }

    private void loadSettings() {
        SettingsRepo s = new SettingsRepo(this);
        enabled.setChecked(s.isEnabled());
        strict.setChecked(s.strictIntraday());
        capital.setText(String.format(Locale.ROOT, "%.2f", s.capital()));
        utilization.setText(String.format(Locale.ROOT, "%.2f", s.utilizationPct()));
        tolerance.setText(String.format(Locale.ROOT, "%.2f", s.tolerancePct()));
        if (new TokenStore(this).hasToken()) token.setHint("Token is saved securely — paste a new one to replace it");
    }

    private void saveSettings() {
        try {
            double c = parsePositive(capital.getText().toString(), "Capital");
            double u = parsePositive(utilization.getText().toString(), "Utilization");
            double t = Double.parseDouble(tolerance.getText().toString().trim());
            if (u > 100) throw new IllegalArgumentException("Utilization cannot exceed 100%.");
            if (t < 0 || t > 5) throw new IllegalArgumentException("Tolerance must be 0–5%.");
            new SettingsRepo(this).save(enabled.isChecked(), c, u, t, strict.isChecked(), "NSE");
            String tokenText = token.getText().toString().trim();
            if (!tokenText.isEmpty()) {
                new TokenStore(this).save(tokenText);
                token.setText("");
                token.setHint("Token saved securely — paste a new one to replace it");
            }
            toast("Settings saved.");
            updateStatus();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    private void runTest() {
        try {
            double c = parsePositive(capital.getText().toString(), "Capital");
            double u = parsePositive(utilization.getText().toString(), "Utilization");
            double t = Double.parseDouble(tolerance.getText().toString().trim());
            double ltp = parsePositive(testLtp.getText().toString(), "Test LTP");
            Signal signal = SignalParser.parse("Test Multyfi alert", testAlert.getText().toString(), strict.isChecked());
            TradeTicket ticket = TradeDecision.build(signal, ltp, c, u, t, "NSE");
            new TradeStore(this).saveTicket(ticket);
            new TradeStore(this).appendLog("TEST_TICKET_PREPARED", ticket, ticket.reason);
            ApprovalNotifier.showApproval(this, ticket);
            result.setText(ticket.displayText());
            toast("Test approval notification created.");
        } catch (Exception e) {
            result.setText("BLOCKED: " + e.getMessage());
            toast(e.getMessage());
        }
    }

    private void updateStatus() {
        boolean listener = notificationListenerEnabled();
        boolean notifications = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean hasToken = new TokenStore(this).hasToken();
        status.setText("Notification access: " + (listener ? "ON" : "OFF") +
                "\nApp notifications: " + (notifications ? "ON" : "OFF") +
                "\nGroww token: " + (hasToken ? "SAVED" : "MISSING"));
    }

    private boolean notificationListenerEnabled() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private TextView label(String s) {
        TextView v = text(s, 13, true);
        v.setPadding(0, dp(10), 0, 0);
        return v;
    }

    private EditText number(String defaultValue) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(defaultValue);
        return e;
    }

    private EditText decimal(String defaultValue) { return number(defaultValue); }

    private double parsePositive(String raw, String name) {
        double v = Double.parseDouble(raw.trim());
        if (v <= 0) throw new IllegalArgumentException(name + " must be above zero.");
        return v;
    }

    private int dp(int x) {
        return (int) (x * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s == null ? "Unknown error" : s, Toast.LENGTH_LONG).show();
    }

    // Small helper to keep Groww launch logic available from the main screen.
    public static final class TradeActionReceiverOpen {
        public static void open(Activity a) {
            Intent launch = a.getPackageManager().getLaunchIntentForPackage("com.nextbillion.groww");
            if (launch != null) a.startActivity(launch);
            else a.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://groww.in/stocks")));
        }
    }
}
