package com.qtone.camerause.util.file;

import android.graphics.BitmapFactory;
import android.text.TextUtils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Created on 2026/8/24.
 *
 * @author 郑少鹏
 * @desc 文件配套原件
 */
public class FileKit {
    /**
     * 每英寸毫米
     * <p>
     * 1 英寸（inch）等于多少毫米（mm）
     */
    private static final double MM_PER_INCH = 25.4;

    /**
     * 获取图像尺寸
     * <p>
     * 综合公式
     * 毫米 = 像素 * 25.4 / DPI
     * <p>
     * 换算原理
     * 步骤一 物理长度 (英寸) = 像素宽度 / DPI
     * 步骤二 物理长度 (毫米) = 物理长度 (英寸) * 25.4 (即 MM_PER_INCH)
     * <p>
     * 单位说明
     * - 25.4
     * 单位换算常数 1 inch = 25.4 mm
     * - DPI (Dots Per Inch)
     * 扫描设备或屏幕的采样密度 (每英寸包含的像素点数)
     * - 像素尺寸 (px)
     * 图片的像素点数 (通过 BitmapFactory.Options.outWidth / outHeight 获取)
     *
     * @param path 路径
     * @param dpi  屏幕密度
     * @return 图像尺寸
     */
    @Contract("_, _ -> new")
    public static double @NotNull [] getImageSize(String path, int dpi) {
        if (TextUtils.isEmpty(path) || (dpi <= 0)) {
            return new double[]{0, 0};
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if ((options.outWidth <= 0) || (options.outHeight <= 0)) {
            return new double[]{0, 0};
        }
        double widthMm = Math.round(options.outWidth * MM_PER_INCH / dpi * 100) / 100.0;
        double heightMm = Math.round(options.outHeight * MM_PER_INCH / dpi * 100) / 100.0;
        return new double[]{widthMm, heightMm};
    }

    /**
     * 格式化文件大小
     *
     * @param sizeInBytes 字节大小
     * @return 格式化后文件大小
     */
    public static @NotNull String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "0 KB";
        }
        double sizeInKb = sizeInBytes / 1024.0;
        if (sizeInKb < 1024.0) {
            return String.format(Locale.getDefault(), "%.2f KB", sizeInKb);
        } else {
            double sizeInMb = sizeInKb / 1024.0;
            return String.format(Locale.getDefault(), "%.2f MB", sizeInMb);
        }
    }
}