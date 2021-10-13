package ru.igla.duocamera;

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.dto.CameraInfoExt
import ru.igla.duocamera.utils.logI


class DebugCameraActivity : AppCompatActivity() {

    private lateinit var cameraInfo: CameraInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.debug_activity_camera)
        actionBar?.hide()
        supportActionBar?.hide()

        cameraInfo = requireNotNull(intent.getParcelableExtra(CAMERA_INFO_OBJ))
        logI { "Camera info: $cameraInfo" }

        if (savedInstanceState == null) {
            supportFragmentManager.apply {
                beginTransaction()
                    .replace(R.id.fragment_container1, newInstance("0", cameraInfo))
                    .commit()

                beginTransaction()
                    .replace(R.id.fragment_container2, newInstance("1", cameraInfo))
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
    }
}