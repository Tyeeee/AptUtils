package com.yjt.apt.test.router.module;

import android.app.Activity;
import android.os.Bundle;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.test.router.R;

@Route(path = "/module/TestActivity5", group = "袁锦泰")
public class TestActivity5 extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test5);
    }
}
