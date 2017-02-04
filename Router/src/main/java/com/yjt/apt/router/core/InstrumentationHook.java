package com.yjt.apt.router.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.utils.DebugUtil;
import com.yjt.apt.router.utils.StringUtil;

import java.lang.reflect.Field;

public class InstrumentationHook extends Instrumentation {

    //    public Activity newActivity(Class<?> clazz, Context context,
//                                IBinder token, Application application, Intent intent, ActivityInfo info,
//                                CharSequence title, Activity parent, String id,
//                                Object lastNonConfigurationInstance) throws InstantiationException,
//            IllegalAccessException {
//        Activity activity = (Activity)clazz.newInstance();
//        ActivityThread aThread = null;
//        activity.attach(context, aThread, this, token, 0, application, intent,
//                info, title, parent, id,
//                (Activity.NonConfigurationInstances)lastNonConfigurationInstance,
//                new Configuration(), null, null);
//        return activity;
//    }

    public Activity newActivity(ClassLoader loader, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<?> targetActivity = loader.loadClass(className);
        Object instanceOfTarget = targetActivity.newInstance();
        if (Router.getInstance().canAutoInject()) {
            String[] autoInjectParams = intent.getStringArrayExtra(Constant.AUTO_INJECT);
            if (null != autoInjectParams && autoInjectParams.length > 0) {
                for (String paramsName : autoInjectParams) {
                    Object value = intent.getExtras().get(StringUtil.getInstance().getLeft(paramsName));
                    if (null != value) {
                        try {
                            Field injectField = targetActivity.getDeclaredField(StringUtil.getInstance().getLeft(paramsName));
                            injectField.setAccessible(true);
                            injectField.set(instanceOfTarget, value);
                        } catch (NoSuchFieldException e) {
                            DebugUtil.getInstance().error(Constant.TAG, "Inject values for activity error! [" + e.getMessage() + "]");
                        }
                    }
                }
            }
        }
        return (Activity) instanceOfTarget;
    }
}
