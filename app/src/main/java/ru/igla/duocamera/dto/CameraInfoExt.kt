package ru.igla.duocamera.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraInfoExt(
    val cameraRequestId: String,
    val cameraInfo: CameraInfo,
) : Parcelable