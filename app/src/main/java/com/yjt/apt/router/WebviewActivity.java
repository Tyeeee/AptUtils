package com.yjt.apt.router;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/router/WebviewActivity")
public class WebviewActivity extends Activity {

    WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webview = (WebView) findViewById(R.id.webview);
        webview.loadUrl(getIntent().getStringExtra("url"));
    }
}
