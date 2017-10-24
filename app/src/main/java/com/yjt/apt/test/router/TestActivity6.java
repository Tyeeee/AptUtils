package com.yjt.apt.test.router;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Autowire;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/test/router/TestActivity6")
public class TestActivity6 extends Activity {

    @Autowire
    String name;
    @Autowire
    int age;
    @Autowire(name = "boy")
    boolean girl;
    private long high;
    @Autowire
    String url;
    @Autowire
    String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test6);
        name = getIntent().getStringExtra("name");
        age = getIntent().getIntExtra("age", 0);
        girl = getIntent().getBooleanExtra("girl", false);
        high = getIntent().getLongExtra("high", 0);
        url = getIntent().getStringExtra("url");
        key = getIntent().getStringExtra("key");
        ((TextView) findViewById(R.id.tvData)).setText(String.format("name=%s, age=%s, girl=%s, high=%s, url=%s, key=%s", name, age, girl, high, url, key));
    }
}
