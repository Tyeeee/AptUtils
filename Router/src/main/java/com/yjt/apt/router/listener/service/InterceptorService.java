package com.yjt.apt.router.listener.service;

import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.template.IProvider;
import com.yjt.apt.router.model.Postcard;

public interface InterceptorService extends IProvider {

    void intercept(Postcard postcard, InterceptorCallback callback);
}
