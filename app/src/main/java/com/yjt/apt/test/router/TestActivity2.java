package com.yjt.apt.test.router;

import android.app.Activity;
import android.os.Bundle;

import com.yjt.apt.R;
import com.yjt.apt.constant.Constant;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/test/router/TestActivity2")
public class TestActivity2 extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);
        setResult(Constant.RESULT_CODE);
        finish();
    }
}
