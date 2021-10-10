package ru.igla.duocamera.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator


class FlashRecordAnimation {

    private val durationAnim = 1_000L

    private lateinit var alphaAnimation: ObjectAnimator

    fun startAnim(view: View) {
        if (::alphaAnimation.isInitialized) {
            if (alphaAnimation.isRunning) return
            alphaAnimation.removeAllListeners()
        }
        view.clearAnimation()
        view.visibility = View.VISIBLE
        alphaAnimation = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f).apply {
            duration = durationAnim
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        alphaAnimation.start()
    }

    fun stopAnim() {
        if (::alphaAnimation.isInitialized) {
            alphaAnimation.cancel()
            alphaAnimation.removeAllListeners()
        }
    }
}