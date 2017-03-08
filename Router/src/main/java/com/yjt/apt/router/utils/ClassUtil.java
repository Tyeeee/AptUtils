package com.yjt.apt.router.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.constant.Constant;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dalvik.system.DexFile;

public class ClassUtil {

    private static final String EXTRACTED_NAME_EXT = ".classes";
    private static final String EXTRACTED_SUFFIX = ".zip";
    private static final String SECONDARY_FOLDER_NAME = "code_cache" + File.separator + "secondary-dexes";
    private static final String SP_FILE = "multidex.version";
    private static final String KEY_DEX_NUMBER = "dex.number";

    private static final int VM_WITH_MULTIDEX_VERSION_MAJOR = 2;
    private static final int VM_WITH_MULTIDEX_VERSION_MINOR = 1;

    private static ClassUtil classUtil;

    private ClassUtil() {
        // cannot be instantiated
    }

    public static synchronized ClassUtil getInstance() {
        if (classUtil == null) {
            classUtil = new ClassUtil();
        }
        return classUtil;
    }

    public static synchronized void releaseInstance() {
        if (classUtil != null) {
            classUtil = null;
        }
    }

    private SharedPreferences getMultiDexPreferences(Context context) {
        return context.getSharedPreferences(SP_FILE, Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? Context.MODE_PRIVATE : Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public List<String> getFileNameByPackageName(Context context, String packageName) throws PackageManager.NameNotFoundException, IOException {
        List<String> classNames = new ArrayList<>();
        for (String path : getSourcePaths(context)) {
            DebugUtil.getInstance().debug(Constant.TAG, "path: " + path);
            DexFile dexfile;
            if (path.endsWith(EXTRACTED_SUFFIX)) {
                //Don't use new DexFile(path), because it will throw "permission error in /data/dalvik-cache"
                dexfile = DexFile.loadDex(path, path + ".tmp", 0);
            } else {
                dexfile = new DexFile(path);
            }
            Enumeration<String> dexEntries = dexfile.entries();
            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
//                    DebugUtil.getInstance().debug(Constant.TAG, "packageName: " + packageName);
//                    DebugUtil.getInstance().debug(Constant.TAG, "className: " + className);
                if (className.contains(packageName)) {
                    classNames.add(className);
                }
            }
        }
        DebugUtil.getInstance().debug(Constant.TAG, "result:" + classNames.size() + " classes by packageName <" + packageName + ">");
        return classNames;
    }

    private List<String> getSourcePaths(Context context) throws PackageManager.NameNotFoundException, IOException {
        ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        File sourceApk = new File(applicationInfo.sourceDir);

        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(applicationInfo.sourceDir); //add the default apk path

//        如果VM已经支持了MultiDex，就不要去Secondary Folder加载 Classesx.zip了，那里已经么有了
//        通过是否存在sp中的multidex.version是不准确的，因为从低版本升级上来的用户，是包含这个sp配置的
        if (!isVMMultidexCapable()) {
            //the total dex numbers
            for (int secondaryNumber = 2; secondaryNumber <= getMultiDexPreferences(context).getInt(KEY_DEX_NUMBER, 1); secondaryNumber++) {
                //the prefix of extracted file, ie: test.classes
                // for each dex file, ie: test.classes2.zip, test.classes3.zip...
                File extractedFile = new File(new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME)
                        , sourceApk.getName() + EXTRACTED_NAME_EXT + secondaryNumber + EXTRACTED_SUFFIX);
                if (extractedFile.isFile()) {
                    sourcePaths.add(extractedFile.getAbsolutePath());
                    //we ignore the verify zip part
                } else {
                    throw new IOException("Missing extracted secondary dex file '" + extractedFile.getPath() + "'");
                }
            }
        }
        if (Router.getInstance().debuggable()) { // Search instant run support only debuggable
            sourcePaths.addAll(loadInstantRunDexFile(applicationInfo));
        }
        return sourcePaths;
    }

    private static List<String> loadInstantRunDexFile(ApplicationInfo applicationInfo) {
        List<String> instantRunSourcePaths = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && null != applicationInfo.splitSourceDirs) {
            // add the splite apk, normally for InstantRun, and newest version.
            instantRunSourcePaths.addAll(Arrays.asList(applicationInfo.splitSourceDirs));
            DebugUtil.getInstance().debug(Constant.TAG, "Found InstantRun support");
        } else {
            try {
                // This man is reflection from Google instant run sdk, he will tell me where the dex files go.
                Class pathsByInstantRun = Class.forName("com.android.tools.fd.runtime.Paths");
                Method getDexFileDirectory = pathsByInstantRun.getMethod("getDexFileDirectory", String.class);
                String instantRunDexPath = (String) getDexFileDirectory.invoke(null, applicationInfo.packageName);
                File instantRunFilePath = new File(instantRunDexPath);
                if (instantRunFilePath.exists() && instantRunFilePath.isDirectory()) {
                    File[] dexFile = instantRunFilePath.listFiles();
                    for (File file : dexFile) {
                        if (null != file && file.exists() && file.isFile() && file.getName().endsWith(".dex")) {
                            instantRunSourcePaths.add(file.getAbsolutePath());
                        }
                    }
                    DebugUtil.getInstance().debug(Constant.TAG, "Found InstantRun support");
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                DebugUtil.getInstance().error(Constant.TAG, "InstantRun support error:" + e.getMessage());
            }
        }
        return instantRunSourcePaths;
    }

    private boolean isVMMultidexCapable() {
        boolean isMultidexCapable = false;
        String vmName = null;
        try {
            if (isYunOS()) {    // YunOS需要特殊判断
                vmName = "'YunOS'";
                isMultidexCapable = Integer.valueOf(System.getProperty("ro.build.version.sdk")) >= 21;
            } else {    // 非YunOS原生Android
                vmName = "'Android'";
                String version = System.getProperty("java.vm.version");
                DebugUtil.getInstance().debug(Constant.TAG, "java.vm.version:" + version);
                if (TextUtils.isEmpty(version)) {
                    Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(version);
                    if (matcher.matches()) {
                        try {
                            int major = Integer.parseInt(matcher.group(1));
                            int minor = Integer.parseInt(matcher.group(2));
                            isMultidexCapable = (major > VM_WITH_MULTIDEX_VERSION_MAJOR)
                                    || ((major == VM_WITH_MULTIDEX_VERSION_MAJOR)
                                    && (minor >= VM_WITH_MULTIDEX_VERSION_MINOR));
                        } catch (NumberFormatException ignore) {
                            // let isMultidexCapable be false
                            isMultidexCapable = false;
                        }
                    }
                }
            }
        } catch (Exception ignore) {

        }
        DebugUtil.getInstance().debug(Constant.TAG, "VM with name:" + vmName + (isMultidexCapable ? " has multidex support" : " does not have multidex support"));
        return isMultidexCapable;
    }

    /**
     * 判断系统是否为YunOS系统
     */
    private boolean isYunOS() {
        try {
            String version = System.getProperty("ro.yunos.version");
            String vmName = System.getProperty("java.vm.name");
            return (vmName != null && vmName.toLowerCase().contains("lemur"))
                    || (version != null && version.trim().length() > 0);
        } catch (Exception ignore) {
            return false;
        }
    }
}
