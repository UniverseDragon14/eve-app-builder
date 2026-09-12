# Existing UDOS Mobile voice repair

This change repairs the Android app `com.universaldragon.udosmobile`. It keeps the existing launcher and visual layout. The baseline is `c93166f6879a419834a14abbda70d8d280dd8738`; its Android source is identical to `bd1715ca63ac39d2cb8fdd9919907f066437f644`.

## Why this repair is needed

Six scripts redefined the same controls, including a final Voice override that always displayed a missing-engine message. Two earlier script syntax errors prevented functions from loading. Wake requested microphone permission twice with different request codes. Recognition used substring matching, ignored its wake-mode flag and matched the wake name before the rest of the command. Speech recognition could restart during spoken output and after the activity was hidden. The foreground service only posted a standby notification; it did not listen and lacked the foreground-service permissions it required.

## Resulting behavior

- One JavaScript owner handles the existing controls. Send reaches the Android bridge.
- Android speech/TTS service discovery is declared. Microphone permission is requested once and its result resumes the selected operation.
- Voice listens for one command. Wake starts a visible, two-minute session. Say `Hey Nova, open camera`, or `Hey Nova` followed by one command within ten seconds of the spoken prompt.
- Recognition pauses during TTS. Stale recognition callbacks and old TTS callbacks cannot restart a newer session. Sleep, Back, activity stop and destruction release recognition resources.
- Silence may retry within the session. Network/busy errors have three retries; permission, language and request-limit failures stop with an explicit result.
- The Android on-device recognizer is preferred where available. Otherwise the installed default recognizer is used; it may send audio online. There is no bundled speech model or guarantee of offline language support.
- Tools → Voice Check shows microphone permission, recognition service availability, chosen language and English/Tamil TTS support. Settings allows English (`en-US`) or Tamil (`ta-IN`) recognition. This selects a language; it does not install language data.
- The notification-only standby service is removed. It never provided background speech recognition. This repair does not claim background wake or a custom wake-word engine.
- WebView loads local assets and blocks remote page loads. Website actions open the external browser. Cleartext traffic is disabled. Input and provider responses are bounded; only an explicit Pi command uses the existing EVE endpoint.

## Commands

| Input | Action |
|---|---|
| `camera` / `open camera` / `கேமரா` / `கேமரா திற` | Open Android camera; does not take a photo |
| `settings` / `phone settings` / `அமைப்புகள்` | Open Android settings |
| `apps` / `open apps` | Open Android apps settings |
| `website` / `live udos` | Open the existing UDOS website |
| `status` | Local launcher status |
| `voice check` | Inspect actual speech capabilities |
| `voice english` / `voice tamil` | Select the recognition language |
| `speak Vanakkam Aslam` | Speak the supplied text |
| `tools` / `map` | Show existing tools or project roles |
| `pi status` | Explicit optional request to the configured EVE endpoint |
| `sleep` / `stop listening` / `நிறுத்து` | Stop recognition |

Wake mode requires a recognized prefix (`Hey Nova`, `Hey Dragon`, `Hey EVE`, or the corresponding supported Tamil name), except for the bounded follow-up window and stop commands. Unknown text never becomes shell commands or arbitrary Android actions. For example, `do not open camera` and `open camera and delete files` are rejected.

## Verification

Run from the repository root:

```bash
bash udos-mobile/tests/check.sh
```

This compiles the real Java command router and checks 30 command/wake cases, then runs 10 UI regression tests against the actual JavaScript and HTML handlers. It requires Java 17 with the compiler module and Node 22. These are host-side tests; they do not prove microphone capture or TTS playback on a handset.

The Android workflow pins Gradle 8.9, runs those checks, then runs `assembleDebug lintDebug` with Java 17 and the existing Android SDK 34 configuration. An APK artifact is a build result, not phone acceptance.

## Device acceptance still required

1. Record the installed app version and signing certificate before attempting an update. This source build has version code 4 and version name `0.3.1-voice-repair`.
2. Use a build signed with the existing app's certificate for an in-place update. A fresh CI debug key will generally differ. Do not uninstall the current launcher or discard its data to bypass a signature mismatch.
3. Open Tools → Voice Check. Record actual recognition service and TTS language availability. Missing speech services or Tamil language data require device setup; a UI patch cannot supply an absent engine.
4. Deny microphone permission and verify text controls remain usable. Grant it and verify one tap of Voice captures and executes one command.
5. Verify `Hey Nova, open camera`, the two-part wake command, spoken replies without microphone feedback, Sleep and the session deadline.
6. Verify an unsupported/negated command cannot open an app. Switch away, lock the screen, rotate or close UDOS and confirm it does not keep listening or restart from a stale callback.
7. Confirm Android Settings and changing the default Home app remain accessible. Test with Pi5 disconnected.

General conversational intelligence, cross-app accessibility actions, background hotword detection, telephony and message sending remain separate work. This repair implements none of those by pretending a launcher bridge has unrestricted device control.

## Primary references

- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer): main-thread/lifecycle requirements and continuous recognition limits.
- [Android UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener): asynchronous speech completion callbacks.
- [Android Gradle plugin 8.7 compatibility](https://developer.android.com/build/releases/agp-8-7-0-release-notes): Java 17, Gradle 8.9 and SDK build tools 34.
