package com.qtone.camerause.widget.capture;

import android.os.Handler;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.storage.MediaFileNameEngine;
import com.qtone.camerause.widget.storage.MediaStorageConfig;
import com.qtone.camerause.widget.storage.StorageType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 拍照辅助者
 */
public class CaptureHelper {
    /**
     * 重置序号
     */
    public static void resetSequence() {
        MediaFileNameEngine.resetSequence();
    }

    /**
     * 相机是否未准备就绪
     *
     * @param iCamera           相机实例
     * @param handler           线程消息调度器
     * @param onCaptureCallBack 拍照回调
     * @return 相机是否未准备就绪
     */
    public static boolean isCameraNotReady(MultiCameraClient.ICamera iCamera, Handler handler, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        if (!CameraController.getInstance().isCameraOpened(iCamera)) {
            notifyError(handler, onCaptureCallBack, "相机未准备就绪");
            return true;
        }
        return false;
    }

    /**
     * 生成保存路径
     *
     * @param handler           线程消息调度器
     * @param onCaptureCallBack 拍照回调
     * @return 保存路径
     */
    public static @Nullable String generateSavePath(Handler handler, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        File targetFile = MediaStorageConfig.getInstance().generateSaveFile(StorageType.CAPTURE, null);
        if (targetFile == null) {
            notifyError(handler, onCaptureCallBack, "无法获取照片存储目录");
            return null;
        }
        File parentDir = targetFile.getParentFile();
        if ((parentDir != null) && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created && !parentDir.exists()) {
                Log.e(LogKit.TAG, "创建照片存储目录失败 || " + parentDir.getAbsolutePath());
                notifyError(handler, onCaptureCallBack, "创建照片存储目录失败");
                return null;
            }
        }
        return targetFile.getAbsolutePath();
    }

    /**
     * 通知开始
     *
     * @param handler           线程消息调度器
     * @param onCaptureCallBack 拍照回调
     */
    public static void notifyBegin(@NotNull Handler handler, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                Log.d(LogKit.TAG, "拍照开始");
                onCaptureCallBack.onCaptureBegin();
            }
        });
    }

    /**
     * 通知错误
     *
     * @param handler           线程消息调度器
     * @param onCaptureCallBack 拍照回调
     * @param errorMsg          错误消息
     */
    public static void notifyError(@NotNull Handler handler, CaptureProcessor.OnCaptureCallback onCaptureCallBack, String errorMsg) {
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                Log.e(LogKit.TAG, "拍照错误 || " + errorMsg);
                onCaptureCallBack.onCaptureError(errorMsg);
            }
        });
    }
}