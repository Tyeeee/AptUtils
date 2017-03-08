package com.yjt.apt.router.constant;

public final class Constant {

    public static final String SDK_NAME = "Router";
    public static final String TAG = SDK_NAME + "-->";
    public static final String SEPARATOR = "$$";
    public static final String SUFFIX_ROOT = "Root";
    public static final String SUFFIX_INTERCEPTORS = "Interceptors";
    public static final String SUFFIX_PROVIDERS = "Providers";
    public static final String SUFFIX_AUTOWIRED = SEPARATOR + SDK_NAME + SEPARATOR + "Autowired";
    public static final String DOT = ".";
    public static final String ROUTE_ROOT_PAKCAGE = "com.yjt.apt.router.routes";

    // Java base type, copy from javax 'TypeKind'
    public static final int DEFINE_BOOLEAN = 0;
    public static final int DEFINE_BYTE = 1;
    public static final int DEFINE_SHORT = 2;
    public static final int DEFINE_INT = 3;
    public static final int DEFINE_LONG = 4;
    public static final int DEFINE_FLOAT = 6;
    public static final int DEFINE_DOUBLE = 7;
    public static final int DEFINE_STRING = 18;

    public static final String RAW_URI = "raw_uri";
    public static final String AUTO_INJECT = "auto_inject";
}
