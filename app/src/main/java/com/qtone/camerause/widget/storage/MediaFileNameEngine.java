package com.qtone.camerause.widget.storage;

import android.text.TextUtils;

import com.qtone.camerause.util.datetime.CurrentTimeMillisClock;

import org.jetbrains.annotations.NotNull;

import java.io.File;
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
     * 序号
     * <p>
     * 原子自增 + 规避同毫秒生成文件名冲突覆盖
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
     * 格式 {Timestamp}_{Seq}
     * 例如 1754294400000_0001
     *
     * @return 根节点 KEY
     */
    public static @NotNull String generateRootKey() {
        return String.format(Locale.CHINA, "%d_%04d", CurrentTimeMillisClock.getInstance().now(), sequence.incrementAndGet());
    }

    /**
     * 解析或生成根节点 KEY
     *
     * @param sourcePathOrName 资源路径或名称
     *                         如 IMG_1754294400000_0001.jpg
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
            if (parts.length >= 3) {
                // 1. 派生格式 PREFIX_TIMESTAMP_SEQ_SUB_INDEX
                // 2. 母图格式 IMG_TIMESTAMP_SEQ
                return (parts[1] + "_" + parts[2]);
            } else if ((parts.length == 2) && (parts[0].length() <= 4)) {
                return parts[1];
            }
        }
        return generateRootKey();
    }

    /**
     * 生成文件名
     * <p>
     * 拍照原图
     * IMG_1754294400000_0001.jpg
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
            // IMG_1754294400000_0001.jpg
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