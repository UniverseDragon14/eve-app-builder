package com.universaldragon.udosmobile;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView web;
    private TextView status;
    private final String UDOS_URL = "https://udos.universaldragon.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(3, 8, 12));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                status.setText("UDOS ONLINE");
                status.postDelayed(new Runnable() {
                    @Override public void run() { status.setVisibility(View.GONE); }
                }, 1200);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl(UDOS_URL);

        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        status = new TextView(this);
        status.setText("UDOS BOOTING...");
        status.setTextColor(Color.rgb(60, 255, 140));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(Color.rgb(0, 0, 0));
        status.setPadding(16, 10, 16, 10);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = 24;
        root.addView(status, params);

        setContentView(root);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            web.loadUrl(UDOS_URL);
        }
    }
}
