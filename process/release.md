# Release parameters (release-seal)

The generic steps live in the global `release-seal` skill. This file only records the parameters and exceptions for this project.

AppUninstall is a demo app, not a library: there is no JitPack coordinate and nothing to publish except the GitHub Release.

## Version

- Version `X.Y`: bump Y for build or documentation fixes, X only if the demo itself changes.
- **The version is bumped at release time.** Do not bump to the next version after sealing (skip the skill's "bump version" step).
- Update in the release commit: `versionName` and `versionCode` (+1) in `app/build.gradle`.

## Pre-release checks

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The unit tests only cover the sample screens. If the release claims anything about runtime behavior, it must have been checked on a real device and the Android version noted in the release notes.

## Tag

- The tag name is the bare version, without a `v` prefix: `1.1`
- Annotated tag with the message `AppUninstall X.Y`, created on the release commit and pushed to `origin`

## GitHub Release

- Title is the version number. Notes are written in English and must repeat that this is a 2016 demo that does not work reliably on modern Android
- Attachment: the demo APK, named `AppUninstall-sample-X.Y.apk`
  - Build: `./gradlew :app:assembleDebug`; output at `app/build/outputs/apk/debug/app-debug.apk`
  - Signed with the local debug certificate, the same one used by WheelView (SHA-256 `bc4b28be…1433982`, see `WheelView/process/release.md`). Verify the certificate first when releasing from another machine

## Afterwards

- There is no product doc or ROADMAP; the GitHub Release is the source of truth for release notes, so no other docs need syncing
