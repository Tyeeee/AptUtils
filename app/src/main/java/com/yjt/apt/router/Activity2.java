package com.yjt.apt.router;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Parameter;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/router/activity2")
public class Activity2 extends AppCompatActivity {

    @Parameter
    private String key1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity2);
        String value = getIntent().getStringExtra("key1");
        if (!TextUtils.isEmpty(value)) {
            Toast.makeText(this, "exist param :" + value, Toast.LENGTH_LONG).show();
        }
        setResult(999);
    }
}
