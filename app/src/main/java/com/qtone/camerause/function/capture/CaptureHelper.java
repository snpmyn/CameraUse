package com.qtone.camerause.function.capture;

import android.os.Handler;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.qtone.camerause.function.storage.MediaStorageConfig;
import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 拍照辅助者
 */
public class CaptureHelper {
    /**
     * 连拍序号
     * <p>
     * 自动生成 + 规避同毫秒生成文件名冲突覆盖
     */
    private static final AtomicLong burstSequence = new AtomicLong(0);

    /**
     * 重置连拍序号
     */
    public static void resetBurstSequence() {
        burstSequence.set(0);
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
        if ((iCamera == null) || !iCamera.isCameraOpened()) {
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
        File mediaDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(MediaStorageConfig.StorageType.CAPTURE);
        if (mediaDir == null) {
            notifyError(handler, onCaptureCallBack, "无法获取图片存储目录");
            return null;
        }
        if (!mediaDir.exists()) {
            boolean created = mediaDir.mkdirs();
            if (!created && !mediaDir.exists()) {
                Log.e(LogKit.TAG, "创建图片存储目录失败 || " + mediaDir.getAbsolutePath());
                notifyError(handler, onCaptureCallBack, "创建图片存储目录失败");
                return null;
            }
        }
        // 文件名
        // IMG_毫秒时间戳_序号.jpg
        String fileName = String.format(Locale.CHINA, "IMG_%d_%04d.jpg", System.currentTimeMillis(), burstSequence.incrementAndGet());
        return new File(mediaDir, fileName).getAbsolutePath();
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