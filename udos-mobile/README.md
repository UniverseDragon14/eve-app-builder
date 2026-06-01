# UDOS Mobile

UDOS Mobile is the Android launcher prototype for the Universal Dragon assistant-first phone direction.

It is the app version of UDOS, separate from the public `udos-site` website.

## Meaning

```text
udos-site = public website / concept face
udos-mobile = Android launcher / phone control layer prototype
```

## Current Purpose

UDOS Mobile is designed to become a safe default launcher layer where Android stays underneath and EVE becomes the front control brain.

## Current Features

- Android launcher intent support
- home/default launcher intent support
- embedded local UDOS mobile UI
- EVE command bridge from WebView to Android
- text-to-speech response support
- microphone permission support
- one-command voice listen mode
- wake/sleep control buttons
- camera/settings/apps shortcuts
- Pi brain status command hook
- foreground wake standby service

## Launcher Behavior

The Android manifest includes both:

```text
LAUNCHER
HOME + DEFAULT
```

This means Android can offer UDOS Mobile as a Home app.

To use safely:

1. Install debug APK on test phone.
2. Press Home.
3. Choose UDOS Mobile.
4. Select "Always" only after testing.
5. Revert from Android Settings if needed.

## Safe Test Device

Recommended first test device:

```text
Moto G22
```

Do not use a daily personal phone as the first launcher test.

## Safety Rules

- No root required
- No custom ROM required
- Android stays underneath
- Easy revert must remain possible
- No private keys in app source
- No unrestricted terminal control
- No risky action without Aslam approval

## Build

This project is built from the `udos-mobile` folder.

```bash
cd udos-mobile
./gradlew assembleDebug
```

GitHub Actions also has a UDOS Mobile Android build workflow.

## Current Status

Small launcher foundation active.

Next target: improve status handling, backend offline fallback, and safe launcher documentation before deeper mobile automation.
