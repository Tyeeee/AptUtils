package com.yjt.apt.router.listener.service.implement;

import android.content.Context;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.constant.Warehouse;
import com.yjt.apt.router.core.LogisticsCenter;
import com.yjt.apt.router.exception.MainProcessException;
import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.service.InterceptorService;
import com.yjt.apt.router.listener.template.IInterceptor;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.thread.CancelableCountDownLatch;
import com.yjt.apt.router.utils.DebugUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Route(path = "/router/service/interceptor")
public class InterceptorServiceImplement implements InterceptorService {

    private static boolean interceptorHasInitialized;
    private static final Object interceptorInitLock = new Object();

    @Override
    public void initialize(final Context context) {
        LogisticsCenter.getInstance().executor.execute(new Runnable() {

            @Override
            public void run() {
                if (MapUtils.isNotEmpty(Warehouse.interceptorsIndex)) {
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : Warehouse.interceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.initialize(context);
                            Warehouse.interceptors.add(iInterceptor);
                        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                            throw new MainProcessException(Constant.TAG + "Router init interceptor error! name = [" + interceptorClass.getName() + "], reason = [" + e.getMessage() + "]");
                        }
                    }

                    interceptorHasInitialized = true;
                    DebugUtil.getInstance().info(Constant.TAG, "Router interceptors init over.");
                    synchronized (interceptorInitLock){
                        interceptorInitLock.notifyAll();
                    }
                }
            }
        });
    }

    @Override
    public void intercept(final Postcard postcard, final InterceptorCallback callback) {
        if (CollectionUtils.isNotEmpty(Warehouse.interceptors)) {
            checkInterceptorsInitializeStatus();
            if (!interceptorHasInitialized) {
                callback.onInterrupt(new MainProcessException("Interceptors initialization takes too much time."));
                return;
            }
            LogisticsCenter.getInstance().executor.execute(new Runnable() {
                @Override
                public void run() {
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(Warehouse.interceptors.size());
                    try {
                        execute(0, interceptorCounter, postcard);
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS);
                        if (interceptorCounter.getCount() > 0) {    // Cancel the navigation this time, if it hasn't return anythings.
                            callback.onInterrupt(new MainProcessException("The interceptor processing timed out."));
                        } else if (null != postcard.getTag()) {    // Maybe some exception in the tag.
                            callback.onInterrupt(new MainProcessException(postcard.getTag().toString()));
                        } else {
                            callback.onContinue(postcard);
                        }
                    } catch (Exception e) {
                        callback.onInterrupt(e);
                    }
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }

    private void execute(final int index, final CancelableCountDownLatch counter, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()) {
            Warehouse.interceptors.get(index).process(postcard, new InterceptorCallback() {

                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor execute over put no exception.
                    counter.countDown();
                    execute(index + 1, counter, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor execute over put fatal exception.
                    postcard.setTag(exception == null ? new MainProcessException("No message.") : exception.getMessage());
                    // save the exception message for backup.
                    counter.cancel();
                    // Be attention, maybe the thread in callback has been changed,
                    // then the catch block(L207) will be invalid.
                    // The worst is the thread changed to main thread, then the app will be crash, if you throw this exception!
//                    if (!Looper.getMainLooper().equals(Looper.myLooper())) {    // You shouldn't throw the exception if the thread is main thread.
//                        throw new MainProcessException(exception.getMessage());
//                    }
                }
            });
        }
    }

    private void checkInterceptorsInitializeStatus() {
        synchronized (interceptorInitLock){
            while (!interceptorHasInitialized) {
                try {
                    interceptorInitLock.wait(10 * 1000);
                } catch (InterruptedException e) {
                    throw new MainProcessException(Constant.TAG + "Router waiting for interceptor initialize error! reason = [" + e.getMessage() + "]");
                }
            }
        }
    }
}
