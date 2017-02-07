package com.yjt.apt.router.utils;

import android.text.TextUtils;
import android.util.Log;

import com.yjt.apt.router.listener.template.ILog;

public class DebugUtil implements ILog {

    private boolean isShowLog = false;
    private boolean isShowStackTrace = false;
    private boolean isMonitorMode = false;

    private static DebugUtil DebugUtil;

    private DebugUtil() {
        // cannot be instantiated
    }

    public static synchronized DebugUtil getInstance() {
        if (DebugUtil == null) {
            DebugUtil = new DebugUtil();
        }
        return DebugUtil;
    }

    public static void releaseInstance() {
        if (DebugUtil != null) {
            DebugUtil = null;
        }
    }

    public void showLog(boolean showLog) {
        isShowLog = showLog;
    }

    public void showStackTrace(boolean showStackTrace) {
        isShowStackTrace = showStackTrace;
    }

    public void showMonitor(boolean showMonitor) {
        isMonitorMode = showMonitor;
    }

    @Override
    public void debug(String tag, String message) {
        if (isShowLog) {
            Log.d(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(Thread.currentThread().getStackTrace()[3]));
        }
    }

    @Override
    public void info(String tag, String message) {
        if (isShowLog) {
            Log.i(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(Thread.currentThread().getStackTrace()[3]));
        }
    }

    @Override
    public void warning(String tag, String message) {
        if (isShowLog) {
            Log.w(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(Thread.currentThread().getStackTrace()[3]));
        }
    }

    @Override
    public void error(String tag, String message) {
        if (isShowLog) {
            Log.e(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(Thread.currentThread().getStackTrace()[3]));
        }
    }

    @Override
    public void monitor(String message) {
        if (isShowLog && isMonitorMode()) {
            Log.d(defaultTag + "::monitor", message + getExtInfo(Thread.currentThread().getStackTrace()[3]));
        }
    }

    @Override
    public boolean isMonitorMode() {
        return isMonitorMode;
    }

    @Override
    public String getDefaultTag() {
        return defaultTag;
    }

    public String getExtInfo(StackTraceElement stackTraceElement) {
        String separator = " & ";
        StringBuilder builder = new StringBuilder("[");
        if (isShowStackTrace) {
            String threadName = Thread.currentThread().getName();
            String fileName = stackTraceElement.getFileName();
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();
            long threadID = Thread.currentThread().getId();
            int lineNumber = stackTraceElement.getLineNumber();
            builder.append("ThreadId=").append(threadID).append(separator);
            builder.append("ThreadName=").append(threadName).append(separator);
            builder.append("FileName=").append(fileName).append(separator);
            builder.append("ClassName=").append(className).append(separator);
            builder.append("MethodName=").append(methodName).append(separator);
            builder.append("LineNumber=").append(lineNumber);
        }
        builder.append(" ] ");
        return builder.toString();
    }
}