# Multyfi → Groww Approval Assistant (Android) v1.1

Native Android project for this workflow:

**Multyfi entry notification → parse intraday advisory → Groww LTP → calculate quantity → MARKET/LIMIT decision → APPROVE/REJECT → open Groww for final confirmation**

and for follow-up advisory updates:

**Multyfi EXIT / BOOK PROFIT / COST-TO-COST / REDUCE LOSS AND EXIT → verify current Groww MIS position → check for an already-pending opposite-side order → prepare urgent MARKET exit for only the remaining quantity → EXIT NOW / IGNORE → open Groww for final confirmation**

## Important boundary

This app does **not** place, modify, or cancel securities orders and contains no Groww order-submission API call. Approval copies the prepared ticket and opens Groww. The final broker action remains under the user's direct confirmation.

## Entry rules

- Reads notifications only from Multyfi package `com.multyfi.invest`.
- Entry alerts require BUY or SELL.
- Strict Intraday mode is ON by default and requires intraday/MIS wording for new entries.
- Positional, swing, delivery, investment, and long-term wording is blocked.
- Stop-loss is mandatory for entry alerts.
- CASH / NSE / MIS only in this build.
- Duplicate identical Multyfi alerts are blocked for the day.
- Capital utilization defaults to 99.5%.
- Entry tolerance defaults to 0.20%.
- BUY: inside range/tolerance → MARKET; above the advisory entry → LIMIT at the advisory high; materially below → manual review.
- SELL mirrors the same conservative entry logic.

## New urgent exit-update rules

Recognized strong full-exit phrases include variants of:

- `EXIT NOW`
- `BOOK PROFIT`
- `COST TO COST`
- `BREAKEVEN EXIT`
- `REDUCE LOSS AND EXIT`
- `CLOSE POSITION`
- `SQUARE OFF`

Before preparing an exit ticket, the app calls Groww's read-only position endpoint for the symbol and filters for `MIS` on the configured exchange.

- Groww MIS quantity = `0` or no open MIS position → **no exit ticket**; status says already closed / no open position.
- Partial position remaining → exit ticket quantity is **only the absolute remaining quantity**.
- Long MIS position → prepared exit side is `SELL`.
- Short MIS position → prepared exit side is `BUY`.
- Existing active opposite-side MIS order in Groww → duplicate exit ticket is blocked.
- Partial-exit wording such as `book partial profit` or `partial exit` is blocked for manual review rather than closing the full position.
- LTP is displayed when available, but a verified open MIS position is the critical safety check for an exit ticket.

## Groww API usage

The token is encrypted on the device using Android Keystore. It is used only for read operations:

- Live LTP
- Current position for trading symbol
- Current-day order list for duplicate-exit protection

There is no Groww place-order, modify-order, or cancel-order call in this project.

## Install/use

1. Build and install `app-debug.apk`.
2. Open the app.
3. Grant Notification Access to **Multyfi Trade Approval**.
4. Allow app notifications.
5. Set capital per trade and keep Strict intraday enabled initially.
6. Paste the current Groww Trading API access token and save it.
7. Use the built-in test entry ticket first.
8. For a real Multyfi entry alert, review the prepared ticket and confirm in Groww.
9. For a real Multyfi exit update, the app verifies the remaining Groww MIS quantity first, then shows an urgent exit approval only when a position still exists.

## GitHub Actions APK build

The included workflow is:

`.github/workflows/build-apk.yml`

Push this project to the repository root. GitHub Actions will build:

`app/build/outputs/apk/debug/app-debug.apk`

The downloadable artifact is named:

`MultyfiGrowwApproval-debug-apk`

## Version

- `versionCode 2`
- `versionName 1.1`
- Added advisory exit-follow-up handling, position verification, already-closed handling, remaining-quantity handling, active-exit duplicate protection, and partial-exit blocking.
