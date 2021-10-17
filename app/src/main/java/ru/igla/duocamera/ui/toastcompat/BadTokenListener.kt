package ru.igla.duocamera.ui.toastcompat

import android.widget.Toast

/**
 * @author drakeet
 */
interface BadTokenListener {
    fun onBadTokenCaught(toast: Toast)
}