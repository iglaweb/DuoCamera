package ru.igla.duocamera.utils

import timber.log.Timber


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

inline fun <T> List<T>.countNoIterator(predicate: (T) -> Boolean): Int {
    if (isEmpty()) return 0
    var count = 0
    forEachNoIterator { element ->
        if (predicate(element)) count++
    }
    return count
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