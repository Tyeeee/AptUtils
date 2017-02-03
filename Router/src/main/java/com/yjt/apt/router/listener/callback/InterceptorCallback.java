package com.yjt.apt.router.listener.callback;

import com.yjt.apt.router.model.Postcard;

public interface InterceptorCallback {

    void onContinue(Postcard postcard);

    void onInterrupt(Throwable throwable);
}
