package ru.igla.duocamera.ui;

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import ru.igla.duocamera.R
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.dto.CameraInfoExt
import ru.igla.duocamera.utils.logI


class DebugCameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.debug_activity_camera)
        actionBar?.hide()
        supportActionBar?.hide()

        val cameraInfo: CameraInfo = requireNotNull(intent.getParcelableExtra(CAMERA_INFO_OBJ))
        logI { "Camera info: $cameraInfo" }

        if (savedInstanceState == null) {
            supportFragmentManager.apply {
                beginTransaction()
                    .replace(R.id.fragment_container1, newInstance(CAMERA_FIRST_ID, cameraInfo))
                    .commit()

                beginTransaction()
                    .replace(R.id.fragment_container2, newInstance(CAMERA_SECOND_ID, cameraInfo))
                    .commit()
            }
        }
    }

    private fun newInstance(requestId: String, cameraInfo: CameraInfo): DebugCameraFragment {
        return DebugCameraFragment().apply {
            val arguments = Bundle().apply {
                putParcelable(
                    DebugCameraFragment.CAMERA_INFO_EXT,
                    CameraInfoExt(requestId, cameraInfo)
                )
            }
            setArguments(arguments)
        }
    }

    companion object {
        const val ANIMATION_SLOW_MILLIS = 100L
        const val CAMERA_INFO_OBJ = "data"
        private const val CAMERA_FIRST_ID = "0"
        private const val CAMERA_SECOND_ID = "1"
    }
}