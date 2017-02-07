package com.yjt.apt.test.router;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/test/router/TestActivity3")
public class TestActivity3 extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test3);
        ((WebView) findViewById(R.id.webview)).loadUrl(getIntent().getStringExtra("url"));
    }
}
