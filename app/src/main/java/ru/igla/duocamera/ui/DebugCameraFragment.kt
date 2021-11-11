package ru.igla.duocamera.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.hardware.camera2.*
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import android.view.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import ru.igla.duocamera.R
import ru.igla.duocamera.core.CameraProvider
import ru.igla.duocamera.core.CameraStateListener
import ru.igla.duocamera.core.FpsCalculator
import ru.igla.duocamera.core.MediaRecorderWrapper
import ru.igla.duocamera.databinding.DebugCameraFragmentBinding
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.dto.CameraInfoExt
import ru.igla.duocamera.dto.CameraReqType
import ru.igla.duocamera.ui.toastcompat.Toaster
import ru.igla.duocamera.utils.*
import java.io.File
import java.util.*


class DebugCameraFragment : BaseFragment() {

    private lateinit var cameraProvider: CameraProvider

    private val flashRecordAnimation by lazy { FlashRecordAnimation() }

    private val fpsCalculator by lazy {
        FpsCalculator()
    }

    private val toaster: Toaster by lazy { Toaster(requireContext().applicationContext) }

    private lateinit var cameraInfoExt: CameraInfoExt

    /** Android ViewBinding */
    private var _fragmentCameraBinding: DebugCameraFragmentBinding? = null

    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private val cameraStateListener = object : CameraStateListener() {
        override fun onOpened(camera: CameraDevice) {
            logI { "Camera opened" }
        }

        override fun onDisconnected(camera: CameraDevice) {
            logI { "Camera disconnected id = " + camera.id }
            requireActivity().finish()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            logI { "Camera error $error" }
        }

        override fun onInitCamera(camera: CameraDevice) {
            logI { "Camera initialized" }
            //set click listener after camera opened
            fragmentCameraBinding.captureButton.setOnClickListener { view ->
                cameraProvider.toggleRecord(view)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.apply {
            cameraInfoExt = getParcelable(CAMERA_INFO_EXT)
                ?: CameraInfoExt(
                    "0",
                    CameraInfo(
                        CameraReqType.REQ_MAX_SIZE,
                        "30 fps, 640x480",
                        "0",
                        Size(640, 480),
                        30
                    )
                )
        }
        _fragmentCameraBinding = DebugCameraFragmentBinding.inflate(
            inflater,
            container,
            false
        )

        cameraProvider = CameraProvider(
            requireContext().applicationContext,
            cameraStateListener,
            lifecycleScope,
            fragmentCameraBinding.viewFinder,
            cameraInfoExt,
            object : CameraProvider.RecordingListener {
                override suspend fun onStartRecording() {
                    this@DebugCameraFragment.onStartRecording()
                }

                override suspend fun onStopRecording(outputFile: File) {
                    this@DebugCameraFragment.onStopRecording(outputFile)
                }
            },
            object : CameraProvider.ReadBitmapListener {
                override fun onReadBitmap(bitmap: Bitmap) {
                    this@DebugCameraFragment.onPreviewImage(bitmap)

                    val fps = fpsCalculator.resolveFps(cameraInfoExt.cameraRequestId)
                    fps?.apply {
                        this@DebugCameraFragment.onChangeFps(this)
                    }
                }
            }
        )

        return fragmentCameraBinding.root
    }

    private suspend fun onStopRecording(outputFile: File) {
        withContext(Dispatchers.Main) {
            fragmentCameraBinding.cMeter.stop()
            flashRecordAnimation.stopAnim()
            fragmentCameraBinding.captureButton.apply {
                clearAnimation()
                //restore
                visibility = View.VISIBLE
                alpha = 1.0f
            }

            toaster.showToast("Video file ${outputFile.absolutePath}")

            // Launch external activity via intent to play video recorded using our provider
            if (CameraProvider.CHOOSE_FILE_SAVE_DST) {
                IntentUtils.startActivitySafely(
                    requireContext().applicationContext,
                    MediaRecorderWrapper.resolveIntentFile(requireContext(), outputFile)
                )
            }
        }
    }

    private suspend fun onStartRecording() {
        withContext(Dispatchers.Main) {
            fragmentCameraBinding.cMeter.apply {
                base = SystemClock.elapsedRealtime()
                start()
                setOnChronometerTickListener {
                    append(" (record)")
                }
            }
            flashRecordAnimation.startAnim(fragmentCameraBinding.captureButton)
        }
    }

    private val setImageBitmap = { targetBitmap: Bitmap? ->
        fragmentCameraBinding.staticCameraImagePreview.clearAndSetBitmapNoRefresh(targetBitmap)
    }

    private fun onPreviewImage(bitmap: Bitmap?) {
        bitmap?.let { it ->
            ViewUtils.runOnUiThread {
                setImageBitmap(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraProvider.initCameraLayout()
        cameraProvider.relativeOrientation.apply {
            observe(viewLifecycleOwner, { orientation ->
                logI { "Orientation changed: $orientation" }
            })
        }
    }

    private fun onChangeFps(fps: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            fragmentCameraBinding.textviewCamDescription.text =
                getString(R.string.camera_description).format(
                    Locale.US,
                    cameraInfoExt.cameraRequestId,
                    cameraProvider.previewSize.toString(),
                    fps
                )
        }
    }

    override fun onStop() {
        cameraProvider.stop()
        super.onStop()
    }

    override fun onDestroy() {
        cameraProvider.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        ViewUtils.cancelCallbacks()
        fragmentCameraBinding.cMeter.onChronometerTickListener = null
        _fragmentCameraBinding = null
        super.onDestroyView()
    }

    companion object {
        const val CAMERA_INFO_EXT = "camera_obj_extra"
    }
}