package com.qtone.camerause.util.media;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import androidx.annotation.Nullable;

import com.qtone.camerause.util.log.LogKit;

/**
 * Created on 2026/8/18.
 *
 * @author 郑少鹏
 * @desc 媒体扫描配套原件
 */
public class MediaScanKit {
    /**
     * 扫描单个文件
     *
     * @param context  上下文
     * @param filePath 文件路径
     */
    public static void scanSingleFile(@Nullable Context context, @Nullable String filePath) {
        scanSingleFile(context, filePath, null);
    }

    /**
     * 扫描单个文件
     *
     * @param context  上下文
     * @param filePath 文件路径
     * @param mimeType 数据类型
     *                 例如 image/jpeg
     *                 传 null 自动根据扩展名推导
     */
    public static void scanSingleFile(@Nullable Context context, @Nullable String filePath, @Nullable String mimeType) {
        if ((context == null) || (filePath == null) || filePath.trim().isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        String[] mimeTypes = (mimeType != null) ? new String[]{mimeType} : null;
        MediaScannerConnection.scanFile(appContext, new String[]{filePath}, mimeTypes, (path, uri) -> Log.d(LogKit.TAG, "媒体库单个刷新完成\nPath || " + path + "\nUri || " + uri));
    }

    /**
     * 扫描批量文件
     *
     * @param context   上下文
     * @param filePaths 批量文件路径
     */
    public static void scanBatchFile(@Nullable Context context, @Nullable String[] filePaths) {
        if ((context == null) || (filePaths == null) || (filePaths.length == 0)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        MediaScannerConnection.scanFile(appContext, filePaths, null, (path, uri) -> Log.d(LogKit.TAG, "媒体库批量刷新完成\nPath || " + path + "\nUri || " + uri));
    }
}