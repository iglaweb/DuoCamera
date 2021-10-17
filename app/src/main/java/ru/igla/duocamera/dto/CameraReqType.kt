package ru.igla.duocamera.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CameraReqType: Parcelable {
    GENERAL_CAMERA_SIZE,
    REQ_MAX_SIZE,
    REQ_MIN_SIZE
}