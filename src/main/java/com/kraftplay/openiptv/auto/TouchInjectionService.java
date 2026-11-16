package com.kraftplay.openiptv.auto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

public class TouchInjectionService extends AccessibilityService {

    private static volatile TouchInjectionService instance = null;

    public static boolean isEnabled() {
        return instance != null;
    }

    public static boolean dispatchMotionEvent(MotionEvent event) {
        TouchInjectionService service = instance;
        if (service == null || event == null) return false;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (x < 0f || y < 0f) return true;

            Path path = new Path();
            path.moveTo(x, y);

            long duration = (action == MotionEvent.ACTION_MOVE) ? 20L : 50L;
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0L, duration);
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            return service.dispatchGesture(gesture, null, null);
        }
        return false;
    }

    public static boolean dispatchBack() {
        TouchInjectionService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
