package com.kraftplay.openiptv.auto;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.apps.auto.sdk.CarActivity;

import java.lang.reflect.Method;

public class CarKeyboardHelper {

    public interface OnTextCommittedListener {
        void onTextCommitted(String text);
    }

    public static CarActivity findCarActivity(Context context) {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof CarActivity) {
                return (CarActivity) ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static void startCarInput(Context context, String initialText, OnTextCommittedListener listener) {
        CarActivity activity = findCarActivity(context);
        if (activity == null) return;

        try {
            CarEditText editText = new CarEditText(activity);
            if (initialText != null && !initialText.isEmpty()) {
                editText.setText(initialText);
            }

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (listener != null && s != null) {
                        listener.onTextCommitted(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            editText.addTextChangedListener(watcher);

            editText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (listener != null) {
                        listener.onTextCommitted(editText.getText().toString());
                    }
                    stopCarInput(activity);
                    return true;
                }
                return false;
            });

            Method aMethod = CarActivity.class.getDeclaredMethod("a");
            aMethod.setAccessible(true);
            Object host = aMethod.invoke(activity);
            if (host != null) {
                Method startInputMethod = host.getClass().getMethod("startInput", View.class);
                startInputMethod.setAccessible(true);
                startInputMethod.invoke(host, editText);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void stopCarInput(CarActivity activity) {
        if (activity == null) return;
        try {
            Method aMethod = CarActivity.class.getDeclaredMethod("a");
            aMethod.setAccessible(true);
            Object host = aMethod.invoke(activity);
            if (host != null) {
                Method stopInputMethod = host.getClass().getMethod("stopInput");
                stopInputMethod.setAccessible(true);
                stopInputMethod.invoke(host);
            }
        } catch (Throwable ignored) {}
    }
}
