package com.qtone.camerause.widget.camera;

import android.hardware.usb.UsbDevice;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.callback.ICaptureCallBack;
import com.jiangdg.ausbc.callback.IEncodeDataCallBack;
import com.jiangdg.ausbc.callback.IPlayCallBack;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.camera.bean.PreviewSize;
import com.jiangdg.ausbc.render.effect.AbstractEffect;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.widget.IAspectRatio;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

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

    /* =================================================================================== */
    /*                               新增 CameraFragment API                                */
    /* =================================================================================== */

    /**
     * 获取默认特效滤镜
     *
     * @param iCamera 相机实例
     * @return 默认特效滤镜
     */
    public AbstractEffect getDefaultEffect(MultiCameraClient.ICamera iCamera) {
        return (iCamera != null) ? iCamera.getDefaultEffect() : null;
    }

    /**
     * 更新分辨率
     *
     * @param iCamera 相机实例
     * @param width   物理帧宽
     * @param height  物理帧高
     */
    public void updateResolution(MultiCameraClient.ICamera iCamera, int width, int height) {
        if (iCamera != null) {
            iCamera.updateResolution(width, height);
        }
    }

    /**
     * 更新预览尺寸
     *
     * @param iCamera    相机实例
     * @param width      物理帧宽
     * @param height     物理帧高
     * @param cameraView 渲染载体
     * @param onResult   结果回调
     */
    public void updatePreviewSize(MultiCameraClient.ICamera iCamera, int width, int height, IAspectRatio cameraView, Function2<Boolean, String, Unit> onResult) {
        if (iCamera instanceof CameraUVC) {
            // 透传当前预览 View / Surface 与双参回调
            ((CameraUVC) iCamera).updatePreviewSize(width, height, cameraView, onResult);
        } else {
            // 当前设备不是 UVC 相机 -> 直接返回失败及 null 模式
            if (onResult != null) {
                onResult.invoke(false, null);
            }
        }
    }

    /**
     * 获取所有预览尺寸
     *
     * @param iCamera     相机实例
     * @param aspectRatio 宽高比例
     *                    可传 null
     * @return 所有预览尺寸
     */
    public List<PreviewSize> getAllPreviewSizes(MultiCameraClient.ICamera iCamera, Double aspectRatio) {
        return (iCamera != null) ? iCamera.getAllPreviewSizes(aspectRatio) : null;
    }

    /**
     * 获取当前预览尺寸
     *
     * @param iCamera 相机实例
     * @return 当前预览尺寸
     */
    public PreviewSize getCurrentPreviewSize(MultiCameraClient.ICamera iCamera) {
        if ((iCamera == null) || (iCamera.getCameraRequest() == null)) {
            return null;
        }
        CameraRequest cameraRequest = iCamera.getCameraRequest();
        return new PreviewSize(cameraRequest.getPreviewWidth(), cameraRequest.getPreviewHeight());
    }

    /**
     * 添加渲染特效 / 滤镜
     * <p>
     * 需要开启 OpenGL 渲染
     *
     * @param iCamera        相机实例
     * @param abstractEffect 滤镜对象
     */
    public void addRenderEffect(MultiCameraClient.ICamera iCamera, AbstractEffect abstractEffect) {
        if ((iCamera != null) && (abstractEffect != null)) {
            iCamera.addRenderEffect(abstractEffect);
        }
    }

    /**
     * 移除渲染特效 / 滤镜
     *
     * @param iCamera        相机实例
     * @param abstractEffect 滤镜对象
     */
    public void removeRenderEffect(MultiCameraClient.ICamera iCamera, AbstractEffect abstractEffect) {
        if ((iCamera != null) && (abstractEffect != null)) {
            iCamera.removeRenderEffect(abstractEffect);
        }
    }

    /**
     * 更新渲染特效 / 滤镜
     *
     * @param iCamera        相机实例
     * @param classifyId     分类 ID
     * @param abstractEffect 滤镜对象
     *                       新滤镜 [传 null 则清除该分类下滤镜]
     */
    public void updateRenderEffect(MultiCameraClient.ICamera iCamera, int classifyId, AbstractEffect abstractEffect) {
        if (iCamera != null) {
            iCamera.updateRenderEffect(classifyId, abstractEffect);
        }
    }

    /**
     * 设置旋转类型
     * <p>
     * 预览画面旋转角度
     *
     * @param iCamera    相机实例
     * @param rotateType 旋转类型
     *                   如 {@link RotateType#ANGLE_90}
     */
    public void setRotateType(MultiCameraClient.ICamera iCamera, RotateType rotateType) {
        if (iCamera != null) {
            iCamera.setRotateType(rotateType);
        }
    }

    /**
     * 设置渲染尺寸
     * <p>
     * 通常用于 SurfaceView / TextureView 尺寸变更时
     *
     * @param iCamera       相机实例
     * @param surfaceWidth  Surface 宽
     * @param surfaceHeight Surface 高
     */
    public void setRenderSize(MultiCameraClient.ICamera iCamera, int surfaceWidth, int surfaceHeight) {
        if (iCamera != null) {
            iCamera.setRenderSize(surfaceWidth, surfaceHeight);
        }
    }

    /* =================================================================================== */
    /*                            UVC 硬件参数调控 API (需 CameraUVC)                        */
    /* =================================================================================== */

    /**
     * 发送相机指令
     * <p>
     * UVC 相机自定义指令
     *
     * @param iCamera 相机实例
     * @param command 指令值
     *                十六进制
     */
    public void sendCameraCommand(MultiCameraClient.ICamera iCamera, int command) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).sendCameraCommand(command);
        }
    }

    /**
     * 设置自动对焦
     *
     * @param iCamera 相机实例
     * @param focus   是否自动对焦
     */
    public void setAutoFocus(MultiCameraClient.ICamera iCamera, boolean focus) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setAutoFocus(focus);
        }
    }

    /**
     * 获取自动对焦状态
     *
     * @param iCamera 相机实例
     * @return 是否自动对焦 [非 UVC 相机返回 null]
     */
    public Boolean getAutoFocus(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getAutoFocus() : null;
    }

    /**
     * 重置自动对焦
     *
     * @param iCamera 相机实例
     */
    public void resetAutoFocus(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetAutoFocus();
        }
    }

    /**
     * 设置亮度
     *
     * @param iCamera    相机实例
     * @param brightness 亮度值
     */
    public void setBrightness(MultiCameraClient.ICamera iCamera, int brightness) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setBrightness(brightness);
        }
    }

    /**
     * 获取亮度
     *
     * @param iCamera 相机实例
     * @return 亮度值 [非 UVC 相机返回 null]
     */
    public Integer getBrightness(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getBrightness() : null;
    }

    /**
     * 重置亮度
     *
     * @param iCamera 相机实例
     */
    public void resetBrightness(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetBrightness();
        }
    }

    /**
     * 设置对比度
     *
     * @param iCamera  相机实例
     * @param contrast 对比度值
     */
    public void setContrast(MultiCameraClient.ICamera iCamera, int contrast) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setContrast(contrast);
        }
    }

    /**
     * 获取对比度
     *
     * @param iCamera 相机实例
     * @return 对比度值 [非 UVC 相机返回 null]
     */
    public Integer getContrast(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getContrast() : null;
    }

    /**
     * 重置对比度
     *
     * @param iCamera 相机实例
     */
    public void resetContrast(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetContrast();
        }
    }

    /**
     * 设置增益
     *
     * @param iCamera 相机实例
     * @param gain    增益值
     */
    public void setGain(MultiCameraClient.ICamera iCamera, int gain) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setGain(gain);
        }
    }

    /**
     * 获取增益
     *
     * @param iCamera 相机实例
     * @return 增益值 [非 UVC 相机返回 null]
     */
    public Integer getGain(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getGain() : null;
    }

    /**
     * 重置增益
     *
     * @param iCamera 相机实例
     */
    public void resetGain(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetGain();
        }
    }

    /**
     * 设置 Gamma 值
     *
     * @param iCamera 相机实例
     * @param gamma   Gamma 值
     */
    public void setGamma(MultiCameraClient.ICamera iCamera, int gamma) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setGamma(gamma);
        }
    }

    /**
     * 获取 Gamma 值
     *
     * @param iCamera 相机实例
     * @return Gamma 值 [非 UVC 相机返回 null]
     */
    public Integer getGamma(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getGamma() : null;
    }

    /**
     * 重置 Gamma 值
     *
     * @param iCamera 相机实例
     */
    public void resetGamma(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetGamma();
        }
    }

    /**
     * 设置色调
     *
     * @param iCamera 相机实例
     * @param hue     色调值
     */
    public void setHue(MultiCameraClient.ICamera iCamera, int hue) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setHue(hue);
        }
    }

    /**
     * 获取色调
     *
     * @param iCamera 相机实例
     * @return 色调值 [非 UVC 相机返回 null]
     */
    public Integer getHue(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getHue() : null;
    }

    /**
     * 重置色调
     *
     * @param iCamera 相机实例
     */
    public void resetHue(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetHue();
        }
    }

    /**
     * 设置变焦
     *
     * @param iCamera 相机实例
     * @param zoom    变焦值
     */
    public void setZoom(MultiCameraClient.ICamera iCamera, int zoom) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setZoom(zoom);
        }
    }

    /**
     * 获取变焦
     *
     * @param iCamera 相机实例
     * @return 变焦值 [非 UVC 相机返回 null]
     */
    public Integer getZoom(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getZoom() : null;
    }

    /**
     * 重置变焦
     *
     * @param iCamera 相机实例
     */
    public void resetZoom(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetZoom();
        }
    }

    /**
     * 设置锐度
     *
     * @param iCamera   相机实例
     * @param sharpness 锐度值
     */
    public void setSharpness(MultiCameraClient.ICamera iCamera, int sharpness) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setSharpness(sharpness);
        }
    }

    /**
     * 获取锐度
     *
     * @param iCamera 相机实例
     * @return 锐度值 [非 UVC 相机返回 null]
     */
    public Integer getSharpness(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getSharpness() : null;
    }

    /**
     * 重置锐度
     *
     * @param iCamera 相机实例
     */
    public void resetSharpness(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetSharpness();
        }
    }

    /**
     * 设置饱和度
     *
     * @param iCamera    相机实例
     * @param saturation 饱和度值
     */
    public void setSaturation(MultiCameraClient.ICamera iCamera, int saturation) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).setSaturation(saturation);
        }
    }

    /**
     * 获取饱和度
     *
     * @param iCamera 相机实例
     * @return 饱和度值 [非 UVC 相机返回 null]
     */
    public Integer getSaturation(MultiCameraClient.ICamera iCamera) {
        return (iCamera instanceof CameraUVC) ? ((CameraUVC) iCamera).getSaturation() : null;
    }

    /**
     * 重置饱和度
     *
     * @param iCamera 相机实例
     */
    public void resetSaturation(MultiCameraClient.ICamera iCamera) {
        if (iCamera instanceof CameraUVC) {
            ((CameraUVC) iCamera).resetSaturation();
        }
    }
}