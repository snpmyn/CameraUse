package com.qtone.camerause.kit.asset;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qtone.camerause.kit.log.LogKit;

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
    /**
     * 拷贝 Asset 文件到缓存
     *
     * @param context          上下文
     * @param subdirectoryName 子目录名
     *                         需根目录传 null 或 ""
     * @param fileName         文件名
     * @return 物理绝对路径 [拷贝失败或目录创建失败返 ""]
     */
    @NotNull
    public static String copyAssetFileToCache(@NotNull Context context, String subdirectoryName, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e(LogKit.TAG, "文件名不能为空");
            return "";
        }
        // subdirectoryName 为 null 或 "" 时直接使用 getFilesDir() 根目录
        boolean hasDir = (subdirectoryName != null) && !subdirectoryName.trim().isEmpty();
        File dir = hasDir ? new File(context.getFilesDir(), subdirectoryName.trim()) : context.getFilesDir();
        // 判断目录创建结果
        if (!dir.exists()) {
            boolean isCreated = dir.mkdirs();
            if (!isCreated && !dir.exists()) {
                Log.e(LogKit.TAG, "创建目标文件夹失败 || " + dir.getAbsolutePath());
                return "";
            }
        }
        File file = new File(dir, fileName);
        // 如果文件已存在且大小正常，不重复复制，直接返回路径。
        if (file.exists() && (file.length() > 0)) {
            return file.getAbsolutePath();
        }
        // subdirectoryName 为 null 或 "" 时直接读取 assets 根目录
        String assetPath = hasDir ? (subdirectoryName.trim() + "/" + fileName) : fileName;
        try (InputStream inputStream = context.getAssets().open(assetPath);
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[2048];
            int byteCount;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();
            Log.d(LogKit.TAG, "复制文件成功 || " + assetPath);
        } catch (Exception e) {
            Log.e(LogKit.TAG, "复制文件异常 || " + assetPath, e);
            // 判断未完整写入的残缺文件删除结果
            if (file.exists()) {
                boolean isDeleted = file.delete();
                if (!isDeleted) {
                    Log.w(LogKit.TAG, "删除未完整写入的残缺文件失败 || " + file.getAbsolutePath());
                }
            }
            return "";
        }
        return file.getAbsolutePath();
    }
}