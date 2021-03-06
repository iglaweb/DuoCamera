package ru.igla.duocamera.ui

import android.os.Handler
import android.os.Looper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

open class BaseFragment(@LayoutRes mContentLayoutId: Int = 0) : Fragment(mContentLayoutId) {

    private val mainThreadHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    private fun isFragmentAlive(): Boolean =
            activity != null && isAdded && !isDetached && view != null && !isRemoving

    fun runOnUiThreadIfFragmentAlive(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper() && isFragmentAlive()) {
            runnable.run()
        } else {
            mainThreadHandler.post {
                if (isFragmentAlive()) {
                    runnable.run()
                }
            }
        }
    }
}