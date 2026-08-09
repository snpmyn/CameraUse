package com.jiangdg.ausbc.camera.bean

/**
 * camera 1 info
 *
 * @author Created by jiangdg on 2022/1/27
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
data class CameraV1Info(override val cameraId: String) : CameraInfo(cameraId) {
    var cameraType: Int = 0

    override fun toString(): String {
        return "CameraV1Info(cameraId='$cameraId', " +
                "cameraType=$cameraType)"
    }
}