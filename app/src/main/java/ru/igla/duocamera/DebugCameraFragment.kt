package ru.igla.duocamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.util.Range
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.igla.duocamera.databinding.DebugCameraFragmentBinding
import ru.igla.duocamera.ui.FlashRecordAnimation
import ru.igla.duocamera.ui.toastcompat.Toaster
import ru.igla.duocamera.utils.IntentUtils
import ru.igla.duocamera.utils.OrientationLiveData
import ru.igla.duocamera.utils.ViewUtils
import ru.igla.duocamera.utils.logI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class DebugCameraFragment : Fragment() {

    private val flashRecordAnimation by lazy { FlashRecordAnimation() }

    private val toaster: Toaster by lazy { Toaster(requireContext().applicationContext) }

    private var cameraId = "0"
    private var fps = 30

    // your fragment parameter, a string
    private var cameraName: String? = null

    /**
     * Parse attributes during inflation from a view hierarchy into the
     * arguments we handle.
     */
    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        logI { "OnInflate" }
        if (cameraName == null) {
            context.obtainStyledAttributes(attrs, R.styleable.CameraDebugFragment_Args).apply {
                if (hasValue(R.styleable.CameraDebugFragment_Args_camera_name)) {
                    cameraName = getString(R.styleable.CameraDebugFragment_Args_camera_name)
                }
                recycle()
            }
        }
        cameraName?.apply {
            cameraId = if (cameraName.equals("camera0")) "0" else "1"
        }
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    /**
     * Setup a persistent [Surface] for the recorder so we can use it as an output target for the
     * camera session without preparing the recorder
     */
    private val recorderSurface: Surface by lazy {
        mediaRecorderWrapper.createPersistentSurface()
    }

    private val mediaRecorderWrapper by lazy {
        MediaRecorderWrapper(requireContext().applicationContext)
    }

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    private val whiteDrawable by lazy {
        ColorDrawable(Color.argb(150, 255, 255, 255))
    }

    /** Performs recording animation of flashing screen */
    private val animationTask: Runnable by lazy {
        Runnable {
            // Flash white animation
            fragmentCameraBinding.overlay.foreground = whiteDrawable
            // Wait for ANIMATION_FAST_MILLIS
            fragmentCameraBinding.overlay.postDelayed({
                // Remove white flash animation
                fragmentCameraBinding.overlay.foreground = null
                // Restart animation recursively
                fragmentCameraBinding.overlay.postDelayed(
                    animationTask,
                    DebugCameraActivity.ANIMATION_FAST_MILLIS
                )
            }, DebugCameraActivity.ANIMATION_FAST_MILLIS)
        }
    }

    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** Requests used for preview only in the [CameraCaptureSession] */
    private val previewRequest: CaptureRequest by lazy {
        // Capture request holds references to target surfaces
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            // Add the preview surface target
            addTarget(fragmentCameraBinding.viewFinder.holder.surface)
        }.build()
    }

    /** Requests used for preview and recording in the [CameraCaptureSession] */
    private val recordRequest: CaptureRequest by lazy {
        // Capture request holds references to target surfaces
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview and recording surface targets
            addTarget(fragmentCameraBinding.viewFinder.holder.surface)
            addTarget(recorderSurface)
            // Sets user requested FPS for all targets
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))
        }.build()
    }

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData


    /** Android ViewBinding */
    private var _fragmentCameraBinding: DebugCameraFragmentBinding? = null

    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = DebugCameraFragmentBinding.inflate(
            inflater,
            container, false
        )
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentCameraBinding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                    fragmentCameraBinding.viewFinder.display,
                    characteristics,
                    SurfaceHolder::class.java
                )
                Log.d(
                    TAG,
                    "View finder size: ${fragmentCameraBinding.viewFinder.width} x ${fragmentCameraBinding.viewFinder.height}"
                )
                Log.d(TAG, "Selected preview size: $previewSize")
                fragmentCameraBinding.viewFinder.setAspectRatio(
                    previewSize.width,
                    previewSize.height
                )

                // To ensure that size is set, initialize camera in the view's thread
                fragmentCameraBinding.viewFinder.post { initializeCamera() }
            }
        })

        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, { orientation ->
                Log.d(TAG, "Orientation changed: $orientation")
            })
        }
    }

    private fun requestStartRecord() {
        // Prevents screen rotation during the video recording
        requireActivity().requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LOCKED

        // Start recording repeating requests, which will stop the ongoing preview
        //  repeating requests without having to explicitly call `session.stopRepeating`
        session.setRepeatingRequest(recordRequest, null, cameraHandler)

        // Finalizes recorder setup and starts recording
        mediaRecorderWrapper.recorder = mediaRecorderWrapper.createRecorder(recorderSurface).apply {
            // Sets output orientation based on current sensor value at start time
            relativeOrientation.value?.let { setOrientationHint(it) }

            mediaRecorderWrapper.startRecording(this)
        }

        ViewUtils.runOnUiThread {
            flashRecordAnimation.startAnim(fragmentCameraBinding.captureButton)
        }
    }

    private suspend fun requestStopRecording(view: View) {
        mediaRecorderWrapper.stopRecording()

        // Broadcasts the media file to the rest of the system
        logI { "Scan video file ${mediaRecorderWrapper.outputFile.absolutePath}" }
        MediaScannerConnection.scanFile(
            view.context,
            arrayOf(mediaRecorderWrapper.outputFile.absolutePath),
            null,
            null
        )

        ViewUtils.runOnUiThread {
            flashRecordAnimation.stopAnim()
            toaster.showToast("Video file ${mediaRecorderWrapper.outputFile.absolutePath}")

            // Launch external activity via intent to play video recorded using our provider
            IntentUtils.startActivitySafely(
                view.context,
                mediaRecorderWrapper.resolveIntentFile()
            )
        }

        // Unlocks screen rotation after recording finished
        requireActivity().requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Finishes our current camera screen
        delay(DebugCameraActivity.ANIMATION_SLOW_MILLIS)
    }

//    private val mOnPreviewAvailableListener =
//        OnImageAvailableListener { reader ->
//            logI { "Next frame" }
//        }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating request
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {

        // Open the selected camera
        camera = openCamera(cameraManager, cameraId, cameraHandler)

//        val mImageReaderPreview = ImageReader.newInstance(
//            640,
//            480,
//            ImageFormat.YUV_420_888,
//            1
//        )
//        mImageReaderPreview.setOnImageAvailableListener(
//            mOnPreviewAvailableListener,
//            null
//        )


        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(fragmentCameraBinding.viewFinder.holder.surface, recorderSurface)

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets, cameraHandler)

        // Sends the capture request as frequently as possible until the session is torn down or
        //  session.stopRepeating() is called
        session.setRepeatingRequest(previewRequest, null, cameraHandler)

        fragmentCameraBinding.captureButton.setOnClickListener { view ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (recordingStatus == STATE_IDLE) {
                    recordingStatus = STATE_STARTING
                    requestStartRecord()
                    recordingStatus = STATE_RECORDING
                } else if (recordingStatus == STATE_RECORDING) {
                    recordingStatus = STATE_STOPPING
                    requestStopRecording(view)
                    recordingStatus = STATE_IDLE
                }
            }
        }
    }

    private var recordingStatus = STATE_IDLE

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                requireActivity().finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Creates a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine)
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Creates a capture session using the predefined targets, and defines a session state
        // callback which resumes the coroutine once the session is configured
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        mediaRecorderWrapper.destroyRecording()
        recorderSurface.release()
    }

    override fun onDestroyView() {
        ViewUtils.cancelCallbacks()
        _fragmentCameraBinding = null
        super.onDestroyView()
    }

    companion object {
        private val TAG = DebugCameraFragment::class.java.simpleName

        private const val STATE_IDLE = 0
        private const val STATE_RECORDING = 1
        private const val STATE_STARTING = 1
        private const val STATE_STOPPING = 2
    }
}