package ru.igla.duocamera.core

abstract class CameraStateListener {
    abstract fun onOpened(camera: CameraDevice)

    abstract fun onDisconnected(camera: CameraDevice)

    abstract fun onError(camera: CameraDevice, error: Int)

    abstract fun onInitCamera(camera: CameraDevice)
}