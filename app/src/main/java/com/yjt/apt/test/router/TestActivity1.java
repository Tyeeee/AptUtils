package com.yjt.apt.test.router;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.yjt.apt.R;
import com.yjt.apt.router.annotation.Route;

@Route(path = "/test/router/TestActivity1")
public class TestActivity1 extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);
        TestModel model = getIntent().getParcelableExtra("model");
        if (model != null) {
            Toast.makeText(this, "name:" + model.getName() + "\n"
                    + "length:" + model.getLength() + "\n"
                    + "width:" + model.getWidth() + "\n"
                    + "height:" + model.getHeight(), Toast.LENGTH_LONG).show();
        }
    }
}
