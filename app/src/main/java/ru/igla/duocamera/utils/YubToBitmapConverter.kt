package ru.igla.duocamera.utils

import android.graphics.Bitmap
import android.media.Image
import android.util.Size

class YubToBitmapConverter {

    private var rgbFrameBitmap: Bitmap? = null
    private val yuvBytes by lazy { arrayOfNulls<ByteArray>(3) }
    private var rgbBytes: IntArray? = null

    private fun getRgbArray(previewSize: Size): IntArray {
        val totalSize: Int = previewSize.width * previewSize.height
        var rgb = rgbBytes
        return if (rgb == null || rgb.size != totalSize) {
            rgb = IntArray(totalSize)
            rgbBytes = rgb
            rgb
        } else {
            rgb
        }
    }

    private fun getRgbBitmap(previewSize: Size): Bitmap {
        var rgbBitmap = rgbFrameBitmap
        return if (rgbBitmap == null ||
            rgbBitmap.height != previewSize.height ||
            rgbBitmap.width != previewSize.width
        ) {
            rgbBitmap = Bitmap.createBitmap(
                previewSize.width,
                previewSize.height,
                Bitmap.Config.ARGB_8888
            )
            rgbFrameBitmap = rgbBitmap
            rgbBitmap
        } else {
            rgbBitmap
        }
    }


    private fun cloneBitmap(bitmap: Bitmap): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            null,
            false
        )
    }

    fun destroy() {
        rgbFrameBitmap?.apply {
            if (!isRecycled) {
                recycle()
            }
        }
    }

    fun extractBitmap(imgYUV420: Image, previewSize: Size): Bitmap {
        rgbBytes = getRgbArray(previewSize)
        rgbFrameBitmap = getRgbBitmap(previewSize)

        val planes: Array<Image.Plane> = imgYUV420.planes
        TensorFlowImageUtils.fillBytes(planes, yuvBytes)
        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        TensorFlowImageUtils.convertYUV420ToARGB8888(
            yuvBytes[0],
            yuvBytes[1],
            yuvBytes[2],
            previewSize.width,
            previewSize.height,
            yRowStride,
            uvRowStride,
            uvPixelStride,
            rgbBytes
        )

        rgbFrameBitmap?.setPixels(
            rgbBytes,
            0,
            previewSize.width,
            0, 0,
            previewSize.width,
            previewSize.height
        )
        return cloneBitmap(rgbFrameBitmap!!)
    }
}