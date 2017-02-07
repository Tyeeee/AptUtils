package com.yjt.apt.test.router;

import android.content.Context;
import android.widget.Toast;

import com.yjt.apt.router.annotation.Route;

@Route(path = "/service/hello")
public class TestServiceImplement implements TestService {

    private Context context;

    @Override
    public void initialize(Context context) {
        this.context = context;
    }

    @Override
    public void hello(String name) {
        Toast.makeText(context, "Hello " + name, Toast.LENGTH_LONG).show();
    }
}
