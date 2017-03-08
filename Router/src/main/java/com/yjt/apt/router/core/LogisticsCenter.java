package com.yjt.apt.router.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.yjt.apt.router.annotation.model.RouteMetadata;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.constant.Warehouse;
import com.yjt.apt.router.exception.MainProcessException;
import com.yjt.apt.router.exception.RouteNotFoundException;
import com.yjt.apt.router.listener.template.IInterceptorGroup;
import com.yjt.apt.router.listener.template.IProvider;
import com.yjt.apt.router.listener.template.IProviderGroup;
import com.yjt.apt.router.listener.template.IRouteGroup;
import com.yjt.apt.router.listener.template.IRouteRoot;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.utils.ClassUtil;
import com.yjt.apt.router.utils.DebugUtil;
import com.yjt.apt.router.utils.StringUtil;

import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class LogisticsCenter {

    private Context context;
    public ThreadPoolExecutor executor;

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

    public static synchronized void releaseInstance() {
        if (logisticsCenter != null) {
            logisticsCenter = null;
        }
    }

    public synchronized void initialize(Context ctx, ThreadPoolExecutor executor) throws MainProcessException {
        this.context = ctx;
        this.executor = executor;
        try {
            // These class was generate by Router-compiler.
            for (String className : ClassUtil.getInstance().getFileNameByPackageName(context, Constant.ROUTE_ROOT_PAKCAGE)) {
                DebugUtil.getInstance().info(Constant.TAG, "******"+className);
                if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_ROOT)) {
                    // This one of root elements, load root.
                    ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
                } else if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_INTERCEPTORS)) {
                    // Load interceptorMeta
                    ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
                } else if (className.startsWith(Constant.ROUTE_ROOT_PAKCAGE + Constant.DOT + Constant.SDK_NAME + Constant.SEPARATOR + Constant.SUFFIX_PROVIDERS)) {
                    // Load providerIndex
                    ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
                }
            }
            if (Warehouse.groupsIndex.size() == 0) {
                DebugUtil.getInstance().error(Constant.TAG, "No mapping files were found, check your configuration please!");
            }
            DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "LogisticsCenter has already been loaded, GroupIndex[%d], InterceptorIndex[%d], ProviderIndex[%d]", Warehouse.groupsIndex.size(), Warehouse.interceptorsIndex.size(), Warehouse.providersIndex.size()));
        } catch (IOException | InstantiationException | NoSuchMethodException | PackageManager.NameNotFoundException | InvocationTargetException | ClassNotFoundException | IllegalAccessException e) {
            throw new MainProcessException(Constant.TAG + "Router initialize atlas exception! [" + e.getMessage() + "]");
        }
    }

    public Postcard buildProvider(String serviceName) {
        RouteMetadata metadata = Warehouse.providersIndex.get(serviceName);
        if (metadata == null) {
            return null;
        } else {
            return new Postcard(metadata.getPath(), metadata.getGroup());
        }
    }

    public synchronized void completion(Postcard postcard) {
        if (postcard == null) {
            throw new RouteNotFoundException(Constant.TAG + "No postcard!");
        }
        RouteMetadata metadata = Warehouse.routes.get(postcard.getPath());
        DebugUtil.getInstance().debug(Constant.TAG, postcard.toString());
        DebugUtil.getInstance().debug(Constant.TAG, String.valueOf(Warehouse.routes.size()));
        if (metadata == null) {    // Maybe its does't exist, or didn't load.
            DebugUtil.getInstance().debug(Constant.TAG, "groupsIndex:" + Warehouse.groupsIndex.toString());
            DebugUtil.getInstance().debug(Constant.TAG, String.valueOf(Warehouse.groupsIndex.size()));
            Class<? extends IRouteGroup> groupMetadata = Warehouse.groupsIndex.get(postcard.getGroup());  // Load route meta.
            if (groupMetadata == null) {
                throw new RouteNotFoundException(Constant.TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                // Load route and cache it into memory, then delete from metas.
                try {
                    DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "The group [%s] starts loading, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    IRouteGroup iGroupInstance = groupMetadata.getConstructor().newInstance();
                    iGroupInstance.loadInto(Warehouse.routes);
                    Warehouse.groupsIndex.remove(postcard.getGroup());
                    DebugUtil.getInstance().debug(Constant.TAG, String.format(Locale.getDefault(), "The group [%s] has already been loaded, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                    throw new MainProcessException(Constant.TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
                }
                completion(postcard);   // Reload
            }
        } else {
            DebugUtil.getInstance().debug(Constant.TAG, "metadata:" + metadata.toString());
            postcard.setDestination(metadata.getDestination());
            postcard.setType(metadata.getType());
            postcard.setPriority(metadata.getPriority());
            postcard.setExtra(metadata.getExtra());
            Uri rawUri = postcard.getUri();
            if (null != rawUri) {   // Try to set params into bundle.
                Map<String, String> resultMap = StringUtil.getInstance().splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = metadata.getParametersType();
                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
//                        setValue(postcard, params.getValue(), params.getKey(), resultMap.get(StringUtil.getInstance().getRight(params.getKey())
                        setValue(postcard, params.getValue(), params.getKey(), resultMap.get(params.getKey()));
                    }
                    // Save params name which need autoinject.
                    postcard.getExtras().putStringArray(Constant.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }
                // Save raw uri
                postcard.putString(Constant.RAW_URI, rawUri.toString());
            }

            switch (metadata.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must be implememt IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) metadata.getDestination();
                    IProvider instance = Warehouse.providers.get(providerMeta);
                    if (instance == null) { // There's no instance of this provider
                        try {
                            IProvider provider = providerMeta.getConstructor().newInstance();
                            provider.initialize(context);
                            Warehouse.providers.put(providerMeta, provider);
                            instance = provider;
                        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                            throw new MainProcessException("Initialize provider failed! " + e.getMessage());
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.setGreenChannel();    // Provider should skip all of interceptors
                    break;
                default:
                    break;
            }
        }
    }

    private void setValue(Postcard postcard, Integer type, String key, String value) {
        try {
//            String currentKey = StringUtil.getInstance().getLeft(key);
            if (null != type) {
                switch (type) {
                    case Constant.DEFINE_BOOLEAN:
                        postcard.putBoolean(key, Boolean.parseBoolean(value));
                        break;
                    case Constant.DEFINE_BYTE:
                        postcard.putByte(key, Byte.valueOf(value));
                        break;
                    case Constant.DEFINE_SHORT:
                        postcard.putShort(key, Short.valueOf(value));
                        break;
                    case Constant.DEFINE_INT:
                        postcard.putInt(key, Integer.valueOf(value));
                        break;
                    case Constant.DEFINE_LONG:
                        postcard.putLong(key, Long.valueOf(value));
                        break;
                    case Constant.DEFINE_FLOAT:
                        postcard.putFloat(key, Float.valueOf(value));
                        break;
                    case Constant.DEFINE_DOUBLE:
                        postcard.putDouble(key, Double.valueOf(value));
                        break;
                    case Constant.DEFINE_STRING:
                    default:
                        postcard.putString(key, value);
                }
            } else {
                postcard.putString(key, value);
            }
        } catch (Throwable throwable) {
            DebugUtil.getInstance().error(Constant.TAG, "logisticsCenter setValue failed! " + throwable.getMessage());
        }
    }

    public void suspend() {
        Warehouse.clear();
    }
}