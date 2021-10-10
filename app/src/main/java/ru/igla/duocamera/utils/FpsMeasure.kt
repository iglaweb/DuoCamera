package ru.igla.duocamera.utils

import java.util.concurrent.TimeUnit

/**
 * Created by lashkov on 06.09.21.
 * Copyright (c) 2021 igla LLC. All rights reserved.
 */
class FpsMeasure(
    private val fpsPeriodInterpolateMs: Long = 1_000L
) {

    private var lastFps = 0.0

    private var frameCounter = 0
    private var startMeasureFpsNs = -1L

    fun calcFps(): Double {
        if (startMeasureFpsNs == -1L) {
            startMeasureFpsNs = DateUtils.getDateNowNano()
        }

        val timeNowNs = DateUtils.getDateNowNano()
        val timeElapsedMs = TimeUnit.NANOSECONDS.toMillis(timeNowNs - startMeasureFpsNs)

        frameCounter++
        // Compute the FPS of the entire pipeline
        return if (timeElapsedMs >= fpsPeriodInterpolateMs) {
            //calc fps
            lastFps = 1_000L * (frameCounter - 1).toDouble() / timeElapsedMs
            //reset in order to recalculate fps continously
            reset()
            lastFps
        } else {
            //wait for a stable fps
            lastFps
        }
    }

    fun reset() {
        logI { "Recalculate fps" }
        frameCounter = 0
        startMeasureFpsNs = -1L
    }
}