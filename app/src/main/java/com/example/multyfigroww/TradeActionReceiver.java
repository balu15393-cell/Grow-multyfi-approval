package com.example.multyfigroww;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class TradeActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String ticketId = intent.getStringExtra(ApprovalNotifier.EXTRA_TICKET_ID);
        if (ticketId == null) return;
        TradeStore store = new TradeStore(context);
        try {
            TradeTicket t = store.loadTicket(ticketId);
            if (t == null) return;
            ApprovalNotifier.cancelTicket(context, t);
            boolean exit = "EXIT".equals(t.ticketKind);

            if (ApprovalNotifier.ACTION_APPROVE.equals(intent.getAction())) {
                ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText(exit ? "Prepared MIS exit" : "Prepared MIS order", t.displayText()));
                store.appendLog(exit ? "EXIT_APPROVED_NOT_SUBMITTED" : "ENTRY_APPROVED_NOT_SUBMITTED", t,
                        "Ticket copied and Groww opened; no broker order was transmitted by this app.");
                openGroww(context);
            } else if (ApprovalNotifier.ACTION_REJECT.equals(intent.getAction())) {
                store.appendLog(exit ? "EXIT_IGNORED" : "ENTRY_REJECTED", t,
                        exit ? "User ignored the prepared exit ticket." : "User rejected the prepared entry ticket.");
            }
            store.deleteTicket(ticketId);
        } catch (Exception e) {
            ApprovalNotifier.showStatus(context, "Trade assistant", "Could not process approval: " + e.getMessage());
        }
    }

    private void openGroww(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.nextbillion.groww");
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
            return;
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://groww.in/stocks"));
        web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(web);
    }
}
