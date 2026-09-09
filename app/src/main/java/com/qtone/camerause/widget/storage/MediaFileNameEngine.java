package com.qtone.camerause.widget.storage;

import android.text.TextUtils;

import com.qtone.camerause.util.datetime.CurrentTimeMillisClock;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 媒体文件名引擎
 */
public class MediaFileNameEngine {
    /**
     * 时间模式
     */
    private static final String TIME_PATTERN = "yyyyMMdd_HHmmss";
    /**
     * 序号
     * <p>
     * 原子自增 + 规避同毫秒/同秒生成文件名冲突覆盖
     */
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * 重置序号
     */
    public static void resetSequence() {
        sequence.set(0);
    }

    /**
     * 生成根节点 KEY
     * <p>
     * 格式 {yyyyMMdd_HHmmss}_{Seq}
     * 例如 20260904_182251_0001
     *
     * @return 根节点 KEY
     */
    public static @NotNull String generateRootKey() {
        long nowMillis = CurrentTimeMillisClock.getInstance().now();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIME_PATTERN, Locale.CHINA);
        String timeStr = simpleDateFormat.format(new Date(nowMillis));
        return String.format(Locale.CHINA, "%s_%04d", timeStr, sequence.incrementAndGet());
    }

    /**
     * 解析或生成根节点 KEY
     *
     * @param sourcePathOrName 资源路径或名称
     *                         如 IMG_20260904_182251_0001.jpg
     * @return 解析或生成后根节点 KEY [无法识别则自动退化生成新根节点 KEY]
     */
    public static @NotNull String parseOrGenerateRootKey(String sourcePathOrName) {
        if (!TextUtils.isEmpty(sourcePathOrName)) {
            String fileName = new File(sourcePathOrName).getName();
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                fileName = fileName.substring(0, dotIndex);
            }
            String[] parts = fileName.split("_");
            // 时间格式本身包含一个下划线 (yyyyMMdd_HHmmss) 导致 parts 会被切分成多段
            // 1. 带前缀母图：IMG_20260904_182251_0001 -> [IMG, 20260904, 182251, 0001] (len = 4)
            // 2. 带前缀派生图：CROP_20260904_182251_0001_01 -> [CROP, 20260904, 182251, 0001, 01] (len = 5)
            // 3. 无前缀根节点：20260904_182251_0001 -> [20260904, 182251, 0001] (len = 3)
            if (parts.length >= 4) {
                // 对应场景 1 和 2
                // 取第 1、2、3 段拼接成 yyyyMMdd_HHmmss_Seq
                return parts[1] + "_" + parts[2] + "_" + parts[3];
            } else if (parts.length == 3) {
                // 对应场景 3
                // 无前缀时直接拼接三段
                return parts[0] + "_" + parts[1] + "_" + parts[2];
            }
        }
        return generateRootKey();
    }

    /**
     * 生成文件名
     * <p>
     * 拍照原图
     * IMG_20260904_182251_0001.jpg
     * <p>
     * 派生场景
     * 默认追加序号 _01
     * 多张处理 _01, _02...
     *
     * @param mediaStorageType 媒体存储类型
     * @param sourcePath       资源路径
     * @param subIndex         子下标
     * @return 文件名
     */
    public static String generateFileName(MediaStorageType mediaStorageType, String sourcePath, int subIndex) {
        MediaStorageType type = (mediaStorageType != null) ? mediaStorageType : MediaStorageType.CAPTURE;
        String prefix = type.getFileNamePrefix();
        String rootKey = parseOrGenerateRootKey(sourcePath);
        if (type == MediaStorageType.CAPTURE) {
            // 拍照原图
            // 不加子序号后缀
            // IMG_20260904_182251_0001.jpg
            return String.format(Locale.CHINA, "%s_%s.jpg", prefix, rootKey);
        } else {
            // 派生场景
            // 统加子序号后缀
            // 传值 <= 0 默取 1 (_01)
            int index = Math.max(1, subIndex);
            return String.format(Locale.CHINA, "%s_%s_%02d.jpg", prefix, rootKey, index);
        }
    }
}