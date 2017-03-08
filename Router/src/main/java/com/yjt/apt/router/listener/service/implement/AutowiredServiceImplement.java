package com.yjt.apt.router.listener.service.implement;

import android.content.Context;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.listener.service.AutowiredService;
import com.yjt.apt.router.listener.template.ISyringe;
import com.yjt.apt.router.utils.DebugUtil;

import org.apache.commons.collections4.map.LRUMap;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(path = "/router/service/autowired")
public class AutowiredServiceImplement implements AutowiredService {

    private Map<String, ISyringe> classCache;
    private List<String> blackList;

    @Override
    public void initialize(Context context) {
        classCache = new LRUMap<>();
        blackList = new ArrayList<>();
    }

    @Override
    public void autowire(Object instance) {
        String className = instance.getClass().getName();
        try {
            if (!blackList.contains(className)) {
                ISyringe autowiredHelper = classCache.get(className);
                if (null == autowiredHelper) {  // No cache.
                    autowiredHelper = (ISyringe) Class.forName(instance.getClass().getName() + Constant.SUFFIX_AUTOWIRED).getConstructor().newInstance();
                }
                autowiredHelper.inject(instance);
                classCache.put(className, autowiredHelper);
            }
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            DebugUtil.getInstance().error(Constant.TAG, "Autowired made exception in class [" + className + "]");
            blackList.add(className);    // This instance need not autowired.
        }
    }
}
