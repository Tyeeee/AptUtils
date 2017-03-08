package com.yjt.apt.router.constant;

import com.yjt.apt.router.annotation.model.RouteMetadata;
import com.yjt.apt.router.listener.template.IInterceptor;
import com.yjt.apt.router.listener.template.IProvider;
import com.yjt.apt.router.listener.template.IRouteGroup;
import com.yjt.apt.router.utils.wrapper.UniqueKeyTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Warehouse {

    // Cache route and metas
    public static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    public static Map<String, RouteMetadata> routes = new HashMap<>();

    // Cache provider
    public static Map<Class, IProvider> providers = new HashMap<>();
    public static Map<String, RouteMetadata> providersIndex = new HashMap<>();

    // Cache interceptor
    public static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    public static List<IInterceptor> interceptors = new ArrayList<>();

    public static void clear() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
        interceptors.clear();
        interceptorsIndex.clear();
    }
}
