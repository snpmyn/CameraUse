package com.qtone.camerause.widget.camera;

import android.hardware.usb.UsbDevice;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.callback.ICaptureCallBack;
import com.jiangdg.ausbc.callback.IEncodeDataCallBack;
import com.jiangdg.ausbc.callback.IPlayCallBack;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;

/**
 * @decs: 相机控制器
 * @author: 郑少鹏
 * @date: 2026/8/24 13:41
 * @version: v 1.0
 */
public class CameraController {
    /**
     * 实例
     */
    private static volatile CameraController instance;
    /**
     * 相机实例
     */
    private MultiCameraClient.ICamera iCamera;

    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     */
    private CameraController() {

    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static CameraController getInstance() {
        if (instance == null) {
            synchronized (CameraController.class) {
                if (instance == null) {
                    instance = new CameraController();
                }
            }
        }
        return instance;
    }

    /**
     * 获取单例
     *
     * @param iCamera 相机实例
     * @return 单例
     */
    public static CameraController getInstance(MultiCameraClient.ICamera iCamera) {
        if (instance == null) {
            synchronized (CameraController.class) {
                if (instance == null) {
                    instance = new CameraController();
                }
            }
        }
        instance.setCamera(iCamera);
        return instance;
    }

    /**
     * 获取相机实例
     *
     * @return 相机实例
     */
    public MultiCameraClient.ICamera getCamera() {
        return iCamera;
    }

    /**
     * 设置相机实例
     *
     * @param iCamera 相机实例
     */
    public void setCamera(MultiCameraClient.ICamera iCamera) {
        this.iCamera = iCamera;
    }

    /**
     * 打开相机
     *
     * @param cameraView    渲染载体
     * @param cameraRequest 相机请求参数
     * @param <T>           渲染载体类型
     */
    public <T> void openCamera(T cameraView, CameraRequest cameraRequest) {
        if (iCamera != null) {
            iCamera.openCamera(cameraView, cameraRequest);
        }
    }

    /**
     * 关闭摄像头预览
     */
    public void closeCamera() {
        if (iCamera != null) {
            iCamera.closeCamera();
        }
    }

    /**
     * 设置相机状态回调
     *
     * @param iCameraStateCallBack 相机状态回调
     */
    public void setCameraStateCallBack(ICameraStateCallBack iCameraStateCallBack) {
        if (iCamera != null) {
            iCamera.setCameraStateCallBack(iCameraStateCallBack);
        }
    }

    /**
     * 设置编码数据回调
     * <p>
     * H.264
     * AAC 裸流数据
     *
     * @param iEncodeDataCallBack 编码数据回调
     */
    public void setEncodeDataCallBack(IEncodeDataCallBack iEncodeDataCallBack) {
        if (iCamera != null) {
            iCamera.setEncodeDataCallBack(iEncodeDataCallBack);
        }
    }

    /**
     * 添加预览数据回调
     *
     * @param iPreviewDataCallBack 预览数据回调
     */
    public void addPreviewDataCallBack(IPreviewDataCallBack iPreviewDataCallBack) {
        if (iCamera != null) {
            iCamera.addPreviewDataCallBack(iPreviewDataCallBack);
        }
    }

    /**
     * 移除预览数据回调
     *
     * @param iPreviewDataCallBack 预览数据回调
     */
    public void removePreviewDataCallBack(IPreviewDataCallBack iPreviewDataCallBack) {
        if (iCamera != null) {
            iCamera.removePreviewDataCallBack(iPreviewDataCallBack);
        }
    }

    /**
     * 拍照
     *
     * @param iCaptureCallBack 拍照回调
     * @param savePath         保存路径
     */
    public void captureImage(ICaptureCallBack iCaptureCallBack, String savePath) {
        if (iCamera != null) {
            iCamera.captureImage(iCaptureCallBack, savePath);
        }
    }

    /**
     * 开始录制视频
     *
     * @param iCaptureCallBack 录制回调
     * @param savePath         保存路径
     * @param durationInSecond 视频文件自动切分时长秒
     */
    public void captureVideoStart(ICaptureCallBack iCaptureCallBack, String savePath, long durationInSecond) {
        if (iCamera != null) {
            iCamera.captureVideoStart(iCaptureCallBack, savePath, durationInSecond);
        }
    }

    /**
     * 停止录制视频
     */
    public void captureVideoStop() {
        if (iCamera != null) {
            iCamera.captureVideoStop();
        }
    }

    /**
     * 开始捕获音视频编码流
     * <p>
     * H.264
     * AAC 裸流数据
     */
    public void captureStreamStart() {
        if (iCamera != null) {
            iCamera.captureStreamStart();
        }
    }

    /**
     * 停止捕获音视频编码流
     */
    public void captureStreamStop() {
        if (iCamera != null) {
            iCamera.captureStreamStop();
        }
    }

    /**
     * 开始录制 MP3 音频
     *
     * @param iCaptureCallBack 录制回调
     * @param savePath         保存路径
     */
    public void captureAudioStart(ICaptureCallBack iCaptureCallBack, String savePath) {
        if (iCamera != null) {
            iCamera.captureAudioStart(iCaptureCallBack, savePath);
        }
    }

    /**
     * 停止录制 MP3 音频
     */
    public void captureAudioStop() {
        if (iCamera != null) {
            iCamera.captureAudioStop();
        }
    }

    /**
     * 开启麦克风实时回放
     *
     * @param iPlayCallBack 播放回调
     */
    public void startPlayMic(IPlayCallBack iPlayCallBack) {
        if (iCamera != null) {
            iCamera.startPlayMic(iPlayCallBack);
        }
    }

    /**
     * 停止麦克风实时回放
     */
    public void stopPlayMic() {
        if (iCamera != null) {
            iCamera.stopPlayMic();
        }
    }

    /**
     * 相机是否开启中
     *
     * @return 相机是否开启中
     */
    public boolean isCameraOpened() {
        return ((iCamera != null) && iCamera.isCameraOpened());
    }

    /**
     * 获取 USB 设备
     *
     * @return USB 设备
     */
    public UsbDevice getUsbDevice() {
        return (iCamera != null) ? iCamera.getUsbDevice() : null;
    }
}