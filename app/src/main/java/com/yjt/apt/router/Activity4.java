package com.yjt.apt.router;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/router/activity4")
public class Activity4 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity1);

        ((TextView) findViewById(R.id.test)).setText("I am " + Activity4.class.getName());
        String extra = getIntent().getStringExtra("extra");
        if (!TextUtils.isEmpty(extra)) {
            ((TextView) findViewById(R.id.test2)).setText(extra);
        }
    }
}
