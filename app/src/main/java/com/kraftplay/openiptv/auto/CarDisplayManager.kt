package com.kraftplay.openiptv.auto

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display

class CarDisplayManager(private val context: Context) {

    companion object {
        private const val TAG = "CarDisplayManager"
    }

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var presentation: CarVideoPresentation? = null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.d(TAG, "Display added: $displayId")
            displayManager.getDisplay(displayId)?.let { checkAndShow(it) }
        }

        override fun onDisplayChanged(displayId: Int) {
            displayManager.getDisplay(displayId)?.let { display ->
                if (display.state == Display.STATE_ON) {
                    checkAndShow(display)
                }
            }
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.d(TAG, "Display removed: $displayId")
            if (presentation?.display?.displayId == displayId) {
                presentation?.dismiss()
                presentation = null
            }
        }
    }

    fun start() {
        displayManager.registerDisplayListener(listener, handler)
        val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val allDisplays = displayManager.displays
        val targetDisplay = presentationDisplays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
            ?: allDisplays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

        targetDisplay?.let { display ->
            Log.d(TAG, "Using display: ${display.displayId} (${display.name})")
            checkAndShow(display)
        } ?: Log.w(TAG, "No external presentation display found")
    }

    fun stop() {
        displayManager.unregisterDisplayListener(listener)
        presentation?.dismiss()
        presentation = null
    }

    private fun checkAndShow(display: Display) {
        if (display.displayId == Display.DEFAULT_DISPLAY) {
            return
        }
        try {
            if (presentation?.display?.displayId == display.displayId) {
                Log.d(TAG, "Already showing on display ${display.displayId}")
                return
            }

            presentation?.dismiss()
            presentation = CarVideoPresentation(context, display).apply {
                show()
                Log.d(TAG, "Presentation shown on display ${display.displayId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show presentation", e)
            presentation = null
        }
    }
}
