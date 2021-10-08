/*
# Changes from Qualcomm Innovation Center, Inc. are provided under the following license:

# Copyright (c) 2020-2021 Qualcomm Innovation Center, Inc.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted (subject to the limitations in the
# disclaimer below) provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright
#      notice, this list of conditions and the following disclaimer.
#
#    * Redistributions in binary form must reproduce the above
#      copyright notice, this list of conditions and the following
#      disclaimer in the documentation and/or other materials provided
#      with the distribution.
#
#    * Neither the name Qualcomm Innovation Center nor the names of its
#      contributors may be used to endorse or promote products derived
#      from this software without specific prior written permission.
#
# NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
# GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
# HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
# IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
# OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
# IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.igla.duocamera.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LiveData


/**
 * Calculates closest 90-degree orientation to compensate for the device
 * rotation relative to sensor orientation, i.e., allows user to see camera
 * frames with the expected orientation.
 */
class OrientationLiveData(
        context: Context,
        characteristics: CameraCharacteristics
): LiveData<Int>() {

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = when {
                orientation <= 45 -> Surface.ROTATION_0
                orientation <= 135 -> Surface.ROTATION_90
                orientation <= 225 -> Surface.ROTATION_180
                orientation <= 315 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }
            val relative = computeRelativeRotation(characteristics, rotation)
            if (relative != value) postValue(relative)
        }
    }

    override fun onActive() {
        super.onActive()
        listener.enable()
    }

    override fun onInactive() {
        super.onInactive()
        listener.disable()
    }

    companion object {

        /**
         * Computes rotation required to transform from the camera sensor orientation to the
         * device's current orientation in degrees.
         *
         * @param characteristics the [CameraCharacteristics] to query for the sensor orientation.
         * @param surfaceRotation the current device orientation as a Surface constant
         * @return the relative rotation from the camera sensor to the current device orientation.
         */
        @JvmStatic
        private fun computeRelativeRotation(
                characteristics: CameraCharacteristics,
                surfaceRotation: Int
        ): Int {
            val sensorOrientationDegrees =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            val deviceOrientationDegrees = when (surfaceRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            // Reverse device orientation for front-facing cameras
            val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) 1 else -1

            // Calculate desired JPEG orientation relative to camera orientation to make
            // the image upright relative to the device orientation
            return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360
        }

        /**
         * Constants for {@link TAG_ORIENTATION}. They can be interpreted as
         * follows:
         * TOP_LEFT is the normal orientation.
         * TOP_RIGHT is a left-right mirror.
         * BOTTOM_LEFT is a 180 degree rotation.
         * BOTTOM_RIGHT is a top-bottom mirror.
         * LEFT_TOP is mirrored about the top-left<->bottom-right axis.
         * RIGHT_TOP is a 90 degree clockwise rotation.
         * LEFT_BOTTOM is mirrored about the top-right<->bottom-left axis.
         * RIGHT_BOTTOM is a 270 degree clockwise rotation.
         */
        private const val TOP_LEFT = 1
        private const val TOP_RIGHT = 2
        private const val BOTTOM_LEFT = 3
        private const val BOTTOM_RIGHT = 4
        private const val LEFT_TOP = 5
        private const val RIGHT_TOP = 6
        private const val LEFT_BOTTOM = 7
        private const val RIGHT_BOTTOM = 8

        /**
         * Returns the Orientation ExifTag value for a given number of degrees.
         *
         * @param degrees the amount an image is rotated in degrees.
         */
        fun getOrientationValueForRotation(degrees: Int): Int {
            var processedDegrees = degrees % 360;
            if (processedDegrees < 0) {
                processedDegrees += 360
            }
            return when {
                processedDegrees < 90 -> {
                    TOP_LEFT; // 0 degrees
                }
                degrees < 180 -> {
                    RIGHT_TOP; // 90 degrees cw
                }
                degrees < 270 -> {
                    BOTTOM_LEFT; // 180 degrees
                }
                else -> {
                    RIGHT_BOTTOM; // 270 degrees cw
                }
            }
        }
    }
}
