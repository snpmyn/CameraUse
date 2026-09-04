package com.qtone.camerause.widget.storage;

import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

import java.io.File;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 媒体存储清理辅助者
 */
public class MediaStorageCleanHelper {
    /**
     * 根据媒体存储类型清理目录
     *
     * @param mediaStorageType 媒体存储类型
     * @return 是否成功删除该目录下所有文件
     */
    public static boolean clearDirectoryByMediaStorageType(MediaStorageType mediaStorageType) {
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(mediaStorageType);
        if ((targetDir == null) || !targetDir.exists() || !targetDir.isDirectory()) {
            return false;
        }
        File[] files = targetDir.listFiles();
        if (files == null) {
            return true;
        }
        boolean isAllDeleted = true;
        for (File file : files) {
            if (file.isFile() && !file.delete()) {
                isAllDeleted = false;
                Log.w(LogKit.TAG, "删除指定媒体存储类型缓存文件失败 || " + file.getAbsolutePath());
            }
        }
        Log.d(LogKit.TAG, "清理媒体存储目录 [" + mediaStorageType.name() + "]\n清理结果 || " + isAllDeleted);
        return isAllDeleted;
    }

    /**
     * 根据媒体存储类型清理过期文件
     *
     * @param mediaStorageType 媒体存储类型
     * @param maxKeepMs        最大保存时长毫秒
     */
    public static void clearExpiredFilesByMediaStorageType(MediaStorageType mediaStorageType, long maxKeepMs) {
        if (maxKeepMs <= 0) {
            return;
        }
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(mediaStorageType);
        if ((targetDir == null) || !targetDir.exists() || !targetDir.isDirectory()) {
            return;
        }
        File[] files = targetDir.listFiles();
        if (files == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        int deletedCount = 0;
        for (File file : files) {
            if (file.isFile() && ((currentTime - file.lastModified()) > maxKeepMs)) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }
        Log.d(LogKit.TAG, "清理过期媒体文件 [" + mediaStorageType.name() + "]\n清理数量 || " + deletedCount);
    }

    /**
     * 根据媒体存储类型获取目录大小
     *
     * @param mediaStorageType 媒体存储类型
     * @return 目录大小
     */
    public static long getDirectorySizeByMediaStorageType(MediaStorageType mediaStorageType) {
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(mediaStorageType);
        if ((targetDir == null) || !targetDir.exists() || !targetDir.isDirectory()) {
            return 0L;
        }
        File[] files = targetDir.listFiles();
        if (files == null) {
            return 0L;
        }
        long totalSize = 0L;
        for (File file : files) {
            if (file.isFile()) {
                totalSize += file.length();
            }
        }
        return totalSize;
    }
}