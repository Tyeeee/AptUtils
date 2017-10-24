package com.yjt.apt.router.listener.service.implement;

import android.content.Context;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.listener.service.AutowireService;
import com.yjt.apt.router.listener.template.ISyringe;
import com.yjt.apt.router.utils.DebugUtil;

import org.apache.commons.collections4.map.LRUMap;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(path = "/router/service/autowire")
public class AutowireServiceImplement implements AutowireService {

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
                ISyringe autowireHelper = classCache.get(className);
                if (null == autowireHelper) {  // No cache.
                    autowireHelper = (ISyringe) Class.forName(instance.getClass().getName() + Constant.SUFFIX_AUTOWIRE).getConstructor().newInstance();
                }
                autowireHelper.inject(instance);
                classCache.put(className, autowireHelper);
            }
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            DebugUtil.getInstance().error(Constant.TAG, "Autowire made exception in class [" + className + "]");
            blackList.add(className);    // This instance need not autowire.
        }
    }
}
