package com.qtone.camerause.widget.storage;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Created on 2026/8/18.
 *
 * @author 郑少鹏
 * @desc 媒体存储配置
 * <p>
 * - 内部存储 (Internal Storage)
 * 应用独占私有空间
 * 位于 /data/user/0/com.qtone.camerause/files/
 * 电脑连接时完全不可见且无需权限
 * 由 appContext.getFilesDir() 获取
 * <p>
 * - 外部存储 (External Storage)
 * 所有应用共享的公共存储区
 * 位于 /storage/emulated/0/
 * 电脑连接时显示的 [内部共享存储空间] 对应此位置
 * <p>
 * - 外部私有存储 (External Private Storage)
 * 位于 /storage/emulated/0/Android/data/PackageName/...
 * 电脑连接时在 Android/data 专有目录下可见
 * 无需申请存储权限
 * 由 appContext.getExternalFilesDir(...) 获取
 * <p>
 * - 外部公共存储 (External Public Storage)
 * 位于 /storage/emulated/0/DCIM/ 或其它公共目录 (如 Pictures、Movies)
 * 电脑连接时根目录可见
 * 保存的媒体文件可直接在相册中展示
 * 由 Environment.getExternalStoragePublicDirectory(...) 获取
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
        init(context, folderName, StorageMode.EXTERNAL_PRIVATE);
    }

    /**
     * 初始化
     *
     * @param context     上下文
     * @param folderName  目录名称
     * @param storageMode 存储模式
     */
    public void init(Context context, String folderName, StorageMode storageMode) {
        init(context, folderName, storageMode, Environment.DIRECTORY_DCIM);
    }

    /**
     * 初始化
     *
     * @param context                     上下文
     * @param folderName                  目录名称
     * @param storageMode                 存储模式
     * @param externalPublicDirectoryType 外部公共目录类型
     *                                    仅 StorageMode.EXTERNAL_PUBLIC 模式下生效
     *                                    如 Environment.DIRECTORY_DCIM / Environment.DIRECTORY_PICTURES
     */
    public void init(Context context, String folderName, StorageMode storageMode, String externalPublicDirectoryType) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        String targetFolderName = TextUtils.isEmpty(folderName) ? "ZYR" : folderName;
        StorageMode mode = (storageMode == null) ? StorageMode.INTERNAL : storageMode;
        File baseDir;
        if (mode == StorageMode.EXTERNAL_PUBLIC) {
            // 外部公共存储
            // 如 /storage/emulated/0/DCIM 或 /storage/emulated/0/Pictures
            String dirType = TextUtils.isEmpty(externalPublicDirectoryType) ? Environment.DIRECTORY_DCIM : externalPublicDirectoryType;
            baseDir = Environment.getExternalStoragePublicDirectory(dirType);
        } else if (mode == StorageMode.EXTERNAL_PRIVATE) {
            // 外部私有存储
            // 无需动态申请 WRITE_EXTERNAL_STORAGE 权限 + 适用 Android 10+
            baseDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (baseDir == null) {
                // 外存挂载异常自动退化使用内部存储
                baseDir = appContext.getFilesDir();
            }
        } else {
            // 内部存储
            baseDir = appContext.getFilesDir();
        }
        imageDirectoryFile = new File(baseDir, targetFolderName);
        if (!imageDirectoryFile.exists()) {
            boolean isSuccess = imageDirectoryFile.mkdirs();
            Log.d(LogKit.TAG, "媒体存储文件夹初始化\n绝对路径 || " + imageDirectoryFile.getAbsolutePath() + "\n结果 || " + isSuccess);
        } else {
            Log.d(LogKit.TAG, "媒体存储文件夹已存在\n绝对路径 || " + imageDirectoryFile.getAbsolutePath());
        }
    }

    /**
     * 获取基础根目录文件
     * <p>
     * 场景一
     * 无需划分细分子目录、直接保存到根目录时调
     * 对应 Application 初始化时配置的主文件夹目录
     * 未指定 folderName 则默认目录名位 ZYR
     *
     * @return 基础根目录文件
     */
    public File getDirectoryFile() {
        if (imageDirectoryFile == null) {
            Log.w(LogKit.TAG, "媒体存储配置未在 Application 初始化");
        }
        return imageDirectoryFile;
    }

    /**
     * 通过存储类型获取专属子目录文件
     * <p>
     * 场景二
     * 针对不同业务模块划分独立子目录时调
     * 路径规则 [基础根目录]/[StorageType.subFolderName]
     * 注意 storageType 为 null 或未定义子目录将自动退化返回基础根目录文件
     *
     * @param storageType 存储类型
     *                    参考 {@link StorageType}
     * @return 专属子目录文件
     */
    public File getDirectoryFileByStorageType(StorageType storageType) {
        File parentDir = getDirectoryFile();
        if (parentDir == null) {
            return null;
        }
        if ((storageType == null) || TextUtils.isEmpty(storageType.getSubFolderName())) {
            return parentDir;
        }
        File targetDir = new File(parentDir, storageType.getSubFolderName());
        if (!targetDir.exists()) {
            boolean isSuccess = targetDir.mkdirs();
            Log.d(LogKit.TAG, "媒体存储文件夹初始化 [" + storageType.name() + "]\n绝对路径 || " + targetDir.getAbsolutePath() + "\n结果 || " + isSuccess);
        }
        return targetDir;
    }

    /**
     * 生成保存文件
     *
     * @param storageType 存储类型
     * @param sourcePath  资源路径
     * @return 保存文件
     */
    public @Nullable File generateSaveFile(StorageType storageType, String sourcePath) {
        return generateSaveFile(storageType, sourcePath, -1);
    }

    /**
     * 生成保存文件
     *
     * @param storageType 存储类型
     * @param sourcePath  资源路径
     * @param subIndex    子下标
     * @return 保存文件
     */
    public @Nullable File generateSaveFile(StorageType storageType, String sourcePath, int subIndex) {
        File dir = getDirectoryFileByStorageType(storageType);
        if (dir == null) {
            return null;
        }
        String fileName = MediaFileNameEngine.generateFileName(storageType, sourcePath, subIndex);
        return new File(dir, fileName);
    }
}