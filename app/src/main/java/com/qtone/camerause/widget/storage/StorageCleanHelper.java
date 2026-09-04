package com.qtone.camerause.widget.storage;

import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

import java.io.File;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 存储清理辅助者
 */
public class StorageCleanHelper {
    /**
     * 根据存储类型清理目录
     *
     * @param storageType 存储类型
     * @return 是否成功删除该目录下所有文件
     */
    public static boolean clearDirectoryByStorageType(StorageType storageType) {
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(storageType);
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
                Log.w(LogKit.TAG, "删除指定存储类型缓存文件失败 || " + file.getAbsolutePath());
            }
        }
        Log.d(LogKit.TAG, "清理媒体存储目录 [" + storageType.name() + "]\n清理结果 || " + isAllDeleted);
        return isAllDeleted;
    }

    /**
     * 根据存储类型清理过期文件
     *
     * @param storageType 存储类型
     * @param maxKeepMs   最大保存时长毫秒
     */
    public static void clearExpiredFilesByStorageType(StorageType storageType, long maxKeepMs) {
        if (maxKeepMs <= 0) {
            return;
        }
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(storageType);
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
        Log.d(LogKit.TAG, "清理过期媒体文件 [" + storageType.name() + "]\n清理数量 || " + deletedCount);
    }

    /**
     * 根据存储类型获取目录大小
     *
     * @param storageType 存储类型
     * @return 目录大小
     */
    public static long getDirectorySizeByStorageType(StorageType storageType) {
        File targetDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(storageType);
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