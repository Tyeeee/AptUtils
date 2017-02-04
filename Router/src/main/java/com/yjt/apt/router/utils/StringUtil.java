package com.yjt.apt.router.utils;

import android.net.Uri;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StringUtil {

    private static StringUtil stringUtil;

    private StringUtil() {
        // cannot be instantiated
    }

    public static synchronized StringUtil getInstance() {
        if (stringUtil == null) {
            stringUtil = new StringUtil();
        }
        return stringUtil;
    }

    public static void releaseInstance() {
        if (stringUtil != null) {
            stringUtil = null;
        }
    }

    public String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            builder.append("    at ").append(element.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

    public Map<String, String> splitQueryParameters(Uri rawUri) {
        String query = rawUri.getEncodedQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        Map<String, String> paramMap = new LinkedHashMap<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;
            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }
            String name = query.substring(start, separator);
            if (!android.text.TextUtils.isEmpty(name)) {
                String value = (separator == end ? "" : query.substring(separator + 1, end));
                paramMap.put(Uri.decode(name), Uri.decode(value));
            }
            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());
        return Collections.unmodifiableMap(paramMap);
    }

    public String getLeft(String key) {
        if (key.contains("|") && !key.endsWith("|")) {
            return key.substring(0, key.indexOf("|"));
        } else {
            return key;
        }
    }

    public String getRight(String key) {
        if (key.contains("|") && !key.startsWith("|")) {
            return key.substring(key.indexOf("|") + 1);
        } else {
            return key;
        }
    }
}
