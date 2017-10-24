package com.yjt.apt.test.router;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.listener.callback.NavigationCallback;
import com.yjt.apt.router.model.Postcard;


public class SchemeFilterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        直接通过Router处理外部Uri
        Uri uri = getIntent().getData();
        Router.getInstance().build(uri).navigation(this, new NavigationCallback() {

            @Override
            public void onFound(Postcard postcard) {
                finish();
            }

            @Override
            public void onLost(Postcard postcard) {
                finish();
            }
        });
    }
}
