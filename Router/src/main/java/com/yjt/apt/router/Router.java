package com.yjt.apt.router;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.core.InstrumentationHook;
import com.yjt.apt.router.core.LogisticsCenter;
import com.yjt.apt.router.exception.InitializedException;
import com.yjt.apt.router.exception.MainProcessException;
import com.yjt.apt.router.exception.RouteNotFoundException;
import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.callback.NavigationCallback;
import com.yjt.apt.router.listener.service.DegradeService;
import com.yjt.apt.router.listener.service.PathReplaceService;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.thread.DefaultThreadPoolExecutor;
import com.yjt.apt.router.utils.DebugUtil;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

public final class Router {

    private volatile boolean monitorMode = false;
    private volatile boolean debuggable = false;
    private volatile boolean autoInject = false;
    private volatile static Router instance = null;
    private volatile static boolean hasInitialize = false;
    private volatile static ThreadPoolExecutor executor = DefaultThreadPoolExecutor.getInstance();
    private Context context;

    private Router() {
        // cannot be instantiated
    }

    public static synchronized Router getInstance() {
        if (!hasInitialize) {
            DebugUtil.getInstance().info(Constant.TAG, "ARouter init start.");
            throw new InitializedException("ARouterCore::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (Router.class){
                    if (instance == null) {
                        instance = new Router();
                    }
                }
            }
            return instance;
        }
    }

    public static void releaseInstance() {
        if (instance != null) {
            instance = null;
        }
    }

    private synchronized boolean initialize(Application application) {
        context = application;
        LogisticsCenter.getInstance().initialize(context, executor);
        DebugUtil.getInstance().info(Constant.TAG, "ARouter init success!");
        hasInitialize = true;
        LogisticsCenter.getInstance().initializeInterceptors();
        DebugUtil.getInstance().info(Constant.TAG, "ARouter init over.");
        return true;
    }

    public void destroy() {
        if (debuggable()) {
            hasInitialize = false;
            LogisticsCenter.getInstance().suspend();
            DebugUtil.getInstance().info(Constant.TAG, "ARouter destroy success!");
        } else {
            throw new MainProcessException("ARouter::destroy can be used in debug mode only!");
        }
    }

    synchronized void openDebug() {
        debuggable = true;
        DebugUtil.getInstance().info(Constant.TAG, "ARouter openDebug");
    }

    synchronized void openLog() {
        DebugUtil.getInstance().showLog(true);
        DebugUtil.getInstance().info(Constant.TAG, "ARouter openLog");
    }

    synchronized void enableAutoInject() {
        autoInject = true;
    }

    public boolean canAutoInject() {
        return autoInject;
    }

    void attachBaseContext() {
        Log.i(Constant.TAG, "ARouter start attachBaseContext");
        try {
            Class<?> mMainThreadClass = Class.forName("android.app.ActivityThread");

            // Get current main thread.
            Method getMainThread = mMainThreadClass.getDeclaredMethod("currentActivityThread");
            getMainThread.setAccessible(true);
            Object currentActivityThread = getMainThread.invoke(null);

            // The field contain instrumentation.
            Field mInstrumentationField = mMainThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);

            // Hook current instrumentation
            mInstrumentationField.set(currentActivityThread, new InstrumentationHook());
            Log.i(Constant.TAG, "ARouter hook instrumentation success!");
        } catch (Exception ex) {
            Log.e(Constant.TAG, "ARouter hook instrumentation failed! [" + ex.getMessage() + "]");
        }
    }

    synchronized void printStackTrace() {
        DebugUtil.getInstance().showStackTrace(true);
        DebugUtil.getInstance().info(Constant.TAG, "ARouter printStackTrace");
    }

    synchronized void setExecutor(ThreadPoolExecutor tpe) {
        executor = tpe;
    }

    synchronized void monitorMode() {
        monitorMode = true;
        DebugUtil.getInstance().info(Constant.TAG, "ARouter monitorMode on");
    }

    boolean isMonitorMode() {
        return monitorMode;
    }

    public boolean debuggable() {
        return debuggable;
    }

    protected Postcard build(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new MainProcessException(Constant.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService pService = navigation(PathReplaceService.class);
            if (null != pService) {
                path = pService.forString(path);
            }
            return build(path, extractGroup(path));
        }
    }

    protected Postcard build(Uri uri) {
        if (null == uri || StringUtils.isEmpty(uri.toString())) {
            throw new MainProcessException(Constant.TAG + "Parameter invalid!");
        } else {
            PathReplaceService pService = navigation(PathReplaceService.class);
            if (null != pService) {
                uri = pService.forUri(uri);
            }
            return new Postcard(uri.getPath(), extractGroup(uri.getPath()), uri, null);
        }
    }

    protected Postcard build(String path, String group) {
        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(group)) {
            throw new MainProcessException(Constant.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService pService = navigation(PathReplaceService.class);
            if (null != pService) {
                path = pService.forString(path);
            }
            return new Postcard(path, group);
        }
    }

    private String extractGroup(String path) {
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new MainProcessException(Constant.TAG + "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");
        }

        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (StringUtils.isEmpty(defaultGroup)) {
                throw new MainProcessException(Constant.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            DebugUtil.getInstance().warning(Constant.TAG, "Failed to extract default group! " + e.getMessage());
            return null;
        }
    }

    void afterInit() {
        LogisticsCenter.getInstance().initializeInterceptors();
    }

    public <T> T navigation(Class<? extends T> service) {
        try {
            Postcard postcard = LogisticsCenter.getInstance().buildProvider(service.getSimpleName());
            LogisticsCenter.getInstance().completion(postcard);
            return (T) postcard.getProvider();
        } catch (RouteNotFoundException ex) {
            DebugUtil.getInstance().warning(Constant.TAG, ex.getMessage());
            return null;
        }
    }

    public Object navigation(final Context ctx, final Postcard postcard, final int requestCode, NavigationCallback callback) {
        try {
            LogisticsCenter.getInstance().completion(postcard);
        } catch (RouteNotFoundException ex) {
            DebugUtil.getInstance().warning(Constant.TAG, ex.getMessage());
            if (debuggable()) { // Show friendly tips for user.
                Toast.makeText(ctx, "There's no route matched!\n" +
                        " Path = [" + postcard.getPath() + "]\n" +
                        " Group = [" + postcard.getGroup() + "]", Toast.LENGTH_LONG).show();
            }
            if (null != callback) {
                callback.onLost(postcard);
            } else {    // No callback for this invoke, then we use the global degrade service.
                DegradeService service = navigation(DegradeService.class);
                if (null != service) {
                    service.onLost(context, postcard);
                }
            }

            return null;
        }

        if (null != callback) {
            callback.onFound(postcard);
        }

        if (!postcard.isGreenChannal()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
            LogisticsCenter.getInstance().interception(postcard, new InterceptorCallback() {

                @Override
                public void onContinue(Postcard postcard) {
                    _navigation(context, postcard, requestCode);
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    DebugUtil.getInstance().info(Constant.TAG, "Navigation failed, termination by interceptor : " + exception.getMessage());
                }
            });
        } else {
            return _navigation(context, postcard, requestCode);
        }
        return null;
    }

    private Object _navigation(final Context ctx, final Postcard postcard, final int requestCode) {
        Context currentContext = null == ctx ? this.context : ctx;
        switch (postcard.getType()) {
            case ACTIVITY:
                // Build intent
                Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());
                // Set flags.
                int flag = postcard.getFlag();
                if (-1 != flag) {
                    intent.setFlags(flag);
                } else if (!(currentContext instanceof Activity)) {    // Non activity, need less one flag.
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                // Judgment activity start type.
                if (requestCode > 0) {  // RequestCode exist, need startActivityForResult, so this context must son of activity.
                    ((Activity) currentContext).startActivityForResult(intent, requestCode);
                } else {
                    currentContext.startActivity(intent);
                }
                break;
            case PROVIDER:
                return postcard.getProvider();
            case BOARDCAST:
            case CONTENT_PROVIDER:
            case FRAGMENT:
            case METHOD:
            case SERVICE:
            default:
                return null;
        }
        return null;
    }
}
