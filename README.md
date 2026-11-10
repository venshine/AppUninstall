# AppUninstall

[中文](README-CN.md)

Android does not call your code when your app is uninstalled, and a process you leave behind is killed with the app. This repository collects the ways that **actually work today** to find out that an app was uninstalled, as runnable demos you can try on a device, plus the original 2016 `fork` + `inotify` demo for reference.

| Module | What it is |
|---|---|
| [`app`](app) | The demo app (`com.wx.appuninstall`): FCM token, Firebase Analytics `app_remove`, self-built heartbeat, and a comparison page with official sources |
| [`observer`](observer) | A second app (`com.wx.appuninstall.observer`) that listens for another package being removed and tells uninstall, archive and update apart |
| [`server`](server) | A local server (Kotlin / JVM, no third-party dependencies): token registry, heartbeat timeout, FCM HTTP v1 probe that recognises `UNREGISTERED` |
| [`legacy`](legacy) | The 2016 native-process demo, kept for reference; on modern Android use the approaches above, see [legacy/README.md](legacy/README.md) |
| [`common`](common) | Shared theme, fonts and UI helpers for `app` and `observer` |

## Download

APKs are attached to the [latest release](https://github.com/venshine/AppUninstall/releases/latest) (Android 6.0 / API 23 or later):

- `AppUninstall-sample-X.Y.apk`: the demo app. It is built without `google-services.json`, so FCM and Analytics show "not configured"; to try them, build from source with your own Firebase project (see [Try it](#try-it)).
- `AppUninstall-observer-X.Y.apk`: the observer app.
- `AppUninstall-legacy-X.Y.apk`: the 2016 demo.

## Screenshots

<img src="screenshot/demo.gif" width="300" alt="AppUninstall demo" />

| Home | Dark mode | FCM token | Analytics |
|:---:|:---:|:---:|:---:|
| <img src="screenshot/home.png" width="200" /> | <img src="screenshot/home_dark.png" width="200" /> | <img src="screenshot/fcm.png" width="200" /> | <img src="screenshot/analytics.png" width="200" /> |

| Heartbeat | Comparison | Observer | Observer event |
|:---:|:---:|:---:|:---:|
| <img src="screenshot/heartbeat.png" width="200" /> | <img src="screenshot/compare.png" width="200" /> | <img src="screenshot/observer.png" width="200" /> | <img src="screenshot/observer_event.png" width="200" /> |

The FCM token is partly masked. The 2016 demo's screenshots are in [legacy/README.md](legacy/README.md).

## Modern alternatives

There is no documented client-side way to run code at the moment of uninstall. What exists is detection after the fact:

| Approach | What you get | Limits | Source |
|---|---|---|---|
| FCM token becomes invalid | Sending to the token of an uninstalled app can return `UNREGISTERED` (HTTP 404); delete the token and mark the user as churned | Not immediate: only seen when you send. `UNREGISTERED` is documented as "token no longer valid", with uninstall as one cause among others (expired token, app update that does not receive messages). Android registrations inactive for 270 days expire; a registration is stale after a month without connecting | [Manage tokens](https://firebase.google.com/docs/cloud-messaging/manage-tokens), [Error codes](https://firebase.google.com/docs/cloud-messaging/error-codes), [ErrorCode](https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode) |
| Firebase Analytics `app_remove` | Android-only automatically collected event "when an application package is removed (uninstalled)", usable in reports and BigQuery | Aggregated, no per-user callback; counts removals from any installation source and differs from Play Console numbers; BigQuery data arrives with the daily sync | [Automatically collected events](https://support.google.com/analytics/answer/9234069), [BigQuery export](https://firebase.google.com/docs/projects/bigquery-export), [Export schema](https://support.google.com/analytics/answer/7029846) |
| An observer app listens to the removal broadcast | Near real time, per device: `PACKAGE_FULLY_REMOVED` is delivered to other apps (not to the package being removed) | The user must have the observer installed. See [observer](#the-observer-app) | [Intent reference](https://developer.android.com/reference/android/content/Intent), [Broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions) |
| Google Play Console statistics | User / device loss | Aggregates only, no per-user callback; "loss" also counts devices not used for over 30 days | [Play Console help](https://support.google.com/googleplay/android-developer/answer/139628) |
| Your own server heartbeat | Mark a user as likely churned after a timeout without a heartbeat | A heuristic: cannot tell "uninstalled" from "not using"; no official source (own design). WorkManager periodic work has a 15-minute minimum interval | [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) |

For collecting feedback, ask inside the app before the user leaves, or follow up by email / push with users you can still reach.

## Try it

### 1. What you need to do (Firebase)

The app and server build and run without any Firebase file; FCM and Analytics then show "not configured". To use them:

1. Create a project in the [Firebase console](https://console.firebase.google.com), and add an **Android app** with the package name `com.wx.appuninstall`. The `observer` and `legacy` modules do not use Firebase, so they do not need to be added.
2. Download `google-services.json` and put it in `.private/keys/google-services.json` (git-ignored). The build copies it to `app/google-services.json` (also git-ignored) and applies the `google-services` plugin only when the file exists.
3. For the server probe: Firebase console, Project settings, Service accounts, "Generate new private key". Save it as `.private/keys/firebase-service-account.json`. Never commit it.
4. Rebuild the app. The home page should show "Firebase configured".

### 2. Start the server

```bash
./gradlew :server:run
# optional environment variables:
#   PORT (8080)  BIND (127.0.0.1)  HEARTBEAT_TIMEOUT_SECONDS (1800)
#   FCM_SERVICE_ACCOUNT (path to the service account JSON, default .private/keys/firebase-service-account.json)
```

The server has no authentication and listens on loopback only. Let the phone reach it through the USB connection:

```bash
adb reverse tcp:8080 tcp:8080
```

([`adb reverse`](https://developer.android.com/tools/adb) is listed in `adb help`.) The app talks to `http://127.0.0.1:8080`; cleartext to loopback is allowed by `network_security_config.xml` ([docs](https://developer.android.com/privacy-and-security/security-config)).

| Endpoint | Purpose |
|---|---|
| `POST /register` `{"installId","token"}` | Register a device and its FCM token |
| `POST /heartbeat` `{"installId"}` | Record a heartbeat |
| `GET /devices` | Devices with status `ACTIVE` / `SUSPECTED_CHURN` / `UNINSTALLED` |
| `POST /probe` | Send a data-only message to every registered token; `UNREGISTERED` marks the device `UNINSTALLED`. Needs the service account |

Status rules: `UNINSTALLED` (FCM said `UNREGISTERED`) beats `SUSPECTED_CHURN` (no heartbeat for longer than the timeout) beats `ACTIVE`. Any later register or heartbeat clears the `UNINSTALLED` mark. A 404 without an `UNREGISTERED` detail (for example a wrong project id) and `INVALID_ARGUMENT` are **not** treated as uninstall. State is in memory only.

### 3. Walk through it

1. Install `app`, open "FCM token", tap "Get token", then "Send to local server".
2. Open "Self-built heartbeat" and start the foreground heartbeat; "Check server verdict" shows the status.
3. Uninstall the app, then `curl -X POST http://127.0.0.1:8080/probe`. FCM does not tell you at the moment of uninstall; it only fails when the server next sends. On a real device running Android 17, the first probe about 26 s after the uninstall returned `UNREGISTERED`, see [section 4](#4-what-we-saw-on-a-real-device).
4. Install `observer` first, then uninstall `app`: the observer lists `UNINSTALLED`.

### 4. What we saw on a real device

Tested on a real device running Android 17 with debug builds.

| Step | What happened |
|---|---|
| Register | The app fetched an FCM token and registered it; `POST /probe` returned `Delivered`, and the app's `onMessageReceived` ran (the app stores the receive time). The heartbeat reached the server and `GET /devices` showed `ACTIVE`. |
| Uninstall | Opened the system uninstall dialog with `adb shell am start -a android.intent.action.DELETE -d package:com.wx.appuninstall` and tapped Uninstall. The app's process was gone right after the tap. |
| About 26 s later | The first `POST /probe` after the uninstall returned `Unregistered`, and the server marked the device `UNINSTALLED`. |
| After the tap | No browser was launched: the foreground stayed in the system installer. |

Recording of the dialog and the result screen: [`screenshot/uninstall_dialog.gif`](screenshot/uninstall_dialog.gif).

Observer (installed, launched once, watching `com.wx.appuninstall`):

- With the observer in the background (the uninstall dialog was on top), the system held back `PACKAGE_FULLY_REMOVED` and no event was recorded.
- With the observer's screen open, `adb shell pm uninstall com.wx.appuninstall` produced `REMOVED_WITH_DATA` and `UNINSTALLED` in the same second.
- The observer's own "installed / not installed" line (package visibility via `<queries>`) was correct before and after the uninstall.

Analytics: with `adb shell setprop debug.firebase.analytics.app com.wx.appuninstall`, the test event `demo_open` shows up in the Firebase console under Analytics, DebugView. `app_remove` is logged when the app is uninstalled and appears in the console after a delay.

## The observer app

`observer` registers `android.intent.action.PACKAGE_FULLY_REMOVED` in its manifest and watches `com.wx.appuninstall` by default (editable in the app).

- **Which broadcast.** `PACKAGE_FULLY_REMOVED` is "only set when `EXTRA_DATA_REMOVED` is true and `EXTRA_REPLACING` is false" of `PACKAGE_REMOVED` ([Intent reference](https://developer.android.com/reference/android/content/Intent)). The package being removed does not receive `PACKAGE_REMOVED`.
- **Android 8 implicit broadcast limits.** The broadcast exception list names `PACKAGE_FULLY_REMOVED` (and `PACKAGE_DATA_CLEARED`) as exempt, and says other package broadcasts such as `PACKAGE_REPLACED` are not ([docs](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)). `PACKAGE_REMOVED` is not on the list, so the app registers it dynamically only while its screen is open.
- **Archive vs uninstall (Android 15).** `EXTRA_ARCHIVAL` (API 35) marks `PACKAGE_ADDED` / `PACKAGE_REMOVED` of an archive; archiving removes APKs and cache and **keeps user data** ([`PackageInstaller.requestArchive`](https://developer.android.com/reference/android/content/pm/PackageInstaller)). From these two sources it follows that an archive should not produce `PACKAGE_FULLY_REMOVED`, which needs data removed. The parser still checks `EXTRA_ARCHIVAL` first, so an archive is never recorded as an uninstall.
- **Package visibility (Android 11).** The manifest declares `<queries><package android:name="com.wx.appuninstall"/></queries>` so the app can check whether the target is installed ([docs](https://developer.android.com/training/package-visibility)). Whether visibility also gates delivery of the broadcast is **not stated in the official docs**; on the test device the broadcast arrived with the entry present.
- **Limits.** On the Android 17 test device the broadcast was held back while the observer was in the background (see section 4), so do not rely on the observer alone.

## Build and test

```bash
./gradlew :legacy:testDebugUnitTest :legacy:assembleDebug \
          :app:testDebugUnitTest :app:assembleDebug :app:lintDebug \
          :observer:testDebugUnitTest :observer:assembleDebug :observer:lintDebug \
          :server:test
```

AGP 9.4.1, Gradle 9.8.2, Kotlin 2.2.10, compileSdk / targetSdk 36, minSdk 23. Firebase BoM 34.19.0 is used on purpose: BoM 35.x ships libraries with Kotlin 2.4 metadata that this Kotlin version cannot read. WorkManager is 2.11.2 because 2.12 needs minSdk 24.

Unit tests cover the server verdict and FCM response parsing (offline, using responses built from the documented fields), the heartbeat timeout rule, the observer's broadcast parsing, and a Robolectric smoke test of every screen in light and dark.

## About
* Email: venshine.cn@gmail.com

## Credits

`app`, `observer` and `legacy` bundle these fonts, both licensed under the [SIL Open Font License 1.1](https://openfontlicense.org) (license texts in [`common/licenses/`](common/licenses)):

* [Space Grotesk](https://github.com/floriankarsten/space-grotesk): Copyright 2020 The Space Grotesk Project Authors
* [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono): Copyright 2020 The JetBrains Mono Project Authors

## License
    Copyright (C) 2016 venshine.cn@gmail.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
