package ru.igla.duocamera.utils

import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat



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

    fun runOnUiThread(action: Runnable, delay: Long) {
        uiHandler.postDelayed(action, delay)
    }

    @JvmStatic
    fun stopHandler(backgroundHandler: Handler?) {
        backgroundHandler?.looper?.apply {
            quitSafely()
        }
    }

    @JvmStatic
    fun cancelCallbacks() {
        uiHandler.removeCallbacksAndMessages(null)
    }

    @JvmStatic
    fun cancelCallback(runnable: Runnable) {
        uiHandler.removeCallbacks(runnable)
    }

    @JvmStatic
    fun dismissDialogSafety(dialog: DialogInterface?) {
        dialog?.let {
            runOnUiThread {
                try {
                    it.dismiss()
                } catch (e: Exception) {
                    Log.e(e)
                }
            }
        }
    }

    fun destroyAttachedView(view: View?, windowManager: WindowManager) {
        if (view != null) {
            if (ViewCompat.isAttachedToWindow(view)) {
                windowManager.removeView(view)
            }
        }
    }
}


/**
 * Returns true when this view's visibility is [View.VISIBLE], false otherwise.
 *
 * ```
 * if (view.isVisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.VISIBLE], false to [View.GONE].
 *
 * ```
 * view.isVisible = true
 * ```
 */
inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

/**
 * Returns true when this view's visibility is [View.INVISIBLE], false otherwise.
 *
 * ```
 * if (view.isInvisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.INVISIBLE], false to [View.VISIBLE].
 *
 * ```
 * view.isInvisible = true
 * ```
 */
inline var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }

/**
 * Returns true when this view's visibility is [View.GONE], false otherwise.
 *
 * ```
 * if (view.isGone) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.GONE], false to [View.VISIBLE].
 *
 * ```
 * view.isGone = true
 * ```
 */
inline var View.isGone: Boolean
    get() = visibility == View.GONE
    set(value) {
        visibility = if (value) View.GONE else View.VISIBLE
    }