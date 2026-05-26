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
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;
    private SpeechRecognizer currentRecognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean wakeMode = false;
    private boolean isListening = false;

    private static final int REQ_MIC = 44;
    private static final String UDOS_URL = "https://udos.universaldragon.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        settings.setTextZoom(100);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        web.setBackgroundColor(Color.rgb(3, 6, 18));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new UdosBridge(), "UDOS");

        setContentView(web);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.US);
                    tts.setSpeechRate(0.95f);
                }
            }
        });

        web.loadDataWithBaseURL(UDOS_URL, dashboardHtml(), "text/html", "UTF-8", null);
    }

    public class UdosBridge {
        @JavascriptInterface
        public void openSite() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse(UDOS_URL)));
                }
            });
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    safeStart(new Intent(Settings.ACTION_SETTINGS));
                }
            });
        }

        @JavascriptInterface
        public void openApps() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    safeStart(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
                }
            });
        }

        @JavascriptInterface
        public void openCamera() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    safeStart(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                }
            });
        }

        @JavascriptInterface
        public void speak(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speakNow(message);
                }
            });
        }

        @JavascriptInterface
        public void listen() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startListening(false);
                }
            });
        }

        @JavascriptInterface
        public void wake() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startWakeMode();
                }
            });
        }

        @JavascriptInterface
        public void sleep() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopWakeMode();
                }
            });
        }

        @JavascriptInterface
        public void command(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleCommand(message, false);
                }
            });
        }

        @JavascriptInterface
        public void toast(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void safeStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            speakNow("UDOS: app not available");
        }
    }

    private void speakNow(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "UDOS is active";
        }

        if (tts != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "UDOS_SPEAK");
            } else {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasMicPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    private void startWakeMode() {
        wakeMode = true;
        updateOutput("<b>WAKE MODE:</b> active.<br>Say <b>Hey Dragon</b>, <b>Hey EVE</b>, or <b>Hey NOVA</b>. Screen must stay awake for this safe version.");
        speakNow("Wake mode active. Say Hey Dragon, Hey EVE, or Hey Nova.");
        startListening(true);
    }

    private void stopWakeMode() {
        wakeMode = false;
        stopRecognizer();
        updateOutput("<b>WAKE MODE:</b> stopped.<br>UDOS is still home launcher. Voice loop disabled.");
        speakNow("Wake mode stopped");
    }

    private void stopRecognizer() {
        isListening = false;
        if (currentRecognizer != null) {
            try {
                currentRecognizer.cancel();
                currentRecognizer.destroy();
            } catch (Exception ignored) {
            }
            currentRecognizer = null;
        }
    }

    private void restartWakeListening() {
        if (!wakeMode) return;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (wakeMode && !isListening) {
                    startListening(true);
                }
            }
        }, 900);
    }

    private void startListening(final boolean fromWakeMode) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speakNow("Voice recognition is not available on this phone");
            return;
        }

        if (!hasMicPermission()) {
            requestMicPermission();
            return;
        }

        stopRecognizer();

        currentRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        isListening = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, fromWakeMode ? "Say Hey Dragon" : "Say EVE command");

        currentRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, fromWakeMode ? "UDOS wake listening..." : "UDOS listening...", Toast.LENGTH_SHORT).show();
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                isListening = false;
                stopRecognizer();
                if (wakeMode) {
                    restartWakeListening();
                } else {
                    speakNow("I did not hear clearly");
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = "";
                if (list != null && list.size() > 0) {
                    heard = list.get(0);
                }
                stopRecognizer();
                handleCommand(heard, fromWakeMode);
                if (wakeMode) {
                    restartWakeListening();
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        currentRecognizer.startListening(intent);
    }

    private void handleCommand(String text, boolean fromWakeMode) {
        if (text == null) text = "";
        String clean = text.trim();
        String lower = clean.toLowerCase(Locale.US);
        String reply = "Heard: " + clean;

        if (lower.length() == 0) {
            reply = "No command heard";
        } else if (lower.contains("stop listening") || lower.equals("sleep") || lower.contains("sleep mode")) {
            stopWakeMode();
            return;
        } else if (lower.contains("wake") || lower.contains("hey dragon") || lower.contains("hey eve") || lower.contains("hey nova") || lower.equals("dragon") || lower.equals("eve") || lower.equals("nova")) {
            if (!wakeMode) wakeMode = true;
            reply = "I am awake. UDOS is listening.";
        } else if (lower.contains("camera")) {
            reply = "Opening camera";
            safeStart(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
        } else if (lower.contains("setting")) {
            reply = "Opening settings";
            safeStart(new Intent(Settings.ACTION_SETTINGS));
        } else if (lower.contains("app")) {
            reply = "Opening apps settings";
            safeStart(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        } else if (lower.contains("website") || lower.contains("live") || lower.contains("udos")) {
            reply = "Opening live UDOS";
            safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse(UDOS_URL)));
        } else if (lower.contains("status")) {
            reply = "UDOS Mobile is active. Home layer safe. Voice layer ready.";
        } else if (lower.startsWith("speak ")) {
            reply = clean.substring(6);
        }

        updateOutput("<b>EVE:</b> " + escapeHtml(reply) + "<br><br><small>Input: " + escapeHtml(clean) + "</small>");
        speakNow(reply);
    }

    private void updateOutput(String message) {
        if (web != null) {
            web.evaluateJavascript("noteRaw(" + jsQuote(message) + ")", null);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String jsQuote(String value) {
        if (value == null) value = "";
        value = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "\"" + value + "\"";
    }

    private String dashboardHtml() {
        return "<!doctype html>" +
                "<html><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
                "<style>" +
                "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#030612;color:#f6fbff;font-family:system-ui,-apple-system,Segoe UI,sans-serif}" +
                "body:before{content:'';position:fixed;inset:-30%;background:radial-gradient(circle at 50% 2%,rgba(34,197,255,.38),transparent 28%),radial-gradient(circle at 86% 35%,rgba(255,106,0,.22),transparent 28%),radial-gradient(circle at 8% 68%,rgba(159,70,255,.28),transparent 30%),linear-gradient(180deg,#061a28 0%,#040713 52%,#010207 100%);z-index:-3}" +
                "body:after{content:'';position:fixed;inset:0;background:repeating-linear-gradient(90deg,rgba(255,255,255,.035) 0 1px,transparent 1px 9px),repeating-linear-gradient(0deg,rgba(255,255,255,.025) 0 1px,transparent 1px 10px);mix-blend-mode:screen;opacity:.45;z-index:-2}" +
                ".app{height:100vh;display:flex;flex-direction:column;padding:12px 12px 82px;gap:10px;overflow:hidden}" +
                ".top{display:flex;align-items:center;justify-content:space-between;gap:10px}.brand{display:flex;align-items:center;gap:9px;font-weight:900;letter-spacing:4px;color:#b165ff;text-shadow:0 0 16px rgba(177,101,255,.7)}.dragon{width:26px;height:26px;border-radius:9px;display:grid;place-items:center;background:linear-gradient(135deg,#23ff90,#1666ff);color:#031018}.chip{font-size:10px;letter-spacing:1.5px;color:#8ff7ff;border:1px solid rgba(45,220,255,.45);border-radius:999px;padding:7px 10px;background:rgba(5,18,30,.72)}" +
                ".hero{position:relative;border:1px solid rgba(52,230,255,.36);border-radius:22px;padding:14px;background:linear-gradient(145deg,rgba(5,22,38,.78),rgba(12,8,28,.82));box-shadow:0 0 26px rgba(0,226,255,.18),inset 0 0 36px rgba(21,180,255,.08);overflow:hidden}.hero:before{content:'';position:absolute;inset:-40%;background:conic-gradient(from 180deg,transparent,rgba(0,238,255,.15),transparent,rgba(255,119,0,.14),transparent);animation:spin 16s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}.hero>*{position:relative}.label{text-align:center;color:#ffc46a;border:1px solid #ffb24b;border-radius:4px;padding:8px 6px;font-size:10px;font-weight:800;letter-spacing:2.4px;text-transform:uppercase}.title{text-align:center;font-size:34px;font-weight:1000;letter-spacing:5px;margin:12px 0 4px;background:linear-gradient(90deg,#9ff7ff,#2877ff,#b55cff);-webkit-background-clip:text;color:transparent;text-shadow:0 0 24px rgba(0,200,255,.35)}.sub{text-align:center;color:#d5eaff;font-size:12px;line-height:1.5;margin:0 auto;max-width:310px}.core{display:flex;align-items:center;justify-content:center;gap:12px;margin-top:12px}.orb{width:86px;height:86px;border-radius:50%;border:2px solid rgba(73,239,255,.75);background:radial-gradient(circle,#34d7ff 0 16%,#0b4a75 36%,#07111d 68%);box-shadow:0 0 28px rgba(0,238,255,.55),inset 0 0 24px rgba(0,238,255,.35)}.side{width:88px;min-height:72px;border:1px solid rgba(68,229,255,.38);border-radius:16px;background:rgba(0,10,18,.62);padding:9px;font-size:10px;color:#aeeaff}.side b{display:block;color:#fff;font-size:14px;margin-top:3px}.side span{display:block;color:#65e5ff;margin-top:4px}" +
                ".stage{flex:1;min-height:0;overflow-y:auto;scroll-behavior:smooth;border:1px solid rgba(54,230,255,.35);border-radius:24px;background:linear-gradient(180deg,rgba(8,14,30,.86),rgba(3,7,18,.92));box-shadow:inset 0 0 34px rgba(0,238,255,.08);padding:12px}.stage::-webkit-scrollbar{display:none}.brain{display:flex;gap:12px;align-items:center;margin-bottom:10px}.avatar{width:54px;height:54px;border-radius:50%;display:grid;place-items:center;font-weight:900;background:linear-gradient(135deg,#8d42ff,#22d3ee);box-shadow:0 0 22px rgba(135,80,255,.6)}.brain h2{margin:0;font-size:21px}.brain p{margin:3px 0 0;color:#a6e7ff;font-size:12px}.status{border-left:3px solid #33e6ff;padding:10px 12px;border-radius:14px;background:rgba(0,0,0,.34);font-size:13px;line-height:1.55;color:#f6fbff}.status b{color:#8dffce}.grid{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:12px}.tile{min-height:66px;border:1px solid rgba(58,223,255,.42);border-radius:18px;background:linear-gradient(145deg,rgba(20,31,58,.82),rgba(5,9,22,.92));box-shadow:0 0 18px rgba(0,210,255,.12);color:white;text-align:left;padding:10px;font-weight:850}.tile small{display:block;margin-top:4px;color:#9fb8ca;font-weight:650}.tile.hot{border-color:rgba(255,139,40,.75);box-shadow:0 0 22px rgba(255,106,0,.18)}.tile.wake{border-color:rgba(42,255,148,.8);box-shadow:0 0 22px rgba(42,255,148,.2)}" +
                ".ask{display:flex;gap:8px;background:rgba(0,0,0,.45);border:1px solid rgba(69,226,255,.45);border-radius:18px;padding:8px}input{flex:1;min-width:0;background:rgba(0,0,0,.45);border:0;outline:0;color:#fff;font-size:15px;padding:12px;border-radius:13px}button.send{border:0;border-radius:14px;color:white;font-weight:900;padding:0 16px;background:linear-gradient(135deg,#16e7ff,#8047ff,#ff7a18);box-shadow:0 0 20px rgba(85,170,255,.4)}" +
                ".dock{position:fixed;left:10px;right:10px;bottom:10px;height:64px;border-radius:25px;background:rgba(2,7,16,.86);border:1px solid rgba(77,231,255,.38);box-shadow:0 0 28px rgba(0,220,255,.18);display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:8px;backdrop-filter:blur(12px)}.nav{border:1px solid rgba(75,227,255,.24);border-radius:18px;display:grid;place-items:center;text-align:center;font-size:10px;color:#dffbff;font-weight:850;background:linear-gradient(180deg,rgba(20,34,62,.78),rgba(4,8,18,.88))}.nav b{display:block;font-size:18px;line-height:14px;margin-bottom:3px}.nav.active{border-color:#ffad42;color:#ffd88a}" +
                "@media(max-height:720px){.title{font-size:28px;margin:7px 0 2px}.orb{width:64px;height:64px}.side{min-height:58px}.hero{padding:10px}.sub{font-size:11px}.grid{grid-template-columns:repeat(3,1fr)}.tile{min-height:54px;font-size:12px;padding:8px}.tile small{display:none}.stage{padding:10px}.brain h2{font-size:18px}}" +
                "</style></head><body>" +
                "<div class='app'>" +
                "<div class='top'><div class='brand'><span class='dragon'>UD</span><span>UDOS</span></div><div class='chip'>MOTO G22 · SAFE MODE</div></div>" +
                "<section class='hero'><div class='label'>Universal Dragon Operating System</div><div class='title'>UDOS</div><p class='sub'>Assistant-first mobile layer by Aslam. Apps stay underneath. EVE becomes the front control brain.</p><div class='core'><div class='side'>UNIVERSAL DRAGON<b>ASLAM</b><span>AI · Robotics · Systems</span></div><div class='orb'></div><div class='side'>NOVA / EVE<b>CORE</b><span>Safe launcher layer</span></div></div></section>" +
                "<section class='stage' id='stage'><div class='brain'><div class='avatar'>EVE</div><div><h2>EVE Brain</h2><p>UDOS local launcher layer active</p></div></div><div class='status' id='out'><b>STATUS:</b> UDOS normal. Voice, speak, and wake mode ready.<br><b>WAKE:</b> tap Wake, then say Hey Dragon / Hey EVE / Hey NOVA.<br><b>LIMIT:</b> safe app mode needs screen/app awake.</div><div class='grid'>" +
                "<button class='tile hot' onclick=\"noteRaw('<b>CORE:</b> Universal Dragon launcher layer is active. This is not full OS yet. Sensible engineering, for once.')\">🔥 Core<small>UDOS home layer</small></button>" +
                "<button class='tile' onclick=\"noteRaw('<b>EVE:</b> command brain ready. Voice layer can open settings, camera, apps and live UDOS.')\">🧠 EVE Brain<small>assistant command box</small></button>" +
                "<button class='tile' onclick=\"noteRaw('<b>PROJECTS:</b> Universal Dragon, NOVA, EVE, UDOS. No zero restart. Good, finally.')\">📁 Projects<small>project identity</small></button>" +
                "<button class='tile' onclick=\"window.UDOS&&UDOS.openCamera()\">📷 Camera<small>open camera</small></button>" +
                "<button class='tile' onclick=\"window.UDOS&&UDOS.openSite()\">🌐 Live UDOS<small>open website</small></button>" +
                "<button class='tile hot' onclick=\"window.UDOS&&UDOS.openSettings()\">⚙ Settings<small>phone control</small></button>" +
                "<button class='tile' onclick=\"window.UDOS&&UDOS.listen()\">🎙 Voice<small>one command</small></button>" +
                "<button class='tile' onclick=\"window.UDOS&&UDOS.speak(document.getElementById('out').innerText)\">🔊 Speak<small>read response</small></button>" +
                "<button class='tile wake' onclick=\"window.UDOS&&UDOS.wake()\">🟢 Wake<small>hey dragon mode</small></button>" +
                "<button class='tile hot' onclick=\"window.UDOS&&UDOS.sleep()\">⛔ Sleep<small>stop listening</small></button>" +
                "</div></section>" +
                "<div class='ask'><input id='q' placeholder='Ask EVE...' onkeydown=\"if(event.key==='Enter')send()\"><button class='send' onclick='send()'>Send</button></div>" +
                "</div>" +
                "<nav class='dock'><div class='nav active' onclick=\"home()\"><b>⌂</b>HOME</div><div class='nav' onclick=\"window.UDOS&&UDOS.wake()\"><b>◉</b>WAKE</div><div class='nav' onclick=\"noteRaw('<b>TOOLS:</b> Camera, Settings, Live UDOS, Voice, Speak and Wake are active.')\"><b>▣</b>TOOLS</div><div class='nav' onclick=\"window.UDOS&&UDOS.openApps()\"><b>⚙</b>APPS</div></nav>" +
                "<script>function noteRaw(t){document.getElementById('out').innerHTML=t;document.getElementById('stage').scrollTop=0}function send(){var q=document.getElementById('q').value.trim();if(!q){q='status'}if(window.UDOS){UDOS.command(q)}else{noteRaw('<b>EVE:</b> '+q)}}function home(){document.getElementById('q').value='';noteRaw('<b>STATUS:</b> UDOS normal. Voice, speak, and wake mode ready.<br><b>WAKE:</b> tap Wake, then say Hey Dragon / Hey EVE / Hey NOVA.<br><b>LIMIT:</b> safe app mode needs screen/app awake.')}window.udosHome=home;</script>" +
                "</body></html>";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (wakeMode) {
                startListening(true);
            } else {
                startListening(false);
            }
        } else {
            speakNow("Microphone permission needed for voice");
        }
    }

    @Override
    protected void onDestroy() {
        wakeMode = false;
        stopRecognizer();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (web != null) {
            web.evaluateJavascript("window.udosHome && window.udosHome();", null);
        } else {
            super.onBackPressed();
        }
    }
}
