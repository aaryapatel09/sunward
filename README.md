# Sunward

A native Android sunscreen reminder for Google Pixel. Android 8.0 or later. No account or API key needed for personal use.

## Install

1. Build the APK using the instructions below, then transfer it to your Pixel (USB, Google Drive, or another method you prefer).
2. Open it on the phone. If Android asks, allow that browser/file manager to install this app, then install it. You can turn that install permission off afterward.
3. Open Sunward. Go to More → Reminder settings to enable notifications and precise timing (Alarms & reminders access).
4. Add your sunscreen name, SPF, broad-spectrum status, and its label’s water resistance (none/unknown, 40 minutes, or 80 minutes).
5. Refresh your location. Approximate location is supported. Phone Location must be enabled.
6. After applying sunscreen, tap “I applied sunscreen.” The session starts only after notifications are enabled and a sunscreen profile is saved.
7. Tap “I reapplied” after each application, including directly from a reminder notification. End the outdoor session when finished.

This is a locally signed development build, not a Play Store release. It has been compiled and signature-verified, but has not been run on a physical Pixel or emulator. The first device check should confirm layout, location, exact-alarm permission and actual notification delivery.

## Minimal interface (version 1.1)

White background, black typography, local UV/weather, a large timer and one primary application button. Tap the sunscreen name to edit it. More contains weather details, reminder settings and help. During a session, the activity menu contains swimming/sweating and towel-drying actions. The adaptive launcher icon is a black outlined sun on white; Android themed icons follow the phone’s system palette.

Install version 1.1 over the previous APK to retain settings. This update uses the same signing key.

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

No account, advertising, analytics, backend or location history. Sunscreen settings and the latest weather snapshot are stored in private app preferences, with Android backup disabled. Rounded coordinates (two decimal places) are sent over HTTPS to Open-Meteo, which also sees the request IP address. End the outdoor session to stop background location tracking. Uninstalling clears local data.

Weather data: [Open-Meteo](https://open-meteo.com/), [API documentation](https://open-meteo.com/en/docs), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The free endpoint is intended for non-commercial use; review provider terms before commercial distribution.

Guidance: [AAD sunscreen application](https://www.aad.org/public/everyday-care/sun-protection/shade-clothing-sunscreen/how-to-apply-sunscreen) and [FDA sunscreen guidance](https://www.fda.gov/drugs/understanding-over-counter-medicines/sunscreen-how-help-protect-your-skin-sun). This app follows label-based reminder rules; it does not estimate remaining protection or guarantee prevention of sunburn. Follow the product label.

## Build

Android Studio project: open this directory, use JDK 17 and SDK 36, then run `./gradlew assembleDebug`. Gradle wrapper 8.13 and Android Gradle Plugin 8.13.0 are pinned. Android Studio may download SDK/build dependencies. The Gradle route is provided but was not executed in this environment.

The delivered APK was built with `build-local.sh`, SDK platform 36 and build-tools 36.0.0 using the installed JDK 25 (Java 8 bytecode). No third-party runtime libraries are used. Example with an existing SDK:

```sh
ANDROID_JAR="$ANDROID_HOME/platforms/android-36/android.jar" \
BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0" \
./build-local.sh
```

`BUILD_DIR` can redirect generated artifacts. The script creates a local development signing key in that build directory; retain it to produce updates compatible with that installation. Do not use this development key for a production release. Signing keys are not included in this repository. An APK signed by a different key requires uninstalling the previous app, which clears settings.

## Verification completed

- Resource compilation and complete Java compilation against Android SDK 36.
- D8 conversion and APK alignment.
- APK signature verification (v2 and v3).
- 17 rule/boundary checks plus 540 cases checking that water activity never extends the baseline two-hour deadline.
- Live weather endpoint smoke test: weather fields and hourly UV arrays returned successfully.
- No attached device or emulator was available. Notification delivery, lifecycle behavior, device location and visual layout still require device testing.

Suggested device checks: allow and deny location/notifications; start and end a session; trigger an immediate reminder using the “Swimming or sweating?” activity menu → “I toweled off”; confirm notification reapplication resets the timer; enable 40/80-minute water mode; change the product mid-session; turn off internet; reboot during a session; verify ending a session stops its location notification.

Gradle wrapper files are from the Gradle project and carry their upstream Apache 2.0 license notices.
