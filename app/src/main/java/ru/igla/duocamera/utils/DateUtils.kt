package ru.igla.duocamera.utils

import java.util.concurrent.TimeUnit

/**
 * Created by lashkov on 10/10/21.
 * Copyright (c) 2021 igla LLC. All rights reserved.
 */
object DateUtils {

    /**
     * Returns the current time in milliseconds.  Note that
     * while the unit of time of the return value is a millisecond,
     * the granularity of the value depends on the underlying
     * operating system and may be larger.  For example, many
     * operating systems measure time in units of tens of
     * milliseconds.
     *
     *
     *  See the description of the class `Date` for
     * a discussion of slight discrepancies that may arise between
     * "computer time" and coordinated universal time (UTC).
     *
     * @return the difference, measured in milliseconds, between
     * the current time and midnight, January 1, 1970 UTC.
     * @see java.util.Date
     */
    @JvmStatic
    fun getCurrentDateInMs(): Long {
        return System.currentTimeMillis()
    }

    @JvmStatic
    fun calcElapsedMsFromNano(startTimeNs: Long): Long {
        val elapsed = getDateNowNano() - startTimeNs
        return TimeUnit.NANOSECONDS.toMillis(elapsed)
    }

    /**
     * Returns the current time in nanoseconds. it is not a wall clock :)
     * can only be used to measure elapsed time
     */
    @JvmStatic
    fun getDateNowNano(): Long {
        return System.nanoTime()
    }
}