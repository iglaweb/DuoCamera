package ru.igla.duocamera;

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DebugCameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug_activity_camera)
    }

    companion object {
        const val ANIMATION_SLOW_MILLIS = 100L
    }
}