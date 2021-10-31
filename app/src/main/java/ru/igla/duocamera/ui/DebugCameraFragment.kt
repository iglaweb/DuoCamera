package ru.igla.duocamera.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import ru.igla.duocamera.R
import ru.igla.duocamera.databinding.DebugCameraFragmentBinding
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.dto.CameraInfoExt
import ru.igla.duocamera.dto.CameraReqType
import ru.igla.duocamera.ui.toastcompat.Toaster
import ru.igla.duocamera.utils.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class DebugCameraFragment : BaseFragment() {

    private val flashRecordAnimation by lazy { FlashRecordAnimation() }

    private val toaster: Toaster by lazy { Toaster(requireContext().applicationContext) }

    private lateinit var cameraInfoExt: CameraInfoExt

    private val fpsMeasure by lazy {
        FpsMeasure(3_000L)
    }

    private var frameNumber = 0

    /**
     * Capture the last FPS to allow debouncing to notify the consumer only when an FPS change is actually detected.
     */
    private var lastFPS = 0

    private var recordingStatus = STATE_IDLE

    /**
     * An [ImageReader] that handles live preview.
     */
    private var imageReaderPreview: ImageReader? = null


    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraInfoExt.cameraRequestId)
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

    private val recordBgThread by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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
            imageReaderPreview?.let {
                addTarget(it.surface)
            }
        }.build()
    }

    /** Requests used for preview and recording in the [CameraCaptureSession] */
    private val recordRequest: CaptureRequest by lazy {
        // Capture request holds references to target surfaces
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview and recording surface targets
            addTarget(fragmentCameraBinding.viewFinder.holder.surface)
            addTarget(recorderSurface)
            imageReaderPreview?.apply {
                addTarget(surface)
            }
            // Sets user requested FPS for all targets
            set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(cameraInfoExt.cameraInfo.fps, cameraInfoExt.cameraInfo.fps)
            )
        }.build()
    }

    private val yubToBitmapConverter by lazy { YubToBitmapConverter() }

    /**
     * The [android.util.Size] of camera preview.
     */
    lateinit var previewSize: Size

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
        return fragmentCameraBinding.root
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
                val previewSize = fetchPreviewSize()
                this@DebugCameraFragment.previewSize = previewSize

                logD {
                    "View finder size: ${fragmentCameraBinding.viewFinder.width} x ${fragmentCameraBinding.viewFinder.height}"
                }
                logD { "Selected preview size: $previewSize" }
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
                logI { "Orientation changed: $orientation" }
            })
        }
    }

    private fun fetchPreviewSize(): Size {
        when (cameraInfoExt.cameraInfo.cameraReqType) {
            CameraReqType.GENERAL_CAMERA_SIZE -> {
                return cameraInfoExt.cameraInfo.size
            }
            CameraReqType.REQ_MIN_SIZE -> {
                return getMinPreviewOutputSize(
                    characteristics,
                    SurfaceHolder::class.java
                )
            }
            else -> {
                return getMaxPreviewOutputSize(
                    fragmentCameraBinding.viewFinder.display,
                    characteristics,
                    SurfaceHolder::class.java
                )
            }
        }
    }

    private suspend fun requestStartRecord() {
        // Start recording repeating requests, which will stop the ongoing preview
        //  repeating requests without having to explicitly call `session.stopRepeating`
        session.setRepeatingRequest(recordRequest, null, cameraHandler)

        // Finalizes recorder setup and starts recording
        mediaRecorderWrapper.recorder = mediaRecorderWrapper.createRecorder(recorderSurface).apply {
            // Sets output orientation based on current sensor value at start time
            relativeOrientation.value?.let { setOrientationHint(it) }

            mediaRecorderWrapper.startRecording(this)
        }

        onStartRecording()
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

        onStopRecording(view)

        // Finishes our current camera screen
        delay(DebugCameraActivity.ANIMATION_SLOW_MILLIS)
    }

    private suspend fun onStopRecording(view: View) {
        withContext(Dispatchers.Main) {
            fragmentCameraBinding.cMeter.stop()
            flashRecordAnimation.stopAnim()
            fragmentCameraBinding.captureButton.apply {
                clearAnimation()
                //restore
                visibility = View.VISIBLE
                alpha = 1.0f
            }

            toaster.showToast("Video file ${mediaRecorderWrapper.outputFile.absolutePath}")

            // Launch external activity via intent to play video recorded using our provider
            if (CHOOSE_FILE_SAVE_DST) {
                IntentUtils.startActivitySafely(
                    view.context,
                    mediaRecorderWrapper.resolveIntentFile()
                )
            }
        }
    }

    private val previewAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener

            val bitmap = yubToBitmapConverter.extractBitmap(image, previewSize)
            onPreviewImage(bitmap)
            image.close()

            val currentFps = fpsMeasure.calcFps().toInt()
            logI { "Frame #${++frameNumber}, cameraId ${cameraInfoExt.cameraRequestId} fps $currentFps" }
            if (currentFps != lastFPS) {
                onChangeFps(currentFps)
                lastFPS = currentFps
            }
        }

    private fun onChangeFps(fps: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            fragmentCameraBinding.textviewCamDescription.text =
                getString(R.string.camera_description).format(
                    Locale.US, cameraInfoExt.cameraRequestId, previewSize.toString(), fps
                )
        }
    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating request
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        // Open the selected camera
        camera = openCamera(cameraManager, cameraInfoExt.cameraRequestId, cameraHandler)

        val imageReader = ImageReader.newInstance(
            imageRetrieveSize.width,
            imageRetrieveSize.height,
            ImageFormat.YUV_420_888,
            1
        ).apply {
            setOnImageAvailableListener(
                previewAvailableListener,
                null
            )
        }.also {
            imageReaderPreview = it
        }

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(
            fragmentCameraBinding.viewFinder.holder.surface,
            imageReader.surface,
            recorderSurface
        )

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets, cameraHandler)

        // Sends the capture request as frequently as possible until the session is torn down or
        //  session.stopRepeating() is called
        session.setRepeatingRequest(previewRequest, null, cameraHandler)

        fragmentCameraBinding.captureButton.setOnClickListener { view ->
            onClickRecordBtn(view)
        }
    }

    private fun onClickRecordBtn(view: View) {
        lifecycleScope.launch(recordBgThread) {
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
                logI { "Camera $cameraId has been disconnected" }
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
        if (recordingStatus == STATE_RECORDING) {
            mediaRecorderWrapper.forceStopRecording()
            recordingStatus = STATE_IDLE
        }
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageReaderPreview?.close()
        cameraThread.quitSafely()
        mediaRecorderWrapper.destroyRecording()
        recorderSurface.release()
        yubToBitmapConverter.destroy()
    }

    override fun onDestroyView() {
        ViewUtils.cancelCallbacks()
        fragmentCameraBinding.cMeter.onChronometerTickListener = null
        _fragmentCameraBinding = null
        super.onDestroyView()
    }

    companion object {
        private val TAG = DebugCameraFragment::class.java.simpleName
        const val CAMERA_INFO_EXT = "camera_obj_extra"

        private const val CHOOSE_FILE_SAVE_DST = false

        private const val STATE_IDLE = 0
        private const val STATE_RECORDING = 1
        private const val STATE_STARTING = 1
        private const val STATE_STOPPING = 2

        private val imageRetrieveSize = Size(640, 480)
    }
}