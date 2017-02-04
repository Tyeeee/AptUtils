package com.yjt.apt.router;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Parameter;
import com.yjt.apt.router.annotation.Route;

/**
 * https://m.yjt.com/test/activity1?name=老王&age=23&boy=true&high=180
 */
@Route(path = "/router/activity1")
public class Activity1 extends AppCompatActivity {

    @Parameter
    private String name;
    @Parameter
    private int age;
    @Parameter(name = "boy")
    private boolean girl;
    private long high;
    @Parameter
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity1);

        name = getIntent().getStringExtra("name");
        age = getIntent().getIntExtra("age", 0);
        girl = getIntent().getBooleanExtra("girl", false);
        high = getIntent().getLongExtra("high", 0);
        url = getIntent().getStringExtra("url");

        String params = String.format("name=%s, age=%s, girl=%s, high=%s, url=%s", name, age, girl, high, url);

        ((TextView) findViewById(R.id.test)).setText("I am " + Activity1.class.getName());
        ((TextView) findViewById(R.id.test2)).setText(params);
    }
}
