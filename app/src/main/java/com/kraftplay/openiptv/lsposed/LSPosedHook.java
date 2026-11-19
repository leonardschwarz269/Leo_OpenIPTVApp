package com.kraftplay.openiptv.lsposed;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LSPosedHook implements IXposedHookLoadPackage {

    public static final String TARGET_APP = "com.kraftplay.openiptv";
    public static final String GEARHEAD_PKG = "com.google.android.projection.gearhead";
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GPLAY_PKG = "com.android.vending";
    private static final String IPM_STUB_PROXY = "android.content.pm.IPackageManager.Stub.Proxy";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (TARGET_APP.equals(lpparam.packageName)) {
            markSelfActive(lpparam);
        }

        if (GEARHEAD_PKG.equals(lpparam.packageName) ||
                GMS_PKG.equals(lpparam.packageName) ||
                GPLAY_PKG.equals(lpparam.packageName)) {

            XposedBridge.log("OpenIPTV: Hooking " + lpparam.packageName);
            hookInstallSource(lpparam);
            hookDrivingRestrictions(lpparam);
            hookSpeedAndDrivingState(lpparam);
            hookPackageManager(lpparam);
            hookPhenotypeFlags(lpparam);
            hookActivityWindowBounds(lpparam);
        }
    }

    private void markSelfActive(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> statusClass = XposedHelpers.findClass(
                    TARGET_APP + ".lsposed.LSPosedStatus",
                    lpparam.classLoader
            );
            XposedHelpers.setStaticBooleanField(statusClass, "isActive", true);
            XposedHelpers.findAndHookMethod(
                    statusClass,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
            );
            XposedBridge.log("OpenIPTV: LSPosed module marked active in-app");
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: markSelfActive failed: " + t.getMessage());
        }
    }

    /** 1. Белый список: обход подлинности установки для Android Auto 11.x - 18.0 (Android 11-16) */
    private void hookInstallSource(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            spoofInstallSourceInfoMethod("getInitiatingPackageName", lpparam.classLoader);
            spoofInstallSourceInfoMethod("getInstallingPackageName", lpparam.classLoader);
            spoofInstallSourceInfoMethod("getOriginatingPackageName", lpparam.classLoader);
            spoofInstallSourceInfoMethod("getUpdateOwnerPackageName", lpparam.classLoader);
        }

        // Подмена getInstallSourceInfo в IPackageManager (перегрузки с 1 и 2 аргументами)
        try {
            XposedHelpers.findAndHookMethod(
                    IPM_STUB_PROXY,
                    lpparam.classLoader,
                    "getInstallSourceInfo",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            modifyInstallSourceInfoResult(param);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: getInstallSourceInfo(String) hook skipped: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    IPM_STUB_PROXY,
                    lpparam.classLoader,
                    "getInstallSourceInfo",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            modifyInstallSourceInfoResult(param);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: getInstallSourceInfo(String, int) hook skipped: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    IPM_STUB_PROXY,
                    lpparam.classLoader,
                    "getInstallerPackageName",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String pkg = (String) param.args[0];
                            if (pkg != null && (pkg.equals(TARGET_APP) || pkg.startsWith(TARGET_APP + "."))) {
                                param.setResult(GPLAY_PKG);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    "android.app.ApplicationPackageManager",
                    lpparam.classLoader,
                    "getInstallerPackageName",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String pkg = (String) param.args[0];
                            if (pkg != null && (pkg.equals(TARGET_APP) || pkg.startsWith(TARGET_APP + "."))) {
                                param.setResult(GPLAY_PKG);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: getInstallerPackageName hook skipped: " + t.getMessage());
        }
    }

    private void modifyInstallSourceInfoResult(XC_MethodHook.MethodHookParam param) {
        String pkg = (String) param.args[0];
        if (pkg != null && (pkg.equals(TARGET_APP) || pkg.startsWith(TARGET_APP + "."))) {
            Object info = param.getResult();
            if (info != null) {
                try { XposedHelpers.setObjectField(info, "mInitiatingPackageName", GPLAY_PKG); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(info, "mInstallingPackageName", GPLAY_PKG); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(info, "mOriginatingPackageName", GPLAY_PKG); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(info, "mUpdateOwnerPackageName", GPLAY_PKG); } catch (Throwable ignored) {}
            }
        }
    }

    /** 2. Снятие ограничений UI в движении (CarUxRestrictions) для AA 18.0 */
    private void hookDrivingRestrictions(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] restrictionsClasses = new String[]{
                "com.google.android.apps.auto.sdk.CarUxRestrictions",
                "android.car.drivingstate.CarUxRestrictions",
                "com.google.android.gms.car.CarUxRestrictions"
        };
        for (String className : restrictionsClasses) {
            try {
                Class<?> uxClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (uxClass == null) continue;
                XposedHelpers.findAndHookMethod(uxClass, "isRequiresDistractionOptimization", XC_MethodReplacement.returnConstant(false));
                XposedHelpers.findAndHookMethod(uxClass, "getActiveRestrictions", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod(uxClass, "getMaxCumulativeContentItems", XC_MethodReplacement.returnConstant(Integer.MAX_VALUE));
                XposedHelpers.findAndHookMethod(uxClass, "getMaxRestrictedStringLength", XC_MethodReplacement.returnConstant(Integer.MAX_VALUE));
            } catch (Throwable t) {
                XposedBridge.log("OpenIPTV: hook " + className + " error: " + t.getMessage());
            }
        }
    }

    /** 3. Подмена датчиков: машина всегда на парковке (PARKED), скорость 0.0 */
    private void hookSpeedAndDrivingState(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            String[] speedClasses = new String[]{
                    "com.google.android.gearhead.sdk.speed.SpeedInfo",
                    "com.google.android.gms.car.speed.SpeedInfo"
            };
            for (String className : speedClasses) {
                Class<?> speedClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (speedClass == null) continue;
                XposedHelpers.findAndHookMethod(speedClass, "getSpeedKph", XC_MethodReplacement.returnConstant(0.0f));
                XposedHelpers.findAndHookMethod(speedClass, "isParked", XC_MethodReplacement.returnConstant(true));
            }

            String[] carDrivingManagerClasses = new String[]{
                    "com.google.android.gms.car.CarDrivingStateManager",
                    "android.car.drivingstate.CarDrivingStateManager"
            };
            for (String className : carDrivingManagerClasses) {
                Class<?> managerClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (managerClass == null) continue;
                // 1 = DRIVING_STATE_PARKED
                XposedHelpers.findAndHookMethod(managerClass, "getDrivingState", XC_MethodReplacement.returnConstant(1));
                XposedHelpers.findAndHookMethod(managerClass, "getCurrentCarDrivingState", XC_MethodReplacement.returnConstant(1));
            }
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: hook speed error: " + t.getMessage());
        }
    }

    /** 4. Разрешение оптимизации отвлечений и белого списка пакетов для AA 18.0 */
    private void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] carPmClasses = new String[]{
                "com.google.android.gms.car.CarPackageManager",
                "android.car.content.pm.CarPackageManager",
                "com.google.android.gearhead.car.policy.CarPackageManager"
        };
        for (String className : carPmClasses) {
            try {
                Class<?> pmClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (pmClass == null) continue;
                XposedHelpers.findAndHookMethod(pmClass, "isActivityDistractionOptimized", String.class, String.class, XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(pmClass, "isPackageDistractionOptimized", String.class, XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(pmClass, "isActivityAllowed", String.class, String.class, XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(pmClass, "isPackageAllowed", String.class, XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(pmClass, "isAllowed", String.class, XC_MethodReplacement.returnConstant(true));
            } catch (Throwable ignored) {}
        }

        try {
            Class<?> carActivityClass = XposedHelpers.findClassIfExists(
                    "com.google.android.apps.auto.sdk.CarActivity",
                    lpparam.classLoader
            );
            if (carActivityClass != null) {
                XposedHelpers.findAndHookMethod(carActivityClass, "isDistractionOptimized", XC_MethodReplacement.returnConstant(true));
            }
        } catch (Throwable ignored) {}
    }

    /** 5. Сброс серверных флагов Phenotype (блокировки движения) */
    private void hookPhenotypeFlags(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] flagClasses = new String[]{
                "com.google.android.projection.gearhead.common.Flags",
                "com.google.android.gms.car.common.Flags"
        };
        for (String className : flagClasses) {
            try {
                Class<?> flagClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
                if (flagClass == null) continue;
                XposedHelpers.findAndHookMethod(flagClass, "isDistractionOptimizationRequired", XC_MethodReplacement.returnConstant(false));
            } catch (Throwable ignored) {}
        }
    }

    private void spoofInstallSourceInfoMethod(String methodName, ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.content.pm.InstallSourceInfo",
                    classLoader,
                    methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(GPLAY_PKG);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("OpenIPTV: hook InstallSourceInfo." + methodName + " failed: " + t.getMessage());
        }
    }

    /** 6. Принудительное расширение геометрии окна активности во весь физический экран */
    private void hookActivityWindowBounds(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> carActivityClass = XposedHelpers.findClassIfExists(
                    "com.google.android.apps.auto.sdk.CarActivity",
                    lpparam.classLoader
            );
            if (carActivityClass != null) {
                XposedHelpers.findAndHookMethod(
                        carActivityClass,
                        "onCreate",
                        android.os.Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Object activity = param.thisObject;
                                if (activity != null && activity.getClass().getName().contains("FS")) {
                                    try {
                                        android.view.Window win = (android.view.Window) XposedHelpers.callMethod(activity, "getWindow");
                                        if (win != null) {
                                            win.setLayout(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            );
                                            win.addFlags(
                                                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                                                            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                                            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                            );
                                            int fullScreenFlags = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                    | android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
                                                    | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                                    | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                    | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
                                                    | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                                            win.getDecorView().setSystemUiVisibility(fullScreenFlags);
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                );
            }
        } catch (Throwable ignored) {}
    }
}
