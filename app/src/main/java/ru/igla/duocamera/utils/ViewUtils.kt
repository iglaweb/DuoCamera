package ru.igla.duocamera.utils

import android.os.Handler
import android.os.Looper


/**
 * Created by igor-lashkov on 27/11/2017.
 */

object ViewUtils {

    private val uiHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun runOnUiThread(action: () -> Unit) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            action()
        } else {
            uiHandler.post { action() }
        }
    }

    @JvmStatic
    fun cancelCallbacks() {
        uiHandler.removeCallbacksAndMessages(null)
    }
}