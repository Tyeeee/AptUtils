package com.yjt.apt.router.listener.template;


import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.model.Postcard;

public interface IInterceptor extends IProvider {

    void process(Postcard postcard, InterceptorCallback callback);
}
