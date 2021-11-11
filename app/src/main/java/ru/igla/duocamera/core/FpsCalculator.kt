package ru.igla.duocamera.core

import ru.igla.duocamera.utils.FpsMeasure
import ru.igla.duocamera.utils.logI

class FpsCalculator {

    private val fpsMeasure by lazy {
        FpsMeasure(3_000L)
    }

    private var frameNumber = 0

    /**
     * Capture the last FPS to allow debouncing to notify the consumer only when an FPS change is actually detected.
     */
    private var lastFPS = 0

    fun resolveFps(cameraId: String): Int? {
        val currentFps = fpsMeasure.calcFps().toInt()
        logI { "Frame #${++frameNumber}, cameraId $cameraId fps $currentFps" }
        if (currentFps != lastFPS) {
            lastFPS = currentFps
            return currentFps
        }
        return null
    }
}