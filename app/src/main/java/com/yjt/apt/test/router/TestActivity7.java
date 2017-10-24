package com.yjt.apt.test.router;

import android.app.Activity;
import android.os.Bundle;

import com.yjt.apt.R;
import com.yjt.apt.router.Router;
import com.yjt.apt.router.annotation.Autowire;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/test/router/TestActivity7")
public class TestActivity7 extends Activity {

    @Autowire()
    TestService testService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test7);
        Router.getInstance().inject(this);
        testService.hello("袁锦泰");
    }
}
