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

inline fun <E> List<E>.forEachNoIteratorIdx(block: (Int, E) -> Unit) {
    var index = 0
    val size = size
    while (index < size) {
        block(index, get(index))
        index++
    }
}

inline fun <E> List<E>.forEachNoIteratorFromEnd(block: (E) -> Unit) {
    var index = size - 1
    while (index >= 0) {
        block(get(index))
        index--
    }
}

inline fun <R, A> ifNotNull(a: A?, block: (A) -> R): R? =
    if (a != null) {
        block(a)
    } else null

inline fun <R, A, B> ifNotNull(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) {
        block(a, b)
    } else null

inline fun <R, A, B, C> ifNotNull(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) {
        block(a, b, c)
    } else null

inline fun <R, A, B, C, D> ifNotNull(a: A?, b: B?, c: C?, d: D?, block: (A, B, C, D) -> R): R? =
    if (a != null && b != null && c != null && d != null) {
        block(a, b, c, d)
    } else null

inline fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

inline fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    p3: T3?,
    block: (T1, T2, T3) -> R?
): R? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}