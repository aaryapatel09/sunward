# Sunward

A native Android sunscreen reminder for Google Pixel. Android 8.0 or later. No account or API key needed for personal use. Barcode scanning uses Google Play services, available on standard Google Pixels.

## Install

1. Open [the Sunward download page](https://sunward-sigma.vercel.app) on your Pixel and download the APK.
2. Open it on the phone. If Android asks, allow that browser/file manager to install this app, then install it. You can turn that install permission off afterward.
3. Open Sunward. Go to More → Reminder settings to enable notifications and precise timing (Alarms & reminders access).
4. Tap the sunscreen card, then **Scan barcode** (or **Enter barcode number**). Review any match, confirm SPF, broad-spectrum status and water resistance against your bottle, and save. Manual entry is always available.
5. Refresh your location. Approximate location is supported. Phone Location must be enabled.
6. After applying sunscreen, tap “I applied sunscreen.” The session starts only after notifications are enabled and a sunscreen profile is saved.
7. Tap “I reapplied” after each application, including directly from a reminder notification. End the outdoor session when finished.

This is a locally signed development build, not a Play Store release. It has been compiled and signature-verified, but has not been run on a physical Pixel or emulator. The first device check should confirm layout, location, exact-alarm permission and actual notification delivery.

## Minimal interface (version 1.3)

White background, black typography, a compact weather panel, a thin circular countdown and one primary application button. The circle represents time until the next reminder, not remaining sunscreen protection. Tap the sunscreen name to edit it. More contains weather details, reminder settings and help. During a session, the activity menu contains swimming/sweating and towel-drying actions. The adaptive launcher icon is a black outlined sun on white; Android themed icons follow the phone’s system palette.

Install version 1.3 over the previous APK to retain settings. This update uses the same signing key.

## Barcode scanning

- Tap the sunscreen card → **Scan barcode**. The scanner supports EAN-13, EAN-8, UPC-A, UPC-E and valid 14-digit ITF product barcodes. It checks the barcode checksum before requesting a lookup.
- Google Code Scanner reads the barcode on-device, without Sunward requesting camera permission. First use may download the Google Play services scanning module; the app shows progress and handles failed or delayed downloads.
- The barcode is looked up in Open Beauty Facts. No account, API key or paid service is configured. Network access is required for product lookup, and the database may not contain your sunscreen.
- A result is shown for review. **Use this product** fills the name and suggests SPF only when explicitly written in the product name. It clears the previous product’s broad-spectrum and water-resistance values. Confirm those details from your bottle and tick the label-check checkbox before saving.
- No result, an outage, scan cancellation or a rejected match leaves your saved sunscreen unchanged. **Enter barcode number** and fully manual entry are available as fallbacks. A failed scan never changes your running reminder.
- A 5-second minimum between lookups limits repeated requests. The app does not upload camera images, contribute product records, or build a barcode history.

## Behavior

- Normal outdoor reapplication: two hours after confirmed application. Higher SPF, low UV, clouds and cool weather never extend it.
- Swimming/heavy sweating: the earlier of the original due time or the saved 40/80-minute water-resistance interval from the activity start. No confirmed resistance makes reapplication due immediately.
- Finishing swimming, heavy sweating or toweling off makes reapplication due immediately. Confirming reapplication starts the next timer.
- Changing sunscreen cannot extend a running timer. Reapplying during water/sweat mode preserves that mode.
- A due notification offers “I reapplied” and “End session.” It does not assume you reapplied just because you dismissed it, and does not silently restart another two-hour period.
- UV >= 6 can trigger additional shade/clothing guidance during an active session, at most once per two hours. UV is an hourly model estimate, not a sensor measurement or burn-time calculation.
- Active sessions use an Android foreground location service with an ongoing notification. Location/weather updates are requested about every 15 minutes; timing and availability depend on Android and providers. Only the latest snapshot is retained.
- Without location or internet, timers still work; old/missing forecasts are labeled. Readings older than one hour are shown as stale.
- Timers restore after reboot; open the app to restart location tracking. Android force-stop disables reminders until the app is reopened. Without exact-alarm access, reminder delivery may be delayed. Notification permission/channel settings must remain enabled.

## Privacy and data

No account, advertising, Sunward analytics, backend or location history. Google Play services/ML Kit may collect their own SDK diagnostics; see [ML Kit data disclosure](https://developers.google.com/ml-kit/data-disclosure). Camera frames are processed on-device by Google Code Scanner. Barcode lookup sends the barcode number and request IP address to Open Beauty Facts over HTTPS, after a disclosure in the app. No camera images are sent to the product database. Sunscreen settings and the latest weather snapshot are stored in private app preferences, with Android backup disabled. Rounded coordinates (two decimal places) are sent over HTTPS to Open-Meteo, which also sees the request IP address. End the outdoor session to stop background location tracking. Uninstalling clears local data.

Weather data: [Open-Meteo](https://open-meteo.com/), [API documentation](https://open-meteo.com/en/docs), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The free endpoint is intended for non-commercial use; review provider terms before commercial distribution.

Guidance: [AAD sunscreen application](https://www.aad.org/public/everyday-care/sun-protection/shade-clothing-sunscreen/how-to-apply-sunscreen) and [FDA sunscreen guidance](https://www.fda.gov/drugs/understanding-over-counter-medicines/sunscreen-how-help-protect-your-skin-sun). This app follows label-based reminder rules; it does not estimate remaining protection or guarantee prevention of sunburn. Follow the product label.

Product data: [Open Beauty Facts](https://world.openbeautyfacts.org/), a community database, under [ODbL](https://opendatacommons.org/licenses/odbl/1-0/) (database) and [DbCL](https://opendatacommons.org/licenses/dbcl/1-0/) (individual contents). Matches and SPF suggestions must be checked against the actual product label.

## Build

Open this directory in Android Studio with **JDK 17** and Android SDK platform **36** / build-tools **36.0.0**. Gradle wrapper 8.13 and Android Gradle Plugin 8.13.0 are pinned.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Gradle resolves Google Code Scanner 16.1.0 and its transitive dependencies. No API keys are needed. The old direct aapt/javac build has been replaced: Gradle is needed to merge scanner resources and manifest entries correctly.

`build-local.sh` runs that build, tests and lint, and copies `Sunward.apk` to `BUILD_DIR` (default `build-local/`). Set `ANDROID_HOME` to your SDK and `JAVA_HOME` to JDK 17. It creates a development signing key under `BUILD_DIR` unless `SUNWARD_KEYSTORE` points to an existing compatible development key. Signing keys are not included in the repository. Keep the same signing key for future updates; a different key requires uninstalling the previous app and clears its data. Do not use these development signing settings for a production Play Store release.

## Verification completed

- Gradle APK compilation, dependency/resource merging, and APK signing verification.
- Android lint: no errors (warnings include existing localization and dependency-version suggestions).
- Nine JUnit test methods: barcode checksums/UPC-E expansion, leading-zero equivalence, explicit SPF parsing, successful/incomplete/mismatched/malformed API records, plus existing timer/UV checks and the water-mode deadline invariant.
- A live Open Beauty Facts product request returned a valid record; another request timed out. Lookup failure handling is included.
- The APK uses the same development certificate as versions 1.0–1.2.
- Camera scanning and notification delivery have not been verified on a physical Pixel. Emulator setup was attempted but could not finish because workspace disk space was insufficient; temporary emulator files were removed. Device testing remains necessary.

Suggested Pixel checks: open the sunscreen editor, scan a known sunscreen, cancel a scan, reject a match, enter an unknown barcode, disconnect internet, confirm the details before Save, and verify a running timer is not extended when switching products. Also test first-use scanner download and manually entering a barcode.

Gradle wrapper files retain their upstream Apache 2.0 license notices. Google Code Scanner is provided through Google Play services under its applicable SDK terms.
