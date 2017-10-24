# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 5          # 指定代码的压缩级别
-dontusemixedcaseclassnames   # 是否使用大小写混合
-dontpreverify           # 混淆时是否做预校验
-verbose                # 混淆时是否记录日志
-dontnote
-dontskipnonpubliclibraryclassmembers
-printconfiguration

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 混淆时所采用的算法

-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes Deprecated
-keepattributes SourceFile
-keepattributes LineNumberTable
-keepattributes EnclosingMethod  
-keepattributes *Annotation*
-keepattributes *JavascriptInterface*

-keep public class * extends android.app.Activity      
-keep public class * extends android.app.Application   
-keep public class * extends android.app.Service       
-keep public class * extends android.content.BroadcastReceiver  
-keep public class * extends android.content.ContentProvider    
-keep public class * extends android.app.backup.BackupAgentHelper 
-keep public class * extends android.preference.Preference        
-keep public class com.android.vending.licensing.ILicensingService    
-keep class * extends java.lang.annotation.Annotation { *; }

#-keep public class org.apache.commons.collections4.**{*;}
#-keep public class org.apache.commons.lang3.**{*;}
#-keep public class com.google.auto.service.**{*;}
#-keep public class com.squareup.javapoet.**{*;}
-keep public final class com.yjt.apt.router.Router
-keep public class com.yjt.apt.router.listener.callback.**{*;}
-keep public class com.yjt.apt.router.listener.service.**{*;}
-keep public class com.yjt.apt.router.listener.template.**{*;}
-keep public final class com.yjt.apt.router.model.**{*;}
-keep class com.yjt.apt.router.annotation.**{*;}

-keepclasseswithmembernames class * { 
    @com.yjt.apt.router.annotation.* <methods>; 
}
-keepclasseswithmembernames class * { 
    @com.yjt.apt.router.annotation.* <fields>; 
}
-keepclasseswithmembernames class * {  # 保持 native 方法不被混淆
    native <methods>;
}
-keepclasseswithmembers class * {   # 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {# 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity { # 保持自定义控件类不被混淆   
    public void *(android.view.View);
}
-keepclassmembers enum * {     # 保持枚举 enum 类不被混淆    
    public static **[] values();    
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable { # 保持 Parcelable 不被混淆  
    public static final android.os.Parcelable$Creator *;
}
-keep public class com.yjt.apt.router.**{*;}
-keep class * implements com.yjt.apt.router.listener.template.ISyringe{*;}
#-keepclassmembers class com.yjt.apt.router.Router { 
#    public static synchronized <methods>;
#    public synchronized <methods>;
#    public void destroy();
#   	public com.yjt.apt.router.model.Postcard build(java.lang.String);
#   	public com.yjt.apt.router.model.Postcard build(android.net.Uri);
#   	public com.yjt.apt.router.model.Postcard build(java.lang.String, java.lang.String);
#   	public *** navigation(java.lang.Class);
#   	public java.lang.Object navigation(android.content.Context, com.yjt.apt.router.model.Postcard, int, com.yjt.apt.router.listener.callback.NavigationCallback);
#   	public java.lang.Object navigation(android.content.Context, com.yjt.apt.router.model.Postcard, int);
#}
