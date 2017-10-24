package com.yjt.apt.router.listener.service.implement;

import android.content.Context;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.listener.service.DegradeService;
import com.yjt.apt.router.model.Postcard;

public class DegradeServiceImplement implements DegradeService {

    private Context context;

    @Override
    public void initialize(Context context) {
        this.context = context;
    }

    @Override
    public void onLost(Context context, Postcard postcard) {
        Router.getInstance().build("h5/webview")
                .putString("url", "https://m.yjt.com/test/router/error?path=" + postcard.getPath())
                .navigation(context);
    }
}
