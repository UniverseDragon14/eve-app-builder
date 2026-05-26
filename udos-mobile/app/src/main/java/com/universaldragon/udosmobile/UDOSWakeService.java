package com.universaldragon.udosmobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Locale;

public class UDOSWakeService extends Service implements RecognitionListener {
    private static final String CHANNEL_ID = "udos_wake_channel";
    private static final int NOTIFY_ID = 1408;

    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFY_ID, buildNotification("UDOS wake listening..."));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        prepareRecognizer();
        restartListening();
    }

    private void prepareRecognizer() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    }

    private void restartListening() {
        handler.postDelayed(() -> {
            try {
                if (recognizer == null) prepareRecognizer();
                if (recognizer != null && speechIntent != null) {
                    recognizer.cancel();
                    recognizer.startListening(speechIntent);
                }
            } catch (Exception ignored) {}
        }, 900);
    }

    private void handleWords(String text) {
        if (text == null) return;
        String low = text.toLowerCase(Locale.US);

        SharedPreferences prefs = getSharedPreferences("UDOS", MODE_PRIVATE);
        String words = prefs.getString(
                "wake_words",
                "hey dragon,hey eve,hey nova,dragon,eve,nova,hey aslam"
        );

        for (String raw : words.split(",")) {
            String w = raw.trim().toLowerCase(Locale.US);
            if (w.length() > 0 && low.contains(w)) {
                speak("UDOS awake. EVE ready.");
                openUDOS();
                return;
            }
        }
    }

    private void openUDOS() {
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    private void speak(String msg) {
        try {
            if (tts != null) {
                tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "UDOS_WAKE");
            }
        } catch (Exception ignored) {}
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return b.setContentTitle("UDOS Wake")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "UDOS Wake",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        restartListening();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        try { if (recognizer != null) recognizer.destroy(); } catch (Exception ignored) {}
        try { if (tts != null) tts.shutdown(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() { restartListening(); }

    @Override public void onError(int error) { restartListening(); }

    @Override public void onResults(Bundle results) {
        ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null) {
            for (String s : list) handleWords(s);
        }
        restartListening();
    }

    @Override public void onPartialResults(Bundle partialResults) {
        ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null) {
            for (String s : list) handleWords(s);
        }
    }

    @Override public void onEvent(int eventType, Bundle params) {}
}
