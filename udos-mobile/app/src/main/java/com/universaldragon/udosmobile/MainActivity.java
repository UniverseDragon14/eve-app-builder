package com.universaldragon.udosmobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_MIC = 44;
    private static final long WAKE_LIMIT_MS = 120000;
    private static final String ASSET_URL = "file:///android_asset/udos.html";
    private static final String UDOS_URL = "https://udos.universaldragon.com/";
    private static final String PI_BRAIN_BASE = "https://eve.universaldragon.com/api/context?q=";

    private WebView web;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean visible;
    private boolean destroyed;
    private boolean pageReady;
    private boolean wakeMode;
    private boolean listening;
    private boolean permissionPending;
    private boolean permissionWake;
    private boolean permissionPrompt;
    private boolean ttsReady;
    private boolean piBusy;
    private int recognizerEpoch;
    private int requestEpoch;
    private int transientErrors;
    private int utteranceSequence;
    private long awaitingUntil;
    private String activeUtterance;
    private String languageTag = "en-US";
    private String lastOutput = "";
    private String lastReply = "UDOS Mobile is ready.";

    private final Runnable restartListening = new Runnable() {
        @Override public void run() {
            if (visible && wakeMode && !listening && activeUtterance == null) beginListening(true);
        }
    };
    private final Runnable wakeDeadline = new Runnable() {
        @Override public void run() { stopWakeMode("Wake session finished. Tap Wake to start again."); }
    };
    private final Runnable recognitionDeadline = new Runnable() {
        @Override public void run() {
            if (listening) recognitionError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
        }
    };
    private final Runnable speechDeadline = new Runnable() {
        @Override public void run() {
            stopSpeech();
            updateOutput(lastOutput + "<br><b>VOICE:</b> Speech output timed out.");
            scheduleWake(900);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languageTag = getPreferences(MODE_PRIVATE).getString("voice_language", "en-US");
        if (!"ta-IN".equals(languageTag)) languageTag = "en-US";
        getWindow().setStatusBarColor(Color.rgb(3, 6, 18));
        getWindow().setNavigationBarColor(Color.rgb(3, 6, 18));
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setTextZoom(100);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        web.setBackgroundColor(Color.rgb(3, 6, 18));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (ASSET_URL.equals(request.getUrl().toString())) return false;
                String scheme = request.getUrl().getScheme();
                if (request.isForMainFrame() && request.hasGesture()
                        && ("https".equals(scheme) || "http".equals(scheme))) {
                    launch(new Intent(Intent.ACTION_VIEW, request.getUrl()), "website");
                }
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !ASSET_URL.equals(url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = ASSET_URL.equals(url);
                if (pageReady && !lastOutput.isEmpty()) updateOutput(lastOutput);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new UdosBridge(), "UDOS");
        setContentView(web);
        tts = new TextToSpeech(this, status -> handler.post(() -> initializeSpeech(status)));
        web.loadUrl(ASSET_URL);
    }

    private void initializeSpeech(int status) {
        if (destroyed || tts == null) return;
        ttsReady = status == TextToSpeech.SUCCESS;
        if (!ttsReady) return;
        tts.setSpeechRate(0.95f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { speechFinished(id, false); }
            @Override public void onError(String id) { speechFinished(id, true); }
            @Override public void onError(String id, int code) { speechFinished(id, true); }
            @Override public void onStop(String id, boolean interrupted) { speechFinished(id, false); }
        });
    }

    public class UdosBridge {
        @JavascriptInterface public void openSite() { command("website"); }
        @JavascriptInterface public void openSettings() { command("settings"); }
        @JavascriptInterface public void openApps() { command("apps"); }
        @JavascriptInterface public void openCamera() { command("camera"); }
        @JavascriptInterface public void listen() { runOnUiThread(() -> requestVoice(false)); }
        @JavascriptInterface public void wake() { runOnUiThread(() -> requestVoice(true)); }
        @JavascriptInterface public void sleep() {
            runOnUiThread(() -> stopWakeMode("Wake mode stopped."));
        }
        @JavascriptInterface public void speak(String message) {
            runOnUiThread(() -> speakNow(message));
        }
        @JavascriptInterface public void command(String message) {
            runOnUiThread(() -> handleCommand(message, false));
        }
        @JavascriptInterface public void voiceStatus() { runOnUiThread(() -> showVoiceStatus()); }
        @JavascriptInterface public void setVoiceLanguage(String tag) {
            runOnUiThread(() -> changeLanguage(tag));
        }
        @JavascriptInterface public void toast(String message) {
            runOnUiThread(() -> {
                if (visible && !destroyed) Toast.makeText(MainActivity.this, bounded(message), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean onDeviceAvailable() {
        return Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
    }

    private boolean recognitionAvailable() {
        return onDeviceAvailable() || SpeechRecognizer.isRecognitionAvailable(this);
    }

    private void requestVoice(boolean wake) {
        requestVoice(wake, false);
    }

    private void requestVoice(boolean wake, boolean prompt) {
        if (!visible || destroyed) return;
        requestEpoch++;
        permissionWake = wake;
        permissionPrompt = prompt;
        if (!recognitionAvailable()) {
            stopVoiceSession();
            updateOutput("<b>VOICE:</b> No compatible Android speech recognition service was found. "
                    + "Enable a compatible speech service in phone settings, then run Voice Check. "
                    + "Text commands still work. Microphone permission alone cannot provide recognition.");
            return;
        }
        if (!hasMicPermission()) {
            if (!permissionPending) {
                permissionPending = true;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            }
            return;
        }
        startVoiceRequest(wake);
    }

    private void startVoiceRequest(boolean wake) {
        if (!visible || destroyed) return;
        stopVoiceSession();
        stopSpeech();
        transientErrors = 0;
        wakeMode = wake;
        if (wake) {
            handler.postDelayed(wakeDeadline, WAKE_LIMIT_MS);
            updateOutput("<b>WAKE:</b> Two-minute session while UDOS is visible. Say "
                    + "<b>Hey Nova, open camera</b>, or say <b>Hey Nova</b> and then a command. "
                    + "Say <b>sleep</b> to stop.<br>" + recognizerDescription());
            if (permissionPrompt) awaitingUntil = SystemClock.elapsedRealtime() + 10000;
            speakNow(permissionPrompt ? "What should I open?"
                    : "Wake session ready. Say Hey Nova followed by your command.");
            scheduleWake(900);
        } else {
            beginListening(false);
        }
    }

    private void beginListening(final boolean fromWake) {
        if (!visible || destroyed || permissionPending || activeUtterance != null) return;
        if (fromWake && !wakeMode) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || !recognitionAvailable()) {
            stopVoiceSession();
            showVoiceStatus();
            return;
        }
        stopRecognizer();
        final int epoch = recognizerEpoch;
        try {
            recognizer = Build.VERSION.SDK_INT >= 31 && onDeviceAvailable()
                    ? SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                    : SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                private boolean current() { return epoch == recognizerEpoch && listening && visible && !destroyed; }
                @Override public void onReadyForSpeech(Bundle params) {
                    if (current()) updateOutput("<b>VOICE:</b> Listening in " + escapeHtml(languageTag)
                            + (fromWake ? ". Say Hey Nova and a command." : ". Say one command.")
                            + "<br>" + recognizerDescription());
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle results) {}
                @Override public void onEvent(int eventType, Bundle params) {}
                @Override public void onError(int error) { if (current()) recognitionError(error); }
                @Override public void onResults(Bundle results) {
                    if (!current()) return;
                    ArrayList<String> values = results == null ? null
                            : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String heard = values == null || values.isEmpty() ? "" : values.get(0);
                    stopRecognizer();
                    transientErrors = 0;
                    handleCommand(heard, fromWake);
                    scheduleWake(900);
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            listening = true;
            handler.postDelayed(recognitionDeadline, 20000);
            recognizer.startListening(intent);
        } catch (RuntimeException error) {
            stopVoiceSession();
            updateOutput("<b>VOICE:</b> Speech service could not start. Check microphone permission "
                    + "and the selected speech language using Voice Check.");
        }
    }

    private void recognitionError(int error) {
        stopRecognizer();
        boolean silence = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
        boolean retryable = error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_SERVER
                || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED;
        if (wakeMode && silence) {
            scheduleWake(900);
            return;
        }
        if (wakeMode && retryable && ++transientErrors <= 3) {
            updateOutput("<b>VOICE:</b> " + speechErrorName(error) + ". Retry " + transientErrors + " of 3.");
            scheduleWake(900L << transientErrors);
            return;
        }
        stopVoiceSession();
        updateOutput("<b>VOICE:</b> " + speechErrorName(error) + ". Listening stopped. "
                + "Use Voice Check, then tap Voice or Wake to retry.");
    }

    private String speechErrorName(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording failed";
            case SpeechRecognizer.ERROR_CLIENT: return "Speech client failed";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission denied";
            case SpeechRecognizer.ERROR_NETWORK: return "Speech service network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Speech service network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech matched";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Speech recognizer busy";
            case SpeechRecognizer.ERROR_SERVER: return "Speech server error";
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED: return "Speech service disconnected";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech heard";
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS: return "Speech service request limit reached";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED: return "Selected speech language is unsupported";
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: return "Selected speech language is not installed";
            default: return "Speech error " + error;
        }
    }

    private void stopRecognizer() {
        recognizerEpoch++;
        listening = false;
        handler.removeCallbacks(recognitionDeadline);
        SpeechRecognizer old = recognizer;
        recognizer = null;
        if (old != null) {
            try { old.cancel(); } catch (RuntimeException ignored) {}
            try { old.destroy(); } catch (RuntimeException ignored) {}
        }
    }

    private void scheduleWake(long delay) {
        handler.removeCallbacks(restartListening);
        if (wakeMode && visible && activeUtterance == null && !destroyed) {
            handler.postDelayed(restartListening, delay);
        }
    }

    private void stopVoiceSession() {
        wakeMode = false;
        awaitingUntil = 0;
        handler.removeCallbacks(restartListening);
        handler.removeCallbacks(wakeDeadline);
        stopRecognizer();
    }

    private void stopWakeMode(String message) {
        permissionPending = false;
        stopVoiceSession();
        stopSpeech();
        if (visible && !destroyed) reply(message);
    }

    private void stopSpeech() {
        activeUtterance = null;
        handler.removeCallbacks(speechDeadline);
        if (tts != null) tts.stop();
    }

    private void speakNow(String message) {
        if (!visible || destroyed) return;
        stopRecognizer();
        handler.removeCallbacks(restartListening);
        stopSpeech();
        String text = bounded(message);
        if (text.isEmpty()) text = lastReply;
        if (!ttsReady || tts == null) {
            updateOutput(lastOutput + "<br><b>SPEAK:</b> Text-to-speech is unavailable or still starting. Use Voice Check.");
            scheduleWake(900);
            return;
        }
        Locale language = text.matches("(?s).*[\\u0B80-\\u0BFF].*") ? Locale.forLanguageTag("ta-IN") : Locale.US;
        if (tts.setLanguage(language) < TextToSpeech.LANG_AVAILABLE) {
            updateOutput(lastOutput + "<br><b>SPEAK:</b> Speech data for this reply language is unavailable.");
            scheduleWake(900);
            return;
        }
        String id = "UDOS_" + (++utteranceSequence);
        activeUtterance = id;
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            activeUtterance = null;
            updateOutput(lastOutput + "<br><b>SPEAK:</b> Speech output failed to start.");
            scheduleWake(900);
            return;
        }
        handler.postDelayed(speechDeadline, 45000);
    }

    private void speechFinished(String id, boolean failed) {
        handler.post(() -> {
            if (destroyed || id == null || !id.equals(activeUtterance)) return;
            activeUtterance = null;
            handler.removeCallbacks(speechDeadline);
            if (awaitingUntil > 0) awaitingUntil = SystemClock.elapsedRealtime() + 10000;
            if (failed && visible) updateOutput(lastOutput + "<br><b>SPEAK:</b> Speech engine reported a playback error.");
            scheduleWake(900);
        });
    }

    private void handleCommand(String input, boolean fromWake) {
        if (!visible || destroyed) return;
        CommandRouter.Command command = CommandRouter.parse(input, fromWake,
                awaitingUntil > SystemClock.elapsedRealtime());
        if (command.action == CommandRouter.Action.IGNORE) return;
        awaitingUntil = 0;
        requestEpoch++;
        switch (command.action) {
            case PROMPT:
                if (!wakeMode) {
                    requestVoice(true, true);
                } else {
                    awaitingUntil = SystemClock.elapsedRealtime() + 10000;
                    reply("What should I open?");
                }
                return;
            case WAKE: requestVoice(true); return;
            case SLEEP: stopWakeMode("Wake mode stopped."); return;
            case LISTEN: requestVoice(false); return;
            case SPEAK: reply(command.text); return;
            case READ: speakNow(lastReply); return;
            case CAMERA: reply(launch(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "camera")); return;
            case SETTINGS: reply(launch(new Intent(Settings.ACTION_SETTINGS), "phone settings")); return;
            case APPS: reply(launch(new Intent(Settings.ACTION_APPLICATION_SETTINGS), "apps settings")); return;
            case SITE: reply(launch(new Intent(Intent.ACTION_VIEW, Uri.parse(UDOS_URL)), "UDOS website")); return;
            case STATUS: reply("UDOS Mobile is active. Camera, settings and text commands work without Pi5. Use voice check to inspect speech support."); return;
            case VOICE_STATUS: showVoiceStatus(); return;
            case TOOLS: showPanel("tools"); return;
            case MAP: showPanel("mapCore"); return;
            case PI_STATUS: askPiBrain(command.text); return;
            case ENGLISH: case TAMIL: changeLanguage(command.text); return;
            default: reply("Command not recognized. Try camera, settings, apps, website, voice check, tools, map, or sleep.");
        }
    }

    private String launch(Intent intent, String label) {
        if (!visible || destroyed) return "UDOS is not in the foreground.";
        // Releasing the microphone before changing apps also prevents a late recognition result.
        stopVoiceSession();
        try {
            startActivity(intent);
            return "Opening " + label + ".";
        } catch (RuntimeException error) {
            return "Cannot open " + label + ": no available app or Android denied access.";
        }
    }

    private void showPanel(String function) {
        if (pageReady) web.evaluateJavascript(function + "();", null);
    }

    private void reply(String message) {
        lastReply = bounded(message);
        updateOutput("<b>NOVA:</b> " + escapeHtml(lastReply));
        speakNow(lastReply);
    }

    private void changeLanguage(String tag) {
        if (!visible || destroyed || !("en-US".equals(tag) || "ta-IN".equals(tag))) return;
        stopVoiceSession();
        stopSpeech();
        languageTag = tag;
        getPreferences(MODE_PRIVATE).edit().putString("voice_language", tag).apply();
        showVoiceStatus();
    }

    private String recognizerDescription() {
        return onDeviceAvailable() ? "Recognition: Android on-device service."
                : "Recognition: Android default service; it may send audio online.";
    }

    private void showVoiceStatus() {
        if (!visible || destroyed) return;
        updateOutput("<b>VOICE CHECK:</b><br>Microphone permission: " + (hasMicPermission() ? "granted" : "not granted")
                + "<br>Recognition service: " + (recognitionAvailable() ? "available" : "unavailable")
                + "<br>" + recognizerDescription()
                + "<br>Speech language: " + escapeHtml(languageTag)
                + "<br>TTS engine: " + (ttsReady ? "initialized" : "unavailable or starting")
                + "<br>English TTS data: " + ttsLanguageStatus("en-US")
                + "<br>Tamil TTS data: " + ttsLanguageStatus("ta-IN")
                + "<br>Wake session: " + (wakeMode ? "active" : "stopped")
                + "<br>Pi5 is optional for these phone commands.");
    }

    private String ttsLanguageStatus(String tag) {
        return ttsReady && tts != null && tts.isLanguageAvailable(Locale.forLanguageTag(tag)) >= TextToSpeech.LANG_AVAILABLE
                ? "available" : "unavailable or not checked";
    }

    private void updateOutput(String html) {
        if (destroyed) return;
        lastOutput = html;
        if (pageReady && web != null) web.evaluateJavascript("noteRaw(" + JSONObject.quote(html) + ");", null);
    }

    private String bounded(String text) {
        if (text == null) return "";
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void askPiBrain(String question) {
        if (piBusy) {
            updateOutput("<b>Pi5:</b> A request is already running. Wait for it to finish, then retry.");
            return;
        }
        piBusy = true;
        final int epoch = requestEpoch;
        updateOutput("<b>Pi5:</b> Contacting the configured EVE endpoint...");
        new Thread(() -> {
            HttpURLConnection connection = null;
            String response;
            try {
                URL url = new URL(PI_BRAIN_BASE + URLEncoder.encode(bounded(question), "UTF-8"));
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(4000);
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                if (status != 200) throw new java.io.IOException("Non-success response");
                StringBuilder body = new StringBuilder();
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), "UTF-8")) {
                    char[] buffer = new char[2048];
                    int count;
                    long deadline = SystemClock.elapsedRealtime() + 6000;
                    while ((count = reader.read(buffer)) != -1) {
                        if (body.length() + count > 65536 || SystemClock.elapsedRealtime() > deadline) {
                            throw new java.io.IOException("Response limit reached");
                        }
                        body.append(buffer, 0, count);
                    }
                }
                response = new JSONObject(body.toString()).optString("reply", "Pi responded without an assistant reply.");
            } catch (Exception error) {
                response = "Pi Brain request failed. Phone commands remain available without Pi5.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String result = response;
            handler.post(() -> {
                piBusy = false;
                if (visible && !destroyed && epoch == requestEpoch) reply(result);
            });
        }, "udos-pi-status").start();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != REQ_MIC || !permissionPending) return;
        permissionPending = false;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED && visible) {
            startVoiceRequest(permissionWake);
        } else {
            stopVoiceSession();
            updateOutput("<b>VOICE:</b> Microphone permission was not granted. Text commands remain available.");
        }
    }

    @Override protected void onStart() {
        super.onStart();
        visible = true;
    }

    @Override protected void onStop() {
        visible = false;
        permissionPending = false;
        requestEpoch++;
        stopVoiceSession();
        super.onStop();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        stopVoiceSession();
        stopSpeech();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) { tts.shutdown(); tts = null; }
        if (web != null) {
            web.removeJavascriptInterface("UDOS");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        stopVoiceSession();
        stopSpeech();
        if (pageReady) web.evaluateJavascript("window.udosHome && window.udosHome();", null);
    }
}
