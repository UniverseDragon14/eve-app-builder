# UDOS Mobile Launcher Test Checklist

Use this checklist before setting UDOS Mobile as the default Home app.

## Safe Test Device

Recommended first device:

```text
Moto G22
```

Avoid using a daily personal phone for the first launcher test.

## Before Install

- Confirm APK is a debug/test build.
- Confirm original launcher is still installed.
- Confirm Android Settings can be opened.
- Confirm the phone has enough battery.
- Keep USB cable available.

## First Launch Test

1. Install UDOS Mobile APK.
2. Open UDOS Mobile from app drawer.
3. Confirm the screen loads.
4. Tap Settings.
5. Tap Apps.
6. Tap Camera.
7. Tap Speak.
8. Tap Voice after granting microphone permission.

## Launcher Test

1. Press Home.
2. Android should ask for Home app.
3. Choose UDOS Mobile.
4. Select "Just once" first.
5. Test Home screen behavior.
6. Only choose "Always" after repeated success.

## Revert Path

If something feels wrong:

```text
Settings -> Apps -> Default apps -> Home app -> original launcher
```

## Safety Rules

- No root required.
- No custom ROM required.
- Android stays underneath.
- Revert path must always work.
- Do not hide Settings.
- Do not block emergency access.

## Test Result Notes

Write test results here later:

```text
Device:
APK version:
Home test:
Voice test:
Settings access:
Revert test:
Problems:
```
