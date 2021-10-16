package ru.igla.duocamera.utils

import ru.igla.duocamera.BuildConfig
import ru.igla.duocamera.utils.StringUtils.isNullOrEmpty

object Log {
    private const val TAG = "DuoCamera"
    private val ENABLED = BuildConfig.DEBUG
    private val LOCATION_ENABLED = BuildConfig.DEBUG

    fun tag(tag: String): String {
        return "$TAG-$tag"
    }

    fun tag(): String {
        return TAG
    }

    fun vt(tag: String?, format: String, vararg args: Any?) {
        if (ENABLED) {
            try {
                android.util.Log.v(tag, String.format(format, *args) + getLocation())
            } catch (ignored: Exception) {
            }
        }
    }

    fun dt(tag: String?, format: String, vararg args: Any?) {
        if (ENABLED) {
            try {
                android.util.Log.d(tag, String.format(format, *args) + getLocation())
            } catch (ignored: Exception) {
            }
        }
    }

    fun it(tag: String?, format: String, vararg args: Any?) {
        if (ENABLED) {
            try {
                android.util.Log.i(tag, String.format(format, *args) + getLocation())
            } catch (ignored: Exception) {
            }
        }
    }

    fun wt(tag: String?, format: String, vararg args: Any?) {
        if (ENABLED) {
            try {
                android.util.Log.w(tag, String.format(format, *args) + getLocation())
            } catch (ignored: Exception) {
            }
        }
    }

    fun wt(tag: String?, message: String, e: Throwable?) {
        if (ENABLED) {
            try {
                android.util.Log.w(tag, message + getLocation(), e)
            } catch (ignored: Exception) {
            }
        }
    }

    fun wt(tag: String?, e: Throwable?) {
        if (ENABLED) {
            try {
                android.util.Log.w(tag, e)
            } catch (ignored: Exception) {
            }
        }
    }

    fun et(tag: String?, e: Throwable?) {
        if (ENABLED) {
            try {
                android.util.Log.e(tag, e?.message + getLocation(), e)
            } catch (ignored: Exception) {
            }
        }
    }

    fun et(tag: String?, format: String, vararg args: Any?) {
        if (ENABLED) {
            try {
                android.util.Log.e(tag, String.format(format, *args) + getLocation())
            } catch (ignored: Exception) {
            }
        }
    }

    fun v(tag: String, format: String, vararg args: Any?) {
        vt(tag(tag), format, *args)
    }

    fun v(format: String, vararg args: Any?) {
        vt(tag(), format, *args)
    }

    fun d(format: String, vararg args: Any?) {
        dt(tag(), format, *args)
    }

    fun d(tag: String, format: String, vararg args: Any?) {
        dt(tag(tag), format, *args)
    }

    fun i(format: String, vararg args: Any?) {
        it(tag(), format, *args)
    }

    fun i(tag: String, format: String, vararg args: Any?) {
        it(tag(tag), format, *args)
    }

    fun w(format: String, vararg args: Any?) {
        wt(TAG, format, *args)
    }

    fun w(message: String, e: Throwable?) {
        wt(tag(), message, e)
    }

    fun w(tag: String, message: String, e: Throwable?) {
        wt(tag(tag), message, e)
    }

    fun w(e: Throwable?) {
        wt(TAG, e)
    }

    fun e(format: String, vararg args: Any?) {
        et(TAG, format, *args)
    }

    fun e(e: Throwable?) {
        et(TAG, e)
    }

    fun vtrace(traceLength: Int, format: String, vararg args: Any?) {
        vtrace(TAG, traceLength, format, *args)
    }

    fun dtrace(traceLength: Int, format: String, vararg args: Any?) {
        dtrace(TAG, traceLength, format, *args)
    }

    fun itrace(traceLength: Int, format: String?, vararg args: Any?) {
        itrace(TAG, traceLength, format, *args)
    }

    fun wtrace(traceLength: Int, format: String?, vararg args: Any?) {
        wtrace(TAG, traceLength, format, *args)
    }

    fun etrace(traceLength: Int, format: String?, vararg args: Any?) {
        etrace(tag(), traceLength, format, *args)
    }

    fun vtrace(tag: String?, traceLength: Int, format: String?, vararg args: Any?) {
        if (ENABLED) android.util.Log.v(tag, String.format(format!!, *args) + getTrace(traceLength))
    }

    fun dtrace(tag: String?, traceLength: Int, format: String?, vararg args: Any?) {
        if (ENABLED) android.util.Log.d(tag, String.format(format!!, *args) + getTrace(traceLength))
    }

    fun itrace(tag: String?, traceLength: Int, format: String?, vararg args: Any?) {
        if (ENABLED) android.util.Log.i(tag, String.format(format!!, *args) + getTrace(traceLength))
    }

    fun wtrace(tag: String?, traceLength: Int, format: String?, vararg args: Any?) {
        if (ENABLED) android.util.Log.w(tag, String.format(format!!, *args) + getTrace(traceLength))
    }

    fun etrace(tag: String?, traceLength: Int, format: String?, vararg args: Any?) {
        if (ENABLED) android.util.Log.e(tag, String.format(format!!, *args) + getTrace(traceLength))
    }

    private fun getTrace(length: Int): String {
        if (!LOCATION_ENABLED) return ""
        val logClassName = Log::class.java.name
        val traces = Thread.currentThread().stackTrace
        var foundIndex = -1
        for (i in traces.indices) {
            val trace = traces[i]
            if (trace.className.startsWith(logClassName)) {
                foundIndex = i
            } else {
                if (foundIndex > 0) break
            }
        }
        val sb = StringBuilder()
        sb.append("\n")
        for (i in foundIndex + 1 until foundIndex + length + 1) {
            if (i > traces.size) break
            val trace = traces[i]
            sb.append(
                String.format(
                    "    at %s.%s:%s\n",
                    trace.className,
                    trace.methodName,
                    trace.lineNumber
                )
            )
        }
        sb.delete(sb.length - 1, sb.length)
        return """
            
            $sb
            """.trimIndent()
    }

    private fun getLocation(): String {
        if (!LOCATION_ENABLED) return ""
        val logClassName = Log::class.java.name
        val traces = Thread.currentThread().stackTrace
        var found = false
        for (i in traces.indices) {
            val trace = traces[i]
            try {
                if (found) {
                    if (!trace.className.startsWith(logClassName)) {
                        val clazz = Class.forName(trace.className)
                        var clazzName = clazz.simpleName
                        if (isNullOrEmpty(clazzName)) clazzName = clazz.name
                        return String.format(
                            " [%s.%s:%d]",
                            clazzName,
                            trace.methodName,
                            trace.lineNumber
                        )
                    }
                } else if (trace.className.startsWith(logClassName)) {
                    found = true
                }
            } catch (e: ClassNotFoundException) {
            }
        }
        return " []"
    }

}