# Release parameters (release-seal)

The generic steps live in the global `release-seal` skill. This file only records the parameters and exceptions for this project.

AppUninstall is a set of demo apps plus a local server, not a library: there is no JitPack coordinate and nothing to publish except the GitHub Release.

## Version

- Version `X.Y`: bump Y for build or documentation fixes, X only if the demos themselves change. `app`, `observer` and `legacy` share the same `versionName` and `versionCode`.
- **The version is bumped at release time.** Do not bump to the next version after sealing (skip the skill's "bump version" step).
- Update in the release commit: `versionName` and `versionCode` (+1) in `app/build.gradle`, `observer/build.gradle` and `legacy/build.gradle`.

## Pre-release checks

```bash
./gradlew :legacy:testDebugUnitTest :legacy:assembleDebug :legacy:lintDebug \
          :app:testDebugUnitTest :app:assembleDebug :app:lintDebug \
          :observer:testDebugUnitTest :observer:assembleDebug :observer:lintDebug \
          :server:test
```

The unit tests cover the server verdict logic, the heartbeat timeout, the observer's broadcast parsing and smoke tests of the screens. Anything the release claims about runtime behavior (FCM delivery, `UNREGISTERED` after an uninstall, the observer receiving `PACKAGE_FULLY_REMOVED`, package visibility, archive behavior) must have been checked on a real device with a real Firebase project, and the Android version noted in the release notes. Build the attached APKs from a clean checkout without `google-services.json` unless the owner decides to ship one built with their Firebase project.

## Tag

- The tag name is the bare version, without a `v` prefix: `2.0`
- Annotated tag with the message `AppUninstall X.Y`, created on the release commit and pushed to `origin`

## GitHub Release

- Title is the version number. Notes are written in English
- Attachments, one APK per app (build with `./gradlew :app:assembleDebug :observer:assembleDebug :legacy:assembleDebug`):
  - `AppUninstall-sample-X.Y.apk` from `app/build/outputs/apk/debug/app-debug.apk`
  - `AppUninstall-observer-X.Y.apk` from `observer/build/outputs/apk/debug/observer-debug.apk`
  - `AppUninstall-legacy-X.Y.apk` from `legacy/build/outputs/apk/debug/legacy-debug.apk`
  - The sample APK built without `google-services.json` shows FCM / Analytics as "not configured"; say so in the notes
  - Signed with the local debug certificate, the same one used by WheelView (SHA-256 `bc4b28be…1433982`, see `WheelView/process/release.md`). Verify the certificate first when releasing from another machine
- The release notes must repeat that `legacy` is a 2016 demo that does not work reliably on modern Android

## Afterwards

- There is no product doc or ROADMAP; the GitHub Release is the source of truth for release notes, so no other docs need syncing
