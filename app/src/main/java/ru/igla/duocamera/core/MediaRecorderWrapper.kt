package ru.igla.duocamera.core

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaRecorder
import android.view.Surface
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import ru.igla.duocamera.BuildConfig
import ru.igla.duocamera.utils.logD
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaRecorderWrapper(
    val context: Context,
    private val fps: Int = 30,
    private val videoWidth: Int = 640,
    private val videoHeight: Int = 480
) {

    private var recordingStartMillis: Long = 0L

    /** Saves the video recording */
    var recorder: MediaRecorder? = null

    /** File where the recording will be saved */
    var outputFile: File = createFile(context, "mp4")

    companion object {

        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000

        /** Creates a [File] named with the current date and time */
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(context.filesDir, "VID_${sdf.format(Date())}.$extension")
        }

        fun resolveIntentFile(context: Context, outputFile: File): Intent {
            return Intent().apply {
                action = Intent.ACTION_VIEW
                type = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(outputFile.extension)
                val authority = "${BuildConfig.APPLICATION_ID}.provider"
                data = FileProvider.getUriForFile(
                    context.applicationContext,
                    authority,
                    outputFile
                )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    /** Creates a [MediaRecorder] instance using the provided [Surface] as input */
    fun createRecorder(surface: Surface) = MediaRecorder().apply {
        //update file for multiple records
        outputFile = createFile(context, "mp4")

        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        if (fps > 0) setVideoFrameRate(fps)
        setVideoSize(videoWidth, videoHeight)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setInputSurface(surface)
    }

    suspend fun stopRecording() {
        // Requires recording of at least MIN_REQUIRED_RECORDING_TIME_MILLIS
        val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
        if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
            delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
        }
        forceStopRecording()
    }

    fun forceStopRecording() {
        logD { "Recording stopped. Output file: $outputFile" }
        try {
            recorder?.stop()
        } catch (exc: Throwable) {
            Timber.e(exc)
        }
    }

    fun destroyRecording() {
        recorder?.release()
    }

    fun createPersistentSurface(): Surface {
        // Get a persistent Surface from MediaCodec, don't forget to release when done
        val surface = MediaCodec.createPersistentInputSurface()

        // Prepare and release a dummy MediaRecorder with our new surface
        // Required to allocate an appropriately sized buffer before passing the Surface as the
        //  output target to the capture session
        createRecorder(surface).apply {
            prepare()
            release()
        }
        return surface
    }

    fun startRecording(mediaRecorder: MediaRecorder) {
        mediaRecorder.apply {
            prepare()
            start()
        }
        recordingStartMillis = System.currentTimeMillis()
        logD { "Recording started" }
    }
}