package com.kraftplay.openiptv.lsposed

import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class LSPosedHook : IXposedHookLoadPackage {

    companion object {
        const val TARGET_APP = "com.kraftplay.openiptv"
        const val GEARHEAD_PKG = "com.google.android.projection.gearhead"
        const val GPLAY_PKG = "com.android.vending"
        private const val IPM_STUB_PROXY = "android.content.pm.IPackageManager.Stub.Proxy"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == TARGET_APP) {
            markSelfActive(lpparam)
        }
        if (lpparam.packageName == GEARHEAD_PKG) {
            XposedBridge.log("OpenIPTV: Hooking Android Auto (Gearhead)")
            hookInstallSource(lpparam)
            hookDrivingRestrictions(lpparam)
            hookSpeedAndDrivingState(lpparam)
            hookPhenotypeFlags(lpparam)
        }
    }

    private fun markSelfActive(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val statusClass = XposedHelpers.findClass(
                "$TARGET_APP.lsposed.LSPosedStatus",
                lpparam.classLoader
            )
            XposedHelpers.setStaticBooleanField(statusClass, "isActive", true)
            XposedHelpers.findAndHookMethod(
                statusClass,
                "isModuleActive",
                XC_MethodReplacement.returnConstant(true)
            )
            XposedBridge.log("OpenIPTV: LSPosed module marked active in-app")
        } catch (t: Throwable) {
            XposedBridge.log("OpenIPTV: markSelfActive failed: ${t.message}")
        }
    }

    /** 1. Белый список: подменяем источник установки на Google Play */
    private fun hookInstallSource(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            spoofVending("android.content.pm.InstallSourceInfo", "getInitiatingPackageName", lpparam.classLoader)
            spoofVending("android.content.pm.InstallSourceInfo", "getInstallingPackageName", lpparam.classLoader)
            spoofVending("android.content.pm.InstallSourceInfo", "getOriginatingPackageName", lpparam.classLoader)
        }

        try {
            XposedHelpers.findAndHookMethod(
                IPM_STUB_PROXY,
                lpparam.classLoader,
                "getInstallerPackageName",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val pkg = param.args[0] as? String
                        if (pkg == TARGET_APP || pkg?.startsWith("$TARGET_APP.") == true) {
                            param.result = GPLAY_PKG
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("OpenIPTV: getInstallerPackageName hook skipped: ${t.message}")
        }
    }

    /** 2. Снятие ограничений UI в движении (CarUxRestrictions) */
    private fun hookDrivingRestrictions(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val uxClass = XposedHelpers.findClassIfExists(
                "com.google.android.apps.auto.sdk.CarUxRestrictions",
                lpparam.classLoader
            )
            uxClass?.let {
                XposedHelpers.findAndHookMethod(it, "isRequiresDistractionOptimization", XC_MethodReplacement.returnConstant(false))
                XposedHelpers.findAndHookMethod(it, "getActiveRestrictions", XC_MethodReplacement.returnConstant(0))
                XposedHelpers.findAndHookMethod(it, "getMaxCumulativeContentItems", XC_MethodReplacement.returnConstant(Int.MAX_VALUE))
                XposedHelpers.findAndHookMethod(it, "getMaxRestrictedStringLength", XC_MethodReplacement.returnConstant(Int.MAX_VALUE))
            }
        } catch (t: Throwable) {
            XposedBridge.log("OpenIPTV: hook CarUxRestrictions error: ${t.message}")
        }
    }

    /** 3. Подмена датчиков: машина всегда на парковке (PARKED), скорость 0.0 */
    private fun hookSpeedAndDrivingState(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val speedClass = XposedHelpers.findClassIfExists(
                "com.google.android.gearhead.sdk.speed.SpeedInfo",
                lpparam.classLoader
            )
            speedClass?.let {
                XposedHelpers.findAndHookMethod(it, "getSpeedKph", XC_MethodReplacement.returnConstant(0.0f))
                XposedHelpers.findAndHookMethod(it, "isParked", XC_MethodReplacement.returnConstant(true))
            }

            val carDrivingManager = XposedHelpers.findClassIfExists(
                "com.google.android.gms.car.CarDrivingStateManager",
                lpparam.classLoader
            )
            carDrivingManager?.let {
                // 1 = DRIVING_STATE_PARKED
                XposedHelpers.findAndHookMethod(it, "getDrivingState", XC_MethodReplacement.returnConstant(1))
            }
        } catch (t: Throwable) {
            XposedBridge.log("OpenIPTV: hook speed error: ${t.message}")
        }
    }

    /** 4. Сброс серверных флагов Phenotype (блокировки движения) */
    private fun hookPhenotypeFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val flagClass = XposedHelpers.findClassIfExists(
                "com.google.android.projection.gearhead.common.Flags",
                lpparam.classLoader
            ) ?: return

            XposedHelpers.findAndHookMethod(
                flagClass,
                "isDistractionOptimizationRequired",
                XC_MethodReplacement.returnConstant(false)
            )
        } catch (_: Throwable) { }
    }

    private fun spoofVending(className: String, methodName: String, classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                className,
                classLoader,
                methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = GPLAY_PKG
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("OpenIPTV: hook $className.$methodName failed: ${t.message}")
        }
    }
}