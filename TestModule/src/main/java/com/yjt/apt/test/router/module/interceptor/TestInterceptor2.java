package com.yjt.apt.test.router.module.interceptor;

import android.content.Context;

import com.yjt.apt.router.annotation.Interceptor;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.template.IInterceptor;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.utils.DebugUtil;

@Interceptor(priority = 30, name = "TestInterceptor2")
public class TestInterceptor2 implements IInterceptor {

    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        callback.onContinue(postcard);
//        callback.onInterrupt(new RuntimeException("TestInterceptor2"));
    }

    @Override
    public void initialize(Context context) {
        DebugUtil.getInstance().info(Constant.TAG, "TestInterceptor2");
    }
}
