package com.qtone.camerause.function.storage;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.qtone.camerause.utils.log.LogKit;

import java.io.File;

/**
 * Created on 2026/8/18.
 *
 * @author 郑少鹏
 * @desc 媒体存储配置
 * <p>
 * - 内部存储 (Internal Storage)
 * App 独占私有空间
 * 位于 /data/user/0/com.qtone.camerause/files/
 * 电脑连接时完全不可见且无需权限
 * 由 appContext.getFilesDir() 获取
 * <p>
 * - 外部存储 (External Storage)
 * 所有应用共享的公共存储区
 * 位于 /storage/emulated/0/
 * 电脑连接时显示的 [内部共享存储空间] 对应此位置
 */
public class MediaStorageConfig {
    /**
     * 单例
     */
    private static volatile MediaStorageConfig instance;
    /**
     * 图片目录文件
     */
    private File imageDirectoryFile;

    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     */
    private MediaStorageConfig() {

    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static MediaStorageConfig getInstance() {
        if (instance == null) {
            synchronized (MediaStorageConfig.class) {
                if (instance == null) {
                    instance = new MediaStorageConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param context    上下文
     * @param folderName 目录名称
     */
    public void init(Context context, String folderName) {
        init(context, folderName, StorageMode.EXTERNAL);
    }

    /**
     * 初始化
     *
     * @param context     上下文
     * @param folderName  目录名称
     * @param storageMode 存储模式
     */
    public void init(Context context, String folderName, StorageMode storageMode) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        String targetFolderName = TextUtils.isEmpty(folderName) ? "CameraUse" : folderName;
        StorageMode mode = (storageMode == null) ? StorageMode.INTERNAL : storageMode;
        File baseDir;
        if (mode == StorageMode.EXTERNAL) {
            // 优先使用 App 私有外部存储目录 (不需要动态申请 WRITE_EXTERNAL_STORAGE 权限 + 适用 Android 10+)
            baseDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (baseDir == null) {
                // 外存挂载异常时自动退化使用内部存储目录
                baseDir = appContext.getFilesDir();
            }
        } else {
            // 强制使用 App 私有内部存储目录
            baseDir = appContext.getFilesDir();
        }
        imageDirectoryFile = new File(baseDir, targetFolderName);
        if (!imageDirectoryFile.exists()) {
            boolean isSuccess = imageDirectoryFile.mkdirs();
            Log.d(LogKit.TAG, "图片存储文件夹初始化 || " + imageDirectoryFile.getAbsolutePath() + "\n结果 || " + isSuccess);
        } else {
            Log.d(LogKit.TAG, "图片存储文件夹已存在 || " + imageDirectoryFile.getAbsolutePath());
        }
    }

    /**
     * 获取图片目录文件
     *
     * @return 图片目录文件
     */
    public File getImageDirectoryFile() {
        if (imageDirectoryFile == null) {
            Log.w(LogKit.TAG, "媒体存储配置未在 Application 初始化");
        }
        return imageDirectoryFile;
    }

    /**
     * 存储模式
     */
    public enum StorageMode {
        /**
         * 外部存储
         */
        EXTERNAL,
        /**
         * 内部存储
         */
        INTERNAL
    }
}