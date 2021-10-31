package ru.igla.duocamera.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.annotation.UiThread
import timber.log.Timber


@UiThread
fun ImageView.clearAndSetBitmapNoRefresh(bitmap: Bitmap?) {
    drawable?.let {
        val screenBitmap = (it as BitmapDrawable).bitmap
        screenBitmap?.recycle()
    }
    setImageBitmap(bitmap)
}

inline fun <T> measureElapsedTimeMs(tag: String, block: () -> T): T {
    val start = DateUtils.getDateNowNano()
    val ret = block()
    logI {
        val diff = DateUtils.calcElapsedMsFromNano(start)
        "TIME [$tag] = $diff ms"
    }
    return ret
}

inline fun <T> measureElapsedTimeMs(block: () -> T): TimeObj<T> {
    val start = DateUtils.getDateNowNano()
    val ret = block()
    val diff = DateUtils.calcElapsedMsFromNano(start)
    return TimeObj(ret, diff)
}

inline fun logI(str: () -> String) {
    if (Constants.DEBUG) {
        Timber.i(str())
    }
}

inline fun logD(str: () -> String) {
    if (Constants.DEBUG) {
        Timber.d(str())
    }
}

/***
 * https://discuss.kotlinlang.org/t/performant-and-elegant-iterator-over-custom-collection/2962/6
 */
inline fun <E> List<E>.forEachNoIterator(block: (E) -> Unit) {
    var index = 0
    val size = size
    while (index < size) {
        block(get(index))
        index++
    }
}