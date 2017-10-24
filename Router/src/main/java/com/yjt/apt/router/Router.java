package com.yjt.apt.router;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.core.LogisticsCenter;
import com.yjt.apt.router.exception.MainProcessException;
import com.yjt.apt.router.exception.RouteNotFoundException;
import com.yjt.apt.router.listener.callback.InterceptorCallback;
import com.yjt.apt.router.listener.callback.NavigationCallback;
import com.yjt.apt.router.listener.service.AutowireService;
import com.yjt.apt.router.listener.service.DegradeService;
import com.yjt.apt.router.listener.service.InterceptorService;
import com.yjt.apt.router.listener.service.PathReplaceService;
import com.yjt.apt.router.model.Postcard;
import com.yjt.apt.router.thread.DefaultThreadPoolExecutor;
import com.yjt.apt.router.utils.DebugUtil;

public final class Router {

    private volatile boolean monitorMode;
    private volatile boolean debuggable;
    private volatile static Router instance;
    private volatile static boolean hasInitialized;
    //    private volatile static ThreadPoolExecutor executor = DefaultThreadPoolExecutor.getInstance();
    private static Context context;
    private static InterceptorService interceptorService;

    private Router() {
        // cannot be instantiated
    }

    public static synchronized Router getInstance() {
//        if (!hasInitialized) {
//            throw new InitializedException("RouterCore::Init::Invoke initialize(context) first!");
//        } else 
        if (instance == null) {
            synchronized (Router.class){
                if (instance == null) {
                    instance = new Router();
                }
            }
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        if (instance != null) {
            instance = null;
        }
    }

    public synchronized boolean initialize(Application application) {
        if (!hasInitialized) {
            DebugUtil.getInstance().info(Constant.TAG, "Router initialize start.");
            context = application;
            LogisticsCenter.getInstance().initialize(context, DefaultThreadPoolExecutor.getInstance());
            DebugUtil.getInstance().info(Constant.TAG, "Router initialize success!");
            hasInitialized = true;
            interceptorService = (InterceptorService) build("/router/service/interceptor").navigation(context);
            DebugUtil.getInstance().info(Constant.TAG, "Router initialize over.");
        } else {
            DebugUtil.getInstance().info(Constant.TAG, "Router has been initialized.");
        }
        return true;
    }

    public void destroy() {
        if (debuggable) {
            hasInitialized = false;
            LogisticsCenter.getInstance().suspend();
            DebugUtil.getInstance().info(Constant.TAG, "Router destroy success!");
        } else {
            throw new MainProcessException("Router::destroy can be used in debug mode only!");
        }
    }

    public synchronized void openDebug() {
        debuggable = true;
        DebugUtil.getInstance().info(Constant.TAG, "Router openDebug");
    }

    public synchronized void openLog() {
        DebugUtil.getInstance().showLog(true);
        DebugUtil.getInstance().info(Constant.TAG, "Router openLog");
    }

    public void inject(Object object) {
        AutowireService autowireService = ((AutowireService) build("/router/service/autowire").navigation(context));
        if (null != autowireService) {
            autowireService.autowire(object);
        }
    }

    synchronized void printStackTrace() {
        DebugUtil.getInstance().showStackTrace(true);
        DebugUtil.getInstance().info(Constant.TAG, "Router printStackTrace");
    }

//    synchronized void setExecutor(ThreadPoolExecutor tpe) {
//        executor = tpe;
//    }

    synchronized void setMonitorMode() {
        monitorMode = true;
        DebugUtil.getInstance().info(Constant.TAG, "Router setMonitorMode on");
    }

    boolean isMonitorMode() {
        return monitorMode;
    }

    public boolean debuggable() {
        return debuggable;
    }

    public Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new MainProcessException(Constant.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService service = navigation(PathReplaceService.class);
            if (null != service) {
                path = service.forString(path);
            }
            return build(path, extractGroup(path));
        }
    }

    public Postcard build(Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            throw new MainProcessException(Constant.TAG + "Parameter invalid!");
        } else {
            PathReplaceService service = navigation(PathReplaceService.class);
            if (null != service) {
                uri = service.forUri(uri);
            }
            return new Postcard(uri.getPath(), extractGroup(uri.getPath()), uri, null);
        }
    }

    public Postcard build(String path, String group) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new MainProcessException(Constant.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService service = navigation(PathReplaceService.class);
            if (null != service) {
                path = service.forString(path);
            }
            DebugUtil.getInstance().info(Constant.TAG, path);
            DebugUtil.getInstance().info(Constant.TAG, group);
            return new Postcard(path, group);
        }
    }

    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new MainProcessException(Constant.TAG + "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");
        }
        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            DebugUtil.getInstance().info(Constant.TAG, defaultGroup);
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new MainProcessException(Constant.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (StringIndexOutOfBoundsException e) {
            DebugUtil.getInstance().error(Constant.TAG, "Failed to extract default group! " + e.getMessage());
            return null;
        }
    }

    public <T> T navigation(Class<? extends T> service) {
        try {
            Postcard postcard = LogisticsCenter.getInstance().buildProvider(service.getSimpleName());
            LogisticsCenter.getInstance().completion(postcard);
            return (T) postcard.getProvider();
        } catch (RouteNotFoundException ex) {
            DebugUtil.getInstance().error(Constant.TAG, ex.getMessage());
            return null;
        }
    }

    public Object navigation(final Context ctx, final Postcard postcard, final int requestCode, NavigationCallback callback) {
        try {
            LogisticsCenter.getInstance().completion(postcard);
        } catch (RouteNotFoundException e) {
            DebugUtil.getInstance().error(Constant.TAG, e.getMessage());
            if (debuggable) { // Show friendly tips for user.
                Toast.makeText(ctx, "There's no route matched!\n" +
                        " Path = [" + postcard.getPath() + "]\n" +
                        " Group = [" + postcard.getGroup() + "]", Toast.LENGTH_LONG).show();
            }
            if (null != callback) {
                callback.onLost(postcard);
            } else {    // No callback for this invoke, then we use the global degrade service.
                DegradeService service = navigation(DegradeService.class);
                if (null != service) {
                    service.onLost(ctx, postcard);
                }
            }
            return null;
        }

        if (null != callback) {
            callback.onFound(postcard);
        }

        if (!postcard.isGreenChannal()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
            interceptorService.intercept(postcard, new InterceptorCallback() {

                @Override
                public void onContinue(Postcard postcard) {
                    navigation(ctx, postcard, requestCode);
                }

                @Override
                public void onInterrupt(Throwable throwable) {
                    DebugUtil.getInstance().info(Constant.TAG, "Navigation failed, termination by interceptor : " + throwable.getMessage());
                }
            });
        } else {
            return navigation(context, postcard, requestCode);
        }
        return null;
    }

    private Object navigation(final Context ctx, final Postcard postcard, final int requestCode) {
        Context currentContext = ctx == null ? context : ctx;
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
