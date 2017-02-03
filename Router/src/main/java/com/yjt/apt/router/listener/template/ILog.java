package com.yjt.apt.router.listener.template;


import com.yjt.apt.router.constant.Constant;

public interface ILog {

    boolean isShowLog = false;
    boolean isShowStackTrace = false;
    String defaultTag = Constant.TAG;

    void showLog(boolean isShowLog);

    void showStackTrace(boolean isShowStackTrace);

    void debug(String tag, String message);

    void info(String tag, String message);

    void warning(String tag, String message);

    void error(String tag, String message);

    void monitor(String message);

    boolean isMonitorMode();

    String getDefaultTag();
}
