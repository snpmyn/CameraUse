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
     * 打开相机
     *
     * @param iCamera       相机实例
     * @param cameraView    渲染载体
     * @param cameraRequest 相机请求参数
     * @param <T>           渲染载体类型
     */
    public <T> void openCamera(MultiCameraClient.ICamera iCamera, T cameraView, CameraRequest cameraRequest) {
        if (iCamera != null) {
            iCamera.openCamera(cameraView, cameraRequest);
        }
    }

    /**
     * 关闭摄像头预览
     *
     * @param iCamera 相机实例
     */
    public void closeCamera(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.closeCamera();
        }
    }

    /**
     * 设置相机状态回调
     *
     * @param iCamera              相机实例
     * @param iCameraStateCallBack 相机状态回调
     */
    public void setCameraStateCallBack(MultiCameraClient.ICamera iCamera, ICameraStateCallBack iCameraStateCallBack) {
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
     * @param iCamera             相机实例
     * @param iEncodeDataCallBack 编码数据回调
     */
    public void setEncodeDataCallBack(MultiCameraClient.ICamera iCamera, IEncodeDataCallBack iEncodeDataCallBack) {
        if (iCamera != null) {
            iCamera.setEncodeDataCallBack(iEncodeDataCallBack);
        }
    }

    /**
     * 添加预览数据回调
     *
     * @param iCamera              相机实例
     * @param iPreviewDataCallBack 预览数据回调
     */
    public void addPreviewDataCallBack(MultiCameraClient.ICamera iCamera, IPreviewDataCallBack iPreviewDataCallBack) {
        if (iCamera != null) {
            iCamera.addPreviewDataCallBack(iPreviewDataCallBack);
        }
    }

    /**
     * 移除预览数据回调
     *
     * @param iCamera              相机实例
     * @param iPreviewDataCallBack 预览数据回调
     */
    public void removePreviewDataCallBack(MultiCameraClient.ICamera iCamera, IPreviewDataCallBack iPreviewDataCallBack) {
        if (iCamera != null) {
            iCamera.removePreviewDataCallBack(iPreviewDataCallBack);
        }
    }

    /**
     * 拍照
     *
     * @param iCamera          相机实例
     * @param iCaptureCallBack 拍照回调
     * @param savePath         保存路径
     */
    public void captureImage(MultiCameraClient.ICamera iCamera, ICaptureCallBack iCaptureCallBack, String savePath) {
        if (iCamera != null) {
            iCamera.captureImage(iCaptureCallBack, savePath);
        }
    }

    /**
     * 开始录制视频
     *
     * @param iCamera          相机实例
     * @param iCaptureCallBack 录制回调
     * @param savePath         保存路径
     * @param durationInSecond 视频文件自动切分时长秒
     */
    public void captureVideoStart(MultiCameraClient.ICamera iCamera, ICaptureCallBack iCaptureCallBack, String savePath, long durationInSecond) {
        if (iCamera != null) {
            iCamera.captureVideoStart(iCaptureCallBack, savePath, durationInSecond);
        }
    }

    /**
     * 停止录制视频
     *
     * @param iCamera 相机实例
     */
    public void captureVideoStop(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.captureVideoStop();
        }
    }

    /**
     * 开始捕获音视频编码流
     * <p>
     * H.264
     * AAC 裸流数据
     *
     * @param iCamera 相机实例
     */
    public void captureStreamStart(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.captureStreamStart();
        }
    }

    /**
     * 停止捕获音视频编码流
     *
     * @param iCamera 相机实例
     */
    public void captureStreamStop(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.captureStreamStop();
        }
    }

    /**
     * 开始录制 MP3 音频
     *
     * @param iCamera          相机实例
     * @param iCaptureCallBack 录制回调
     * @param savePath         保存路径
     */
    public void captureAudioStart(MultiCameraClient.ICamera iCamera, ICaptureCallBack iCaptureCallBack, String savePath) {
        if (iCamera != null) {
            iCamera.captureAudioStart(iCaptureCallBack, savePath);
        }
    }

    /**
     * 停止录制 MP3 音频
     *
     * @param iCamera 相机实例
     */
    public void captureAudioStop(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.captureAudioStop();
        }
    }

    /**
     * 开启麦克风实时回放
     *
     * @param iCamera       相机实例
     * @param iPlayCallBack 播放回调
     */
    public void startPlayMic(MultiCameraClient.ICamera iCamera, IPlayCallBack iPlayCallBack) {
        if (iCamera != null) {
            iCamera.startPlayMic(iPlayCallBack);
        }
    }

    /**
     * 停止麦克风实时回放
     *
     * @param iCamera 相机实例
     */
    public void stopPlayMic(MultiCameraClient.ICamera iCamera) {
        if (iCamera != null) {
            iCamera.stopPlayMic();
        }
    }

    /**
     * 相机是否开启中
     *
     * @param iCamera 相机实例
     * @return 相机是否开启中
     */
    public boolean isCameraOpened(MultiCameraClient.ICamera iCamera) {
        return ((iCamera != null) && iCamera.isCameraOpened());
    }

    /**
     * 获取 USB 设备
     *
     * @param iCamera 相机实例
     * @return USB 设备
     */
    public UsbDevice getUsbDevice(MultiCameraClient.ICamera iCamera) {
        return (iCamera != null) ? iCamera.getUsbDevice() : null;
    }
}