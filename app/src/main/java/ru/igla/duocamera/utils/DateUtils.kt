package ru.igla.duocamera.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Created by lashkov on 13/02/16.
 * Copyright (c) 2016 igla LLC. All rights reserved.
 */
object DateUtils {

    private const val FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
    private val DETAIL_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm:ss:SSS", Locale.US)

    @JvmStatic
    fun getDateStr(date: Date): String {
        return DETAIL_DATE_FORMAT.format(date)
    }

    @JvmStatic
    fun getTimeFromFloat(value: Float): Long {
        val hours = value.toInt()
        val minutes = ((value % 1) * 100).toInt()
        return TimeUnit.HOURS.toMillis(hours.toLong()) + TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    @JvmStatic
    fun getSimpleReadableDateTime(timestamp: Long): String? {
        return try {
            val sdf: DateFormat = SimpleDateFormat(FULL_DATE_FORMAT, Locale.US)
            val netDate = Date(timestamp)
            sdf.format(netDate)
        } catch (ex: Exception) {
            null
        }
    }

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

    /**
     * Returns a long that describes the number of weeks
     * between timeOne and timeTwo.
     */
    @JvmStatic
    fun getSecondsDiff(date1: Long, date2: Long): Long {
        return TimeUnit.MILLISECONDS.toSeconds(abs(date2 - date1))
    }
}