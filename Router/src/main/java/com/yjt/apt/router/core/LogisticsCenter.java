package com.yjt.apt.router.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.annotation.model.RouteMetadata;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.exception.MainProcessException;
import com.yjt.apt.router.exception.RouteNotFoundException;
import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.template.IInterceptor;
import com.yjt.apt.router.listener.template.IInterceptorGroup;
import com.yjt.apt.router.listener.template.IProvider;
import com.yjt.apt.router.listener.template.IProviderGroup;
import com.yjt.apt.router.listener.template.IRouteGroup;
import com.yjt.apt.router.listener.template.IRouteRoot;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.thread.CancelableCountDownLatch;
import com.yjt.apt.router.utils.ClassUtil;
import com.yjt.apt.router.utils.DebugUtil;
import com.yjt.apt.router.utils.LogisticsCenter;
import com.yjt.apt.router.utils.StringUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LogisticsCenter {

    // Cache route and metas
    private static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    private static Map<String, RouteMetadata> routes = new HashMap<>();

    // Cache provider
    private static Map<Class, IProvider> providers = new HashMap<>();
    private static Map<String, RouteMetadata> providersIndex = new HashMap<>();

    // Cache interceptor
    private static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new TreeMap<>();
    private static List<IInterceptor> interceptors = new ArrayList<>();

    private Context context;
    private ThreadPoolExecutor executor;
    private boolean interceptorHasInit;
    private static final Object interceptorInitLock = new Object();

    private static LogisticsCenter logisticsCenter;

    private LogisticsCenter() {
        // cannot be instantiated
    }

    public static synchronized LogisticsCenter getInstance() {
        if (logisticsCenter == null) {
            logisticsCenter = new LogisticsCenter();
        }
        return logisticsCenter;
    }

    public static void releaseInstance() {
        if (logisticsCenter != null) {
            logisticsCenter = null;
        }
    }

    public synchronized void initialize(Context context, ThreadPoolExecutor executor) throws MainProcessException {
        this.context = context;
        this.executor = executor;
        try {
            // These class was generate by arouter-compiler.
            for (String className : ClassUtil.getInstance().getFileNameByPackageName(this.context, Constant.ROUTE_ROOT_PAKCAGE)) {
                if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_ROOT)) {
                    // This one of root elements, load root.
                    ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(groupsIndex);
                } else if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_INTERCEPTORS)) {
                    // Load interceptorMeta
                    ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(interceptorsIndex);
                } else if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_PROVIDERS)) {
                    // Load providerIndex
                    ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(providersIndex);
                }
            }

            if (groupsIndex.size() == 0) {
                DebugUtil.getInstance().error(Constant.TAG, "No mapping files were found, check your configuration please!");
            }
            if (Router.getInstance().debuggable()) {
                DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "logisticsCenter has already been loaded, GroupIndex[%d], InterceptorIndex[%d], ProviderIndex[%d]", groupsIndex.size(), interceptorsIndex.size(), providersIndex.size()));
            }
        } catch (IOException | InstantiationException | NoSuchMethodException | PackageManager.NameNotFoundException | InvocationTargetException | ClassNotFoundException | IllegalAccessException e) {
            throw new MainProcessException(Constant.TAG + "ARouter init atlas exception! [" + e.getMessage() + "]");
        }
    }

    public void initializeInterceptors() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (null != interceptorsIndex && interceptorsIndex.size() > 0) {
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : interceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.init(context);
                            interceptors.add(iInterceptor);
                        } catch (Exception ex) {
                            throw new MainProcessException(Constant.TAG + "ARouter init interceptor error! name = [" + interceptorClass.getName() + "], reason = [" + ex.getMessage() + "]");
                        }
                    }
                    interceptorHasInit = true;
                    DebugUtil.getInstance().info(Constant.TAG, "ARouter interceptors init over.");
                    synchronized (interceptorInitLock){
                        interceptorInitLock.notifyAll();
                    }
                }
            }
        });
    }

    private void checkInterceptorsInitStatus() {
        synchronized (interceptorInitLock){
            while (!interceptorHasInit) {
                try {
                    interceptorInitLock.wait(10 * 1000);
                } catch (InterruptedException e) {
                    throw new MainProcessException(Constant.TAG + "ARouter waiting for interceptor init error! reason = [" + e.getMessage() + "]");
                }
            }
        }
    }

    public Postcard buildProvider(String serviceName) {
        RouteMetadata meta = providersIndex.get(serviceName);
        if (null == meta) {
            return null;
        } else {
            return new Postcard(meta.getPath(), meta.getGroup());
        }
    }

    public synchronized void completion(Postcard postcard) {
        if (null == postcard) {
            throw new RouteNotFoundException(Constant.TAG + "No postcard!");
        }
        RouteMetadata RouteMetadata = routes.get(postcard.getPath());
        if (null == RouteMetadata) {    // Maybe its does't exist, or didn't load.
            Class<? extends IRouteGroup> groupMeta = groupsIndex.get(postcard.getGroup());  // Load route meta.
            if (null == groupMeta) {
                throw new RouteNotFoundException(Constant.TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                // Load route and cache it into memory, then delete from metas.
                try {
                    if (Router.getInstance().debuggable()) {
                        DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "The group [%s] starts loading, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }

                    IRouteGroup iGroupInstance = groupMeta.getConstructor().newInstance();
                    iGroupInstance.loadInto(routes);
                    groupsIndex.remove(postcard.getGroup());

                    if (Router.getInstance().debuggable()) {
                        DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "The group [%s] has already been loaded, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                } catch (Exception e) {
                    throw new MainProcessException(Constant.TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
                }

                completion(postcard);   // Reload
            }
        } else {
            postcard.setDestination(RouteMetadata.getDestination());
            postcard.setType(RouteMetadata.getType());
            postcard.setPriority(RouteMetadata.getPriority());
            postcard.setExtra(RouteMetadata.getExtra());

            Uri rawUri = postcard.getUri();
            if (null != rawUri) {   // Try to set params into bundle.
                Map<String, String> resultMap = StringUtil.getInstance().splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = RouteMetadata.getParamsType();
                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
                        setValue(postcard, params.getValue(), params.getKey(), resultMap.get(StringUtil.getInstance().getRight(params.getKey())
                        ));
                    }
                    // Save params name which need autoinject.
                    postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }
                // Save raw uri
                postcard.putString(ARouter.RAW_URI, rawUri.toString());
            }

            switch (RouteMetadata.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must be implememt IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) RouteMetadata.getDestination();
                    IProvider instance = providers.get(providerMeta);
                    if (null == instance) { // There's no instance of this provider
                        IProvider provider;
                        try {
                            provider = providerMeta.getConstructor().newInstance();
                            provider.init(context);
                            providers.put(providerMeta, provider);
                            instance = provider;
                        } catch (Exception e) {
                            throw new MainProcessException("Init provider failed! " + e.getMessage());
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.greenChannel();    // Provider should skip all of interceptors
                    break;
                default:
                    break;
            }
        }
    }

    private void setValue(Postcard postcard, Integer type, String key, String value) {
        try {
            String currentKey = StringUtil.getInstance().getLeft(key);
            if (null != type) {
                switch (type) {
                    case Constant.DEFINE_BOOLEAN:
                        postcard.putBoolean(currentKey, Boolean.parseBoolean(value));
                        break;
                    case Constant.DEFINE_BYTE:
                        postcard.putByte(currentKey, Byte.valueOf(value));
                        break;
                    case Constant.DEFINE_SHORT:
                        postcard.putShort(currentKey, Short.valueOf(value));
                        break;
                    case Constant.DEFINE_INT:
                        postcard.putInt(currentKey, Integer.valueOf(value));
                        break;
                    case Constant.DEFINE_LONG:
                        postcard.putLong(currentKey, Long.valueOf(value));
                        break;
                    case Constant.DEFINE_FLOAT:
                        postcard.putFloat(currentKey, Float.valueOf(value));
                        break;
                    case Constant.DEFINE_DOUBLE:
                        postcard.putDouble(currentKey, Double.valueOf(value));
                        break;
                    case Constant.DEFINE_STRING:
                        DEFINEault:
                        postcard.putString(currentKey, value);
                }
            } else {
                postcard.putString(currentKey, value);
            }
        } catch (Throwable ex) {
            DebugUtil.getInstance().warning(Constant.TAG, "logisticsCenter setValue failed! " + ex.getMessage());
        }
    }

    public void interception(final Postcard postcard, final InterceptorCallback callback) throws MainProcessException {
        if (CollectionUtils.isNotEmpty(interceptors)) {
            checkInterceptorsInitStatus();
            if (!interceptorHasInit) {
                callback.onInterrupt(new MainProcessException("Interceptors initialization takes too much time."));
                return;
            }
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(interceptors.size());
                    try {
                        _excute(0, interceptorCounter, postcard);
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS); // Cancel the navigation this time, if it hasn't return anythings.
                        if (null != postcard.getTag()) {    // Maybe some exception in the tag.
                            callback.onInterrupt(new MainProcessException(postcard.getTag().toString()));
                        } else {
                            callback.onContinue(postcard);
                        }
                    } catch (InterruptedException e) {
                        callback.onInterrupt(e);
                    }
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }

    private void _excute(final int index, final CancelableCountDownLatch counter, final Postcard postcard) {
        if (index < interceptors.size()) {
            interceptors.get(index).process(postcard, new InterceptorCallback() {

                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor excute over put no exception.
                    counter.countDown();
                    _excute(index + 1, counter, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor excute over put fatal exception.
                    postcard.setTag(null == exception ? new MainProcessException("No message.") : exception.getMessage());    // save the exception message for backup.
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

    public void suspend() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
        interceptors.clear();
        interceptorsIndex.clear();
        interceptorHasInit = false;
    }
}