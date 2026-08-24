package com.jiangdg.ausbc.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.provider.MediaStore
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.MultiCameraClient.Companion.CAPTURE_TIMES_OUT_SEC
import com.jiangdg.ausbc.MultiCameraClient.Companion.MAX_NV21_DATA
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.CameraUtils
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * UVC Camera
 *
 * @author Created by jiangdg on 2023/1/15
 */
class CameraUVC(ctx: Context, device: UsbDevice) : MultiCameraClient.ICamera(ctx, device) {
    private var mUvcCamera: UVCCamera? = null
    private val mCameraPreviewSize by lazy {
        arrayListOf<PreviewSize>()
    }

    private val frameCallBack = IFrameCallback { frame ->
        frame?.apply {
            frame.position(0)
            val data = ByteArray(capacity())
            get(data)
            mCameraRequest?.apply {
                if (data.size != previewWidth * previewHeight * 3 / 2) {
                    return@IFrameCallback
                }
                // for preview callback
                mPreviewDataCbList.forEach { cb ->
                    cb?.onPreviewData(
                        data,
                        previewWidth,
                        previewHeight,
                        IPreviewDataCallBack.DataFormat.NV21
                    )
                }
                // for image
                if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                    mNV21DataQueue.removeLast()
                }
                mNV21DataQueue.offerFirst(data)
                // for video
                // avoid preview size changed
                putVideoData(data)
            }
        }
    }

    /*override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        val previewSizeList = arrayListOf<PreviewSize>()
        if (mUvcCamera?.supportedSizeList?.isNotEmpty() == true) {
            mUvcCamera?.supportedSizeList
        } else {
            mUvcCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
        }?.let { sizeList ->
            if (mCameraPreviewSize.isEmpty()) {
                mCameraPreviewSize.clear()
                sizeList.forEach { size ->
                    val width = size.width
                    val height = size.height
                    mCameraPreviewSize.add(PreviewSize(width, height))
                }
            }
            mCameraPreviewSize
        }?.onEach { size ->
            val width = size.width
            val height = size.height
            val ratio = width.toDouble() / height
            if (aspectRatio == null || aspectRatio == ratio) {
                previewSizeList.add(PreviewSize(width, height))
            }
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "aspect ratio = $aspectRatio, getAllPreviewSizes = $previewSizeList, ")
        }
        return previewSizeList
    }*/

    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        val previewSizeList = arrayListOf<PreviewSize>()
        // 1. 传入 -1
        // 强行让 Native 层返回 MJPEG + YUYV + H264 的所有并集分辨率。
        // 如果 mUvcCamera 没有暴露 getSupportedSize(-1, json)
        // 可以调用下面的兼容提取逻辑
        val rawSizeList = mUvcCamera?.let { camera ->
            val jsonStr = camera.supportedSize
            if (!jsonStr.isNullOrEmpty()) {
                // -1 代表获取全部格式
                camera.getSupportedSize(-1, jsonStr)
            } else {
                camera.supportedSizeList
            }
        }
        if (rawSizeList.isNullOrEmpty()) {
            if (Utils.debugCamera) {
                Logger.e(TAG, "Get camera raw sizes failed: rawSizeList is null or empty.")
            }
            return previewSizeList
        }
        // 2. 刷新本地缓存
        // 并使用 HashSet 根据 (Width x Height) 进行物理尺寸去重
        mCameraPreviewSize.clear()
        // 保持有序去重
        val uniqueSizeSet = LinkedHashSet<PreviewSize>()
        rawSizeList.forEach { size ->
            uniqueSizeSet.add(PreviewSize(size.width, size.height))
        }
        mCameraPreviewSize.addAll(uniqueSizeSet)
        // 3. 按宽高比过滤
        // 引入 0.01 容差防浮点数精度误差
        mCameraPreviewSize.forEach { size ->
            val width = size.width
            val height = size.height
            val ratio = width.toDouble() / height
            if ((aspectRatio == null) || (abs(aspectRatio - ratio) < 0.01)) {
                previewSizeList.add(PreviewSize(width, height))
            }
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "aspect ratio = $aspectRatio, FULL getAllPreviewSizes = $previewSizeList")
        }
        return previewSizeList
    }

    override fun <T> openCameraInternal(cameraView: T) {
        if (Utils.isTargetSdkOverP(ctx) && !CameraUtils.hasCameraPermission(ctx)) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Has no CAMERA permission.")
            Logger.e(
                TAG,
                "open camera failed, need Manifest.permission.CAMERA permission when targetSdk>=28"
            )
            return
        }
        if (mCtrlBlock == null) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Usb control block can not be null ")
            return
        }
        // 1. create a UVCCamera
        val request = mCameraRequest!!
        try {
            mUvcCamera = UVCCamera().apply {
                open(mCtrlBlock)
            }
        } catch (e: Exception) {
            closeCamera()
            postStateEvent(
                ICameraStateCallBack.State.ERROR,
                "open camera failed ${e.localizedMessage}"
            )
            Logger.e(TAG, "open camera failed.", e)
        }

        // 2. set preview size and register preview callback
        var previewSize = getSuitableSize(request.previewWidth, request.previewHeight).apply {
            mCameraRequest!!.previewWidth = width
            mCameraRequest!!.previewHeight = height
        }
        try {
            Logger.i(TAG, "getSuitableSize: $previewSize")
            if (!isPreviewSizeSupported(previewSize)) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "unsupported preview size")
                Logger.e(
                    TAG,
                    "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}"
                )
                return
            }
            initEncodeProcessor(previewSize.width, previewSize.height)
            // if give custom minFps or maxFps or unsupported preview size
            // this method will fail
            mUvcCamera?.setPreviewSize(
                previewSize.width,
                previewSize.height,
                MIN_FS,
                MAX_FPS,
                UVCCamera.FRAME_FORMAT_MJPEG,
                UVCCamera.DEFAULT_BANDWIDTH
            )
        } catch (e: Exception) {
            try {
                previewSize = getSuitableSize(request.previewWidth, request.previewHeight).apply {
                    mCameraRequest!!.previewWidth = width
                    mCameraRequest!!.previewHeight = height
                }
                if (!isPreviewSizeSupported(previewSize)) {
                    postStateEvent(ICameraStateCallBack.State.ERROR, "unsupported preview size")
                    closeCamera()
                    Logger.e(
                        TAG,
                        "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}"
                    )
                    return
                }
                Logger.e(TAG, " setPreviewSize failed, try to use yuv format...")
                mUvcCamera?.setPreviewSize(
                    previewSize.width,
                    previewSize.height,
                    MIN_FS,
                    MAX_FPS,
                    UVCCamera.FRAME_FORMAT_YUYV,
                    UVCCamera.DEFAULT_BANDWIDTH
                )
            } catch (e: Exception) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "err: ${e.localizedMessage}")
                Logger.e(TAG, " setPreviewSize failed, even using yuv format", e)
                return
            }
        }
        // if not opengl render or opengl render with preview callback
        // there should opened
        if (!isNeedGLESRender || mCameraRequest!!.isRawPreviewData || mCameraRequest!!.isCaptureRawImage) {
            mUvcCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
        }
        // 3. start preview
        when (cameraView) {
            is Surface -> {
                mUvcCamera?.setPreviewDisplay(cameraView)
            }
            is SurfaceTexture -> {
                mUvcCamera?.setPreviewTexture(cameraView)
            }
            is SurfaceView -> {
                mUvcCamera?.setPreviewDisplay(cameraView.holder)
            }
            is TextureView -> {
                mUvcCamera?.setPreviewTexture(cameraView.surfaceTexture)
            }
            else -> {
                throw IllegalStateException("Only support Surface or SurfaceTexture or SurfaceView or TextureView or GLSurfaceView--$cameraView")
            }
        }
        mUvcCamera?.autoFocus = true
        mUvcCamera?.autoWhiteBlance = true
        mUvcCamera?.startPreview()
        mUvcCamera?.updateCameraParams()
        isPreviewed = true
        postStateEvent(ICameraStateCallBack.State.OPENED)
        if (Utils.debugCamera) {
            Logger.i(TAG, " start preview, name = ${device.deviceName}, preview=$previewSize")
        }
    }

    override fun closeCameraInternal() {
        postStateEvent(ICameraStateCallBack.State.CLOSED)
        isPreviewed = false
        releaseEncodeProcessor()
        mUvcCamera?.destroy()
        mUvcCamera = null
        if (Utils.debugCamera) {
            Logger.i(TAG, " stop preview, name = ${device.deviceName}")
        }
    }

    override fun captureImageInternal(savePath: String?, callback: ICaptureCallBack) {
        mSaveImageExecutor.submit {
            if (!CameraUtils.hasStoragePermission(ctx)) {
                mMainHandler.post {
                    callback.onError("have no storage permission")
                }
                Logger.e(TAG, "open camera failed, have no storage permission")
                return@submit
            }
            if (!isPreviewed) {
                mMainHandler.post {
                    callback.onError("camera not previewing")
                }
                Logger.i(TAG, "captureImageInternal failed, camera not previewing")
                return@submit
            }
            val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
            if (data == null) {
                mMainHandler.post {
                    callback.onError("Times out")
                }
                Logger.i(TAG, "captureImageInternal failed, times out.")
                return@submit
            }
            mMainHandler.post {
                callback.onBegin()
            }
            val date = mDateFormat.format(System.currentTimeMillis())
            val title = savePath ?: "IMG_AUSBC_$date"
            val displayName = savePath ?: "$title.jpg"
            val path = savePath ?: "$mCameraDir/$displayName"
            val location = Utils.getGpsLocation(ctx)
            val width = mCameraRequest!!.previewWidth
            val height = mCameraRequest!!.previewHeight
            val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
            if (!ret) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                mMainHandler.post {
                    callback.onError("save yuv to jpeg failed.")
                }
                Logger.w(TAG, "save yuv to jpeg failed.")
                return@submit
            }
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, title)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.ImageColumns.DATA, path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
            ctx.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            mMainHandler.post {
                callback.onComplete(path)
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, "captureImageInternal save path = $path")
            }
        }
    }

    /**
     * Is mic supported
     *
     * @return true camera support mic
     */
    fun isMicSupported() = CameraUtils.isCameraContainsMic(this.device)

    /**
     * Send camera command
     *
     * This method cannot be verified, please use it with caution
     */
    fun sendCameraCommand(command: Int) {
        mCameraHandler?.post {
            mUvcCamera?.sendCommand(command)
        }
    }

    /**
     * Set auto focus
     *
     * @param enable true enable auto focus
     */
    fun setAutoFocus(enable: Boolean) {
        mUvcCamera?.autoFocus = enable
    }

    /**
     * Get auto focus
     *
     * @return true enable auto focus
     */
    fun getAutoFocus() = mUvcCamera?.autoFocus

    /**
     * Reset auto focus
     */
    fun resetAutoFocus() {
        mUvcCamera?.resetFocus()
    }

    /**
     * Set auto white balance
     *
     * @param autoWhiteBalance true enable auto white balance
     */
    fun setAutoWhiteBalance(autoWhiteBalance: Boolean) {
        mUvcCamera?.autoWhiteBlance = autoWhiteBalance
    }

    /**
     * Get auto white balance
     *
     * @return true enable auto white balance
     */
    fun getAutoWhiteBalance() = mUvcCamera?.autoWhiteBlance

    /**
     * Set zoom
     *
     * @param zoom zoom value, 0 means reset
     */
    fun setZoom(zoom: Int) {
        mUvcCamera?.zoom = zoom
    }

    /**
     * Get zoom
     */
    fun getZoom() = mUvcCamera?.zoom

    /**
     * Reset zoom
     */
    fun resetZoom() {
        mUvcCamera?.resetZoom()
    }

    /**
     * Set gain
     *
     * @param gain gain value, 0 means reset
     */
    fun setGain(gain: Int) {
        mUvcCamera?.gain = gain
    }

    /**
     * Get gain
     */
    fun getGain() = mUvcCamera?.gain

    /**
     * Reset gain
     */
    fun resetGain() {
        mUvcCamera?.resetGain()
    }

    /**
     * Set gamma
     *
     * @param gamma gamma value, 0 means reset
     */
    fun setGamma(gamma: Int) {
        mUvcCamera?.gamma = gamma
    }

    /**
     * Get gamma
     */
    fun getGamma() = mUvcCamera?.gamma

    /**
     * Reset gamma
     */
    fun resetGamma() {
        mUvcCamera?.resetGamma()
    }

    /**
     * Set brightness
     *
     * @param brightness brightness value, 0 means reset
     */
    fun setBrightness(brightness: Int) {
        mUvcCamera?.brightness = brightness
    }

    /**
     * Get brightness
     */
    fun getBrightness() = mUvcCamera?.brightness

    /**
     * Reset brightnes
     */
    fun resetBrightness() {
        mUvcCamera?.resetBrightness()
    }

    /**
     * Set contrast
     *
     * @param contrast contrast value, 0 means reset
     */
    fun setContrast(contrast: Int) {
        mUvcCamera?.contrast = contrast
    }

    /**
     * Get contrast
     */
    fun getContrast() = mUvcCamera?.contrast

    /**
     * Reset contrast
     */
    fun resetContrast() {
        mUvcCamera?.resetContrast()
    }

    /**
     * Set sharpness
     *
     * @param sharpness sharpness value, 0 means reset
     */
    fun setSharpness(sharpness: Int) {
        mUvcCamera?.sharpness = sharpness
    }

    /**
     * Get sharpness
     */
    fun getSharpness() = mUvcCamera?.sharpness

    /**
     * Reset sharpness
     */
    fun resetSharpness() {
        mUvcCamera?.resetSharpness()
    }

    /**
     * Set saturation
     *
     * @param saturation saturation value, 0 means reset
     */
    fun setSaturation(saturation: Int) {
        mUvcCamera?.saturation = saturation
    }

    /**
     * Get saturation
     */
    fun getSaturation() = mUvcCamera?.saturation

    /**
     * Reset saturation
     */
    fun resetSaturation() {
        mUvcCamera?.resetSaturation()
    }

    /**
     * Set hue
     *
     * @param hue hue value, 0 means reset
     */
    fun setHue(hue: Int) {
        mUvcCamera?.hue = hue
    }

    /**
     * Get hue
     */
    fun getHue() = mUvcCamera?.hue

    /**
     * Reset saturation
     */
    fun resetHue() {
        mUvcCamera?.resetHue()
    }

    /**
     * Update preview size
     *
     * 热切预览分辨率
     *
     * 传输格式
     * 1. FRAME_FORMAT_MJPEG - 压缩流格式
     * - 图像在摄像头硬件内部经 Motion JPEG 压缩后再传入系统，占用 USB 带宽极小。
     * - 支持在大分辨率 (1080P / 4K) 下保持高帧率 (30 ~ 60 FPS)
     * - 作为首选默认切换格式
     * 2. FRAME_FORMAT_YUYV - 未压缩原始数据流格式
     * - 未经任何压缩的裸数据 (2 Bytes / Pixel)，对系统与 USB 总线带宽要求极高。
     * - 受限于 USB 2.0 带宽瓶颈，大分辨率下硬件帧率会被迫降至 5 ~ 15 FPS，甚至引发底层传输丢帧。
     * - 摄像头硬件在目标分辨率下不支持 MJPEG 格式时降级适配
     * <p>
     * 相机关闭原因说明
     * - 若 MJPEG 与 YUYV 两次 setPreviewSize 均抛异常，说明摄像头固件 (UVC Firmware) 根本不支持该目标分辨率或底层 USB 管道 (Pipe) 配流失败。
     * - 此时由于在尝试切换前已调 stopPreview() 停流，若不及时拦截抛出失败，系统将无法继续渲染后续帧，表现为预览画面黑屏 / 挂起 (即相机预览被迫关闭)。
     *
     * @param width 目标宽度
     * @param height 目标高度
     * @param surface 预览画面载体 (Surface / SurfaceTexture / SurfaceView / TextureView)
     * @param onResult 结果异步回调 (isSuccess 是否成功 || formatMode 实际生效模式 MJPEG / YUYV / null)
     */
    fun updatePreviewSize(
        width: Int,
        height: Int,
        surface: Any?,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        mCameraHandler?.post {
            val camera = mUvcCamera
            if (camera == null || !isPreviewed) {
                Logger.e(TAG, "updatePreviewSize failed: camera is null or not previewing.")
                mMainHandler.post { onResult?.invoke(false, null) }
                return@post
            }
            val request = mCameraRequest ?: run {
                Logger.e(TAG, "updatePreviewSize failed: mCameraRequest is null.")
                mMainHandler.post { onResult?.invoke(false, null) }
                return@post
            }
            try {
                camera.setFrameCallback(null, 0)
                camera.stopPreview()
                releaseEncodeProcessor()
                mNV21DataQueue.clear()
                request.previewWidth = width
                request.previewHeight = height
                var isSuccess = false
                var formatMode: String? = null
                val safeMaxFps = if (width >= 3840) 30 else MAX_FPS
                try {
                    camera.setPreviewSize(
                        width,
                        height,
                        MIN_FS,
                        safeMaxFps,
                        UVCCamera.FRAME_FORMAT_MJPEG,
                        UVCCamera.DEFAULT_BANDWIDTH
                    )
                    isSuccess = true
                    formatMode = "MJPEG"
                } catch (e: Exception) {
                    Logger.w(
                        TAG,
                        "Set MJPEG preview size ($width x $height) failed, fallback to YUYV...",
                        e
                    )
                    try {
                        camera.setPreviewSize(
                            width,
                            height,
                            MIN_FS,
                            30,
                            UVCCamera.FRAME_FORMAT_YUYV,
                            UVCCamera.DEFAULT_BANDWIDTH
                        )
                        isSuccess = true
                        formatMode = "YUYV"
                    } catch (ex: Exception) {
                        Logger.e(TAG, "Set YUYV preview size ($width x $height) failed!", ex)
                    }
                }
                if (!isSuccess) {
                    // MJPEG 与 YUYV 均失败
                    // 由于前面已经 stopPreview()，此处已无法继续输出画面，需直接通知 UI 切换失败。
                    mMainHandler.post { onResult?.invoke(false, null) }
                    return@post
                }
                initEncodeProcessor(width, height)
                if (!isNeedGLESRender || request.isRawPreviewData || request.isCaptureRawImage) {
                    camera.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
                }
                when (surface) {
                    is Surface -> camera.setPreviewDisplay(surface)
                    is SurfaceTexture -> camera.setPreviewTexture(surface)
                    is SurfaceView -> camera.setPreviewDisplay(surface.holder)
                    is TextureView -> camera.setPreviewTexture(surface.surfaceTexture)
                    else -> Logger.w(TAG, "Surface is null or unsupported type.")
                }
                camera.startPreview()
                camera.updateCameraParams()
                isPreviewed = true
                Logger.i(TAG, "updatePreviewSize success: ${width}x${height}, format: $formatMode")
                mMainHandler.post { onResult?.invoke(true, formatMode) }
            } catch (e: Exception) {
                Logger.e(TAG, "updatePreviewSize exception: ${e.localizedMessage}", e)
                // 异常捕获
                // 重新标记预览停止，防止状态错乱。
                isPreviewed = false
                mMainHandler.post { onResult?.invoke(false, null) }
            }
        }
    }

    companion object {
        private const val TAG = "CameraUVC"
        private const val MIN_FS = 10
        private const val MAX_FPS = 60
    }
}