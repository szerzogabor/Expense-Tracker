# Expense Tracker

An offline-first personal finance Android app. All data is stored locally on the
device — no accounts, no cloud, no tracking. Built with Kotlin, Jetpack Compose
and Room. See [`APP.md`](APP.md) for the full product requirements.

## Features
- Expense, income, transfer and balance-adjustment transactions
- Accounts with icons/colors, default account, archiving, opening balances
- Categories (reorderable) with a protected **Other** fallback
- Category **learning rules** that *suggest* a category (never auto-apply)
- Free-text tags with AND/OR filtering and a dedicated tags screen
- Dashboard with a remembered period, balances, category summary and recents
- Transaction list grouped by date, description search, swipe edit/delete
- Reports by category / tag / account (income & expense)
- CSV export and import (replace or merge)
- Recurring transactions with full RFC-5545 RRULE support (auto / pending),
  including this / this-and-future / all edit scopes

## Install on your phone

**Recommended — from Releases (with checksum):**

1. On your phone open the [**latest release**](../../releases/latest) and download
   `expense-tracker.apk`.
2. Open the downloaded file. Android will ask to allow installing apps from this
   source — enable it, then tap **Install**.
3. Launch **Expense Tracker**.

The APK is also committed at `dist/expense-tracker.apk` and its SHA-256 at
`dist/expense-tracker.apk.sha256`. To verify integrity after download:

```
sha256sum expense-tracker.apk   # compare with the value in the release notes
```

> The APK is signed with a stable key committed in `keystore/release.jks`, so
> new builds install **over** the previous version without uninstalling.

### Antivirus / Play Protect "suspicious" warning
This is a **false positive** common to sideloaded, self-signed apps from outside
the Play Store — it has nothing to do with the app being unsafe. In fact the app
requests **no permissions at all** (not even internet), so it cannot send data
anywhere; the source is public and the build is reproducible. If your antivirus
(e.g. Avast) or Play Protect flags it, you can safely allow it, or report it as a
false positive (e.g. <https://www.avast.com/false-positive-file-form.php>).
Distribution via a GitHub Release + published checksum and a properly-identified
signing certificate reduce these warnings over time.

## How it's built
This repo can't build Android locally in every environment (Google's Maven/SDK
hosts are sometimes blocked), so the APK is built by **GitHub Actions**
(`.github/workflows/build-apk.yml`). On every push the workflow compiles
`assembleRelease`, then commits the result to `dist/expense-tracker.apk`
(the commit uses `[skip ci]` and the workflow ignores `dist/**` to avoid build
loops). GitHub Actions is free and unlimited for public repositories.

To build locally (with a normal Android SDK):

```bash
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```
