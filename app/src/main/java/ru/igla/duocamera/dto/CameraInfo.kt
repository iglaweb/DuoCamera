package ru.igla.duocamera.dto

import android.os.Parcelable
import android.util.Size
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraInfo(
    val cameraReqType: CameraReqType,
    val name: String,
    val cameraId: String,
    val size: Size,
    val fps: Int
) : Parcelable