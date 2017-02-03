package com.yjt.apt.router.compiler.messager;

import com.yjt.apt.router.compiler.constant.Constant;

import org.apache.commons.lang3.StringUtils;

import javax.tools.Diagnostic;

/**
 * Simplify the messager.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/22 上午11:48
 */
public class Messager {

    private javax.annotation.processing.Messager messager;

    public Messager(javax.annotation.processing.Messager messager) {
        this.messager = messager;
    }

    public void info(CharSequence info) {
        if (StringUtils.isNotEmpty(info)) {
            messager.printMessage(Diagnostic.Kind.NOTE, Constant.PREFIX_OF_LOGGER + info);
        }
    }

    public void error(CharSequence error) {
        if (StringUtils.isNotEmpty(error)) {
            messager.printMessage(Diagnostic.Kind.ERROR, Constant.PREFIX_OF_LOGGER + "An exception is encountered, [" + error + "]");
        }
    }

    public void error(Throwable error) {
        if (null != error) {
            messager.printMessage(Diagnostic.Kind.ERROR, Constant.PREFIX_OF_LOGGER + "An exception is encountered, [" + error.getMessage() + "]" + "\n" + formatStackTrace(error.getStackTrace()));
        }
    }

    public void warning(CharSequence warning) {
        if (StringUtils.isNotEmpty(warning)) {
            messager.printMessage(Diagnostic.Kind.WARNING, Constant.PREFIX_OF_LOGGER + warning);
        }
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            builder.append("    at ").append(element.toString());
            builder.append("\n");
        }
        return builder.toString();
    }
}
