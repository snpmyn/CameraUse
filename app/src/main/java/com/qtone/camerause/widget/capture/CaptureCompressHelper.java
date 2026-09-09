package com.qtone.camerause.widget.capture;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.qtone.camerause.model.setting.kit.SharedPreferencesKit;
import com.qtone.camerause.util.log.LogKit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.zibin.luban.api.Luban;
import top.zibin.luban.api.OnCompressListener;

/**
 * Created on 2026/9/8.
 *
 * @author 郑少鹏
 * @desc 拍照压缩辅助者
 */
public class CaptureCompressHelper {
    /**
     * 增强实现
     */
    private ExecutorService compressExecutor;

    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     */
    private CaptureCompressHelper() {

    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static CaptureCompressHelper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 获取压缩单线程池
     *
     * @return 压缩单线程池
     */
    private synchronized ExecutorService getCompressExecutor() {
        if ((compressExecutor == null) || compressExecutor.isShutdown()) {
            compressExecutor = Executors.newSingleThreadExecutor();
        }
        return compressExecutor;
    }

    /**
     * 压缩并覆盖
     * <p>
     * 原路径不变
     * 文件名不变
     *
     * @param context                   上下文
     * @param originalPath              原始路径
     * @param onCaptureCompressCallback 拍照压缩回调
     */
    public void compressAndOverwrite(Context context, String originalPath, OnCaptureCompressCallback onCaptureCompressCallback) {
        if ((context == null) || (originalPath == null) || !SharedPreferencesKit.isPhotoCompressionEnabled(context)) {
            if (onCaptureCompressCallback != null) {
                onCaptureCompressCallback.onResult(originalPath);
            }
            return;
        }
        getCompressExecutor().execute(() -> {
            File rawFile = new File(originalPath);
            if (!rawFile.exists()) {
                if (onCaptureCompressCallback != null) {
                    onCaptureCompressCallback.onResult(originalPath);
                }
                return;
            }
            // 输出 Luban 临时压缩文件到系统 cache 目录
            // 彻底隔离同名文件冲突
            String cacheDir = context.getCacheDir().getAbsolutePath();
            Luban.with(context)
                    .load(originalPath)
                    .setTargetDir(cacheDir)
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {
                            Log.d(LogKit.TAG, "开始压缩");
                        }

                        @Override
                        public void onSuccess(@NonNull File compressedFile) {
                            if (compressedFile.exists()) {
                                // 强制写回压缩后内容
                                // 以覆盖原图路径
                                boolean isOverwritten = copyAndOverwrite(compressedFile, rawFile);
                                // 清理 cache 临时压缩文件
                                boolean isDeleted = compressedFile.delete();
                                if (!isDeleted) {
                                    Log.w(LogKit.TAG, "临时压缩文件删除失败 || " + compressedFile.getAbsolutePath());
                                }
                                if (isOverwritten) {
                                    Log.d(LogKit.TAG, "压缩成功并覆盖成功 || " + originalPath);
                                } else {
                                    Log.e(LogKit.TAG, "压缩成功但覆盖失败 - 保留原图 || " + originalPath);
                                }
                            }
                            if (onCaptureCompressCallback != null) {
                                onCaptureCompressCallback.onResult(originalPath);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.e(LogKit.TAG, "压缩错误 || " + e.getMessage());
                            if (onCaptureCompressCallback != null) {
                                onCaptureCompressCallback.onResult(originalPath);
                            }
                        }
                    }).launch();
        });
    }

    /**
     * 拷贝并覆盖
     *
     * @param tempFile     临时文件
     * @param originalFile 原始文件
     * @return 是否拷贝并覆盖成功
     */
    private boolean copyAndOverwrite(File tempFile, File originalFile) {
        try (InputStream fileInputStream = new FileInputStream(tempFile);
             OutputStream fileOutputStream = new FileOutputStream(originalFile, false)) {
            // append 为 false 确保清空原文件直接覆写
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.flush();
            return true;
        } catch (Exception e) {
            Log.e(LogKit.TAG, "拷贝并覆盖失败 || " + e.getMessage());
            return false;
        }
    }

    /**
     * 释放
     */
    public void release() {
        if ((compressExecutor != null) && !compressExecutor.isShutdown()) {
            compressExecutor.shutdown();
            compressExecutor = null;
        }
    }

    /**
     * 拍照压缩回调
     */
    public interface OnCaptureCompressCallback {
        /**
         * 结果
         *
         * @param finalPath 最终路径
         *                  必为原始路径
         */
        void onResult(String finalPath);
    }

    private static class InstanceHolder {
        private static final CaptureCompressHelper INSTANCE = new CaptureCompressHelper();
    }
}