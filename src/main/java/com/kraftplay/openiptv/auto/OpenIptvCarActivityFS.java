package com.kraftplay.openiptv.auto;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import de.robv.android.xposed.XposedHelpers;

public class OpenIptvCarActivityFS extends OpenIptvCarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setIgnoreConfigChanges(0xFFFFFFFF);
            try {
                Object host = XposedHelpers.callMethod(this, "a");
                if (host != null) {
                    XposedHelpers.callMethod(host, "setRequestedOrientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            } catch (Throwable ignored) {}

            if (getCarUiController() != null) {
                if (getCarUiController().getStatusBarController() != null) {
                    getCarUiController().getStatusBarController().hideAppHeader();
                }
                if (getCarUiController().getMenuController() != null) {
                    getCarUiController().getMenuController().hideMenuButton();
                }
            }
        } catch (Throwable ignored) {}

        Window win = getWindow();
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            win.addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );

            View decor = win.getDecorView();
            if (decor != null) {
                int fullScreenFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decor.setSystemUiVisibility(fullScreenFlags);
            }
        }
    }
}
