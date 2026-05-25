package com.universaldragon.evemobile;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView log;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 54, 28, 28);
        root.setBackgroundColor(Color.rgb(3, 8, 12));
        scroll.addView(root);

        root.addView(label("UNIVERSAL DRAGON", 24, true));
        root.addView(label("EVE MOBILE ZOMBIE", 30, true));
        root.addView(label("Phone is not app-based. Phone is assistant-based.", 14, true));

        input = new EditText(this);
        input.setHint("Ask EVE...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setMinLines(2);
        input.setBackgroundColor(Color.rgb(8, 20, 28));
        input.setPadding(18, 18, 18, 18);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        root.addView(button("RUN COMMAND", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String cmd = input.getText().toString().trim();
                if (cmd.length() == 0) log.setText("EVE >> waiting...");
                else log.setText("EVE >> " + cmd + "\nCommand received. Brain layer next.");
            }
        }));

        root.addView(button("Call", new View.OnClickListener() {
            @Override public void onClick(View v) { openUri("tel:"); }
        }));
        root.addView(button("Camera", new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent("android.media.action.IMAGE_CAPTURE")); }
        }));
        root.addView(button("Universal Dragon", new View.OnClickListener() {
            @Override public void onClick(View v) { openUri("https://universaldragon.com"); }
        }));
        root.addView(button("EVE Pi", new View.OnClickListener() {
            @Override public void onClick(View v) { openUri("http://nova-pi.local:5058"); }
        }));
        root.addView(button("Android Settings", new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent(Settings.ACTION_SETTINGS)); }
        }));
        root.addView(button("Home Settings", new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent(Settings.ACTION_HOME_SETTINGS)); }
        }));

        log = label("EVE READY\nZombie launcher mode active.\nSafe exit: Settings > Apps > Default apps > Home app.", 16, false);
        log.setGravity(Gravity.START);
        log.setBackgroundColor(Color.rgb(0, 18, 20));
        log.setPadding(18, 18, 18, 18);
        root.addView(log, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
    }

    private TextView label(String text, int size, boolean center) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setGravity(center ? Gravity.CENTER : Gravity.START);
        v.setPadding(10, 12, 10, 12);
        return v;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(12, 36, 48));
        b.setPadding(8, 12, 8, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        b.setLayoutParams(params);
        b.setOnClickListener(listener);
        return b;
    }

    private void openUri(String uri) {
        startSafe(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
    }

    private void startSafe(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            if (log != null) log.setText("EVE error: " + e.getMessage());
        }
    }
}
