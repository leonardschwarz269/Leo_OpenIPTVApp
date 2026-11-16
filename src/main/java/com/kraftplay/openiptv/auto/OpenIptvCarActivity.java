package com.kraftplay.openiptv.auto;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.activity.ViewTreeOnBackPressedDispatcherOwner;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.R;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import com.google.android.apps.auto.sdk.CarActivity;
import com.kraftplay.openiptv.viewmodel.MainViewModel;

public class OpenIptvCarActivity extends CarActivity implements
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner,
        ActivityResultRegistryOwner {

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);
    private final SavedStateRegistryController savedStateRegistryController = SavedStateRegistryController.create(this);
    private final ViewModelStore appViewModelStore = new ViewModelStore();
    private final OnBackPressedDispatcher backDispatcher = new OnBackPressedDispatcher();

    public Window getWindow() {
        return c();
    }

    @NonNull
    @Override
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return backDispatcher;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @NonNull
    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return savedStateRegistryController.getSavedStateRegistry();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return appViewModelStore;
    }

    private final ActivityResultRegistry dummyActivityRegistry = new ActivityResultRegistry() {
        @Override
        public <I, O> void onLaunch(
                int requestCode,
                @NonNull ActivityResultContract<I, O> contract,
                I input,
                @Nullable ActivityOptionsCompat options
        ) {}
    };

    @NonNull
    @Override
    public ActivityResultRegistry getActivityResultRegistry() {
        return dummyActivityRegistry;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_NoActionBar);
        super.onCreate(savedInstanceState);
        savedStateRegistryController.performRestore(savedInstanceState);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        SharedPreferences prefs = getSharedPreferences("com.kraftplay.openiptv_preferences", MODE_PRIVATE);
        boolean isFullscreen = prefs == null || prefs.getBoolean("car_fullscreen_mode", true);

        try {
            setIgnoreConfigChanges(0xFFFFFFFF);
            if (getCarUiController() != null) {
                if (getCarUiController().getStatusBarController() != null) {
                    getCarUiController().getStatusBarController().hideAppHeader();
                }
                if (getCarUiController().getMenuController() != null) {
                    getCarUiController().getMenuController().hideMenuButton();
                }
            }
        } catch (Throwable ignored) {}

        if (getWindow() != null && getWindow().getDecorView() != null) {
            if (isFullscreen) {
                int fullScreenFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                getWindow().getDecorView().setSystemUiVisibility(fullScreenFlags);
            }

            ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
            ViewTreeViewModelStoreOwner.set(getWindow().getDecorView(), this);
            ViewTreeSavedStateRegistryOwner.set(getWindow().getDecorView(), this);
            ViewTreeOnBackPressedDispatcherOwner.set(getWindow().getDecorView(), this);
        }

        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_AppCompat_NoActionBar);
        ComposeView composeView = new ComposeView(themedContext);
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow.INSTANCE);
        ViewTreeLifecycleOwner.set(composeView, this);
        ViewTreeViewModelStoreOwner.set(composeView, this);
        ViewTreeSavedStateRegistryOwner.set(composeView, this);
        ViewTreeOnBackPressedDispatcherOwner.set(composeView, this);

        MainViewModel viewModel = new MainViewModel(resolveApplication());

        CarVideoPresentationKt.launchCarAppRoot(composeView, this, viewModel);
        setContentView(composeView);

        if (composeView.getParent() instanceof View parent) {
            ViewTreeLifecycleOwner.set(parent, this);
            ViewTreeViewModelStoreOwner.set(parent, this);
            ViewTreeSavedStateRegistryOwner.set(parent, this);
            ViewTreeOnBackPressedDispatcherOwner.set(parent, this);
        }
    }

    private Application resolveApplication() {
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
            if (app != null) return app;
        } catch (Throwable ignored) {}
        Context ctx = getApplicationContext();
        if (ctx instanceof Application) return (Application) ctx;
        return (Application) ctx.getApplicationContext();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        appViewModelStore.clear();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backDispatcher.onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
