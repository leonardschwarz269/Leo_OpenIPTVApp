package com.kraftplay.openiptv.auto;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.R;
import androidx.car.app.AppManager;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.validation.HostValidator;

public class OpenIptvCarServiceFS extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    public int getMinCarApiLevel() {
        return 1;
    }

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new OpenIptvCarNavScreen(getCarContext());
            }
        };
    }

    public static class OpenIptvCarNavScreen extends Screen implements SurfaceCallback {

        private VirtualDisplay virtualDisplay;
        private CarVideoPresentation presentation;
        private Surface surface;
        private int surfaceWidth = 1280;
        private int surfaceHeight = 720;
        private int surfaceDensity = 160;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public OpenIptvCarNavScreen(@NonNull CarContext carContext) {
            super(carContext);
            try {
                carContext.getCarService(AppManager.class).setSurfaceCallback(this);
            } catch (Throwable ignored) {}
        }

        @Override
        public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
            surface = surfaceContainer.getSurface();
            if (surface == null || !surface.isValid()) return;

            surfaceWidth = surfaceContainer.getWidth() > 0 ? surfaceContainer.getWidth() : 1280;
            surfaceHeight = surfaceContainer.getHeight() > 0 ? surfaceContainer.getHeight() : 720;
            surfaceDensity = surfaceContainer.getDpi() > 0 ? surfaceContainer.getDpi() : 160;

            mainHandler.post(this::createVirtualDisplayAndPresentation);
        }

        private void createVirtualDisplayAndPresentation() {
            if (surface == null || !surface.isValid()) return;
            releaseVirtualDisplay();

            try {
                ContextThemeWrapper themedContext = new ContextThemeWrapper(getCarContext(), R.style.Theme_AppCompat_NoActionBar);
                DisplayManager dm = (DisplayManager) themedContext.getSystemService(Context.DISPLAY_SERVICE);
                if (dm == null) return;

                virtualDisplay = dm.createVirtualDisplay(
                        "AABrowserWideDisplay",
                        surfaceWidth,
                        surfaceHeight,
                        surfaceDensity,
                        surface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                );

                if (virtualDisplay == null || virtualDisplay.getDisplay() == null) return;

                presentation = new CarVideoPresentation(themedContext, virtualDisplay.getDisplay());
                presentation.show();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        private void releaseVirtualDisplay() {
            if (presentation != null) {
                try {
                    presentation.dismiss();
                } catch (Throwable ignored) {}
                presentation = null;
            }
            if (virtualDisplay != null) {
                try {
                    virtualDisplay.release();
                } catch (Throwable ignored) {}
                virtualDisplay = null;
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
            surface = null;
            mainHandler.post(this::releaseVirtualDisplay);
        }

        @Override
        public void onClick(float x, float y) {
            mainHandler.post(() -> {
                if (presentation != null && presentation.getWindow() != null) {
                    long now = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
                    MotionEvent up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0);
                    View decor = presentation.getWindow().getDecorView();
                    decor.dispatchTouchEvent(down);
                    decor.dispatchTouchEvent(up);
                    down.recycle();
                    up.recycle();
                }
            });
        }

        @Override
        public void onScroll(float distanceX, float distanceY) {
            mainHandler.post(() -> {
                if (presentation != null && presentation.getWindow() != null) {
                    long now = SystemClock.uptimeMillis();
                    float scale = 0.55f;
                    float dx = distanceX * scale;
                    float dy = distanceY * scale;

                    float startX = surfaceWidth / 2f;
                    float startY = surfaceHeight / 2f;
                    float endX = startX - dx;
                    float endY = startY - dy;

                    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, startX, startY, 0);
                    MotionEvent move = MotionEvent.obtain(now, now + 15, MotionEvent.ACTION_MOVE, endX, endY, 0);
                    MotionEvent up = MotionEvent.obtain(now, now + 30, MotionEvent.ACTION_UP, endX, endY, 0);

                    View decor = presentation.getWindow().getDecorView();
                    decor.dispatchTouchEvent(down);
                    decor.dispatchTouchEvent(move);
                    decor.dispatchTouchEvent(up);

                    down.recycle();
                    move.recycle();
                    up.recycle();
                }
            });
        }

        @Override
        public void onFling(float velocityX, float velocityY) {
            onScroll(-velocityX / 15f, -velocityY / 15f);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            ActionStrip strip = new ActionStrip.Builder()
                    .addAction(Action.BACK)
                    .build();

            ActionStrip mapStrip = new ActionStrip.Builder()
                    .addAction(new Action.Builder(Action.PAN).build())
                    .build();

            return new NavigationTemplate.Builder()
                    .setActionStrip(strip)
                    .setMapActionStrip(mapStrip)
                    .build();
        }
    }
}
