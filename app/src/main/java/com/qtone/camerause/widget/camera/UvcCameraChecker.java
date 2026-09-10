package com.qtone.camerause.widget.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.qtone.camerause.util.log.LogKit;

/**
 * Created on 2026/9/10.
 *
 * @author 郑少鹏
 * @desc UVC 相机检测器
 */
public class UvcCameraChecker {
    /**
     * 检测 UVC 相机支持
     * <p>
     * 校验当前设备及系统是否支持通过原生 Camera2 使用 UVC 外接高拍仪 / 摄像头
     *
     * @param context                上下文
     * @param uvcCameraCheckCallback UVC 相机检测回调
     */
    public static void checkUvcCameraSupport(Context context, UvcCameraCheckCallback uvcCameraCheckCallback) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            uvcCameraCheckCallback.onResult(false, false, null, "系统 CameraManager 服务不可用");
            return;
        }
        String externalCameraId = null;
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            Log.d(LogKit.TAG, "UVC 相机检测 - 当前系统注册的 Camera 总数: " + cameraIdList.length);
            // 1. 遍历寻找 LENS_FACING_EXTERNAL 节点
            for (String id : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if ((facing != null) && (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)) {
                    externalCameraId = id;
                    Log.d(LogKit.TAG, "成功识别到外接 UVC 摄像头节点，Camera ID: " + id);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(LogKit.TAG, "UVC 相机检测 - 访问 Camera 失败", e);
            uvcCameraCheckCallback.onResult(false, false, null, "访问 CameraAccessException: " + e.getMessage());
            return;
        }
        // 2. 如果没有找到 EXTERNAL 节点
        if (externalCameraId == null) {
            uvcCameraCheckCallback.onResult(false, false, null, "系统未找到外接摄像头！当前 Android 固件裁剪或关闭了 External Camera Provider (HAL)");
            return;
        }
        // 3. 找到节点
        // 尝试 openCamera 验证硬件通信与通道可用性
        final String targetId = externalCameraId;
        Handler handler = new Handler(Looper.getMainLooper());
        try {
            cameraManager.openCamera(targetId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(LogKit.TAG, "UVC 相机检测 - Camera2 成功打开外接 UVC 摄像头: " + targetId);
                    // 验证成功
                    // 立即释放资源
                    camera.close();
                    uvcCameraCheckCallback.onResult(true, true, targetId, "原生 Camera2 完美支持！节点 ID 为 [" + targetId + "] 且设备可正常打开通信"
                    );
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(LogKit.TAG, "UVC 相机检测 - Camera2 外接设备已断开连接: " + targetId);
                    camera.close();
                    uvcCameraCheckCallback.onResult(true, false, targetId, "找到外接节点 [" + targetId + "]，但设备被断开 (可能供电不足或设备已拨出)。");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(LogKit.TAG, "UVC 相机检测 - Camera2 打开外接设备失败, error code: " + error);
                    camera.close();
                    uvcCameraCheckCallback.onResult(true, false, targetId, "找到外接节点 [" + targetId + "]，但打开失败，错误代码 (onError): " + error);
                }
            }, handler);
        } catch (CameraAccessException e) {
            Log.e(LogKit.TAG, "UVC 相机检测 - 调用 openCamera 抛出异常", e);
            uvcCameraCheckCallback.onResult(true, false, targetId, "找到外接节点 [" + targetId + "]，但打开抛出异常: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e(LogKit.TAG, "UVC 相机检测 - 未获取 CAMERA 动态权限", e);
            uvcCameraCheckCallback.onResult(true, false, targetId, "找到外接节点 [" + targetId + "]，但缺少 android.permission.CAMERA 权限。");
        }
    }

    /**
     * UVC 相机检测回调
     */
    public interface UvcCameraCheckCallback {
        /**
         * 结果
         *
         * @param isSupported 系统 HAL 层是否挂载并识别到 LENS_FACING_EXTERNAL 外接设备
         * @param canOpen     是否能够成功通过 Camera2 打开设备
         *                    排除权限拒绝、设备占用或供电不足导致的打开失败
         * @param cameraId    识别到外接摄像头 ID
         * @param message     详细诊断描述信息
         */
        void onResult(boolean isSupported, boolean canOpen, String cameraId, String message);
    }
}