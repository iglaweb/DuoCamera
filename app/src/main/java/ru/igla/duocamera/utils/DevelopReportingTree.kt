package ru.igla.duocamera.utils

import android.util.Log
import timber.log.Timber

/**
 * A tree which logs important information for crash reporting.
 */
class DevelopReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.ERROR -> {
                ru.igla.duocamera.utils.Log.e(t)
            }
            Log.WARN -> {
                ru.igla.duocamera.utils.Log.w(message)
            }
            Log.DEBUG -> {
                ru.igla.duocamera.utils.Log.d(message)
            }
            else -> {
                ru.igla.duocamera.utils.Log.i(message)
            }
        }
    }
}