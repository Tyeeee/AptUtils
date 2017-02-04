package com.yjt.apt.router;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Parameter;
import com.yjt.apt.router.annotation.Route;

/**
 * 自动注入的测试用例
 */
@Route(path = "/router/activity3")
public class Activity3 extends AppCompatActivity {

    @Parameter
    private String name;
    @Parameter
    private int age;
    @Parameter(name = "boy")
    private boolean girl;

    // 这个字段没有注解，是不会自动注入的
    private long high;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity1);

        String params = String.format("name=%s, age=%s, girl=%s, high=%s", name, age, girl, high);

        ((TextView) findViewById(R.id.test)).setText("I am " + Activity3.class.getName());
        ((TextView) findViewById(R.id.test2)).setText(params);
    }
}
