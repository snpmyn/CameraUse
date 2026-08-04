package com.qtone.camerause.kit;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc Asset 文件配套原件
 */
public class AssetFileKit {
    private static final String TAG = AssetFileKit.class.getSimpleName();

    /**
     * 拷贝 Asset 文件到缓存
     *
     * @param context          上下文
     * @param subdirectoryName 子目录名
     *                         若在根目录传 "" 或 null
     * @param fileName         文件名
     * @return 物理绝对路径 (拷贝失败或目录创建失败时返回空字符串 "")
     */
    @NotNull
    public static String copyAssetFileToCache(@NotNull Context context, String subdirectoryName, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "文件名不能为空");
            return "";
        }
        // 1. 处理目录路径
        // dirName 为 null 或 "" 时，直接使用 getFilesDir() 根目录。
        boolean hasDir = (subdirectoryName != null) && !subdirectoryName.trim().isEmpty();
        File dir = hasDir ? new File(context.getFilesDir(), subdirectoryName.trim()) : context.getFilesDir();
        // 判断目录创建结果
        if (!dir.exists()) {
            boolean isCreated = dir.mkdirs();
            if (!isCreated && !dir.exists()) {
                Log.e(TAG, "创建目标文件夹失败 || " + dir.getAbsolutePath());
                return "";
            }
        }
        File file = new File(dir, fileName);
        // 如果文件已存在且大小正常，不重复复制，直接返回路径。
        if (file.exists() && (file.length() > 0)) {
            return file.getAbsolutePath();
        }
        // 2. 拼接 Asset 中的相对路径
        // dirName 为 null 或 "" 时，直接读取 assets 根目录。
        String assetPath = hasDir ? (subdirectoryName.trim() + "/" + fileName) : fileName;
        try (InputStream inputStream = context.getAssets().open(assetPath);
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[2048];
            int byteCount;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();
            Log.d(TAG, "成功复制模型文件 || " + assetPath);
        } catch (Exception e) {
            Log.e(TAG, "复制模型文件异常 || " + assetPath, e);
            // 判断残缺文件删除结果
            if (file.exists()) {
                boolean isDeleted = file.delete();
                if (!isDeleted) {
                    Log.w(TAG, "删除未完整写入的残缺文件失败 || " + file.getAbsolutePath());
                }
            }
            return "";
        }
        return file.getAbsolutePath();
    }
}