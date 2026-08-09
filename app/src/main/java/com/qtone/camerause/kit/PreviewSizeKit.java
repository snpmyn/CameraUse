package com.qtone.camerause.kit;

import android.util.Log;

import com.jiangdg.ausbc.camera.bean.PreviewSize;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 预览尺寸配套原件
 */
public class PreviewSizeKit {
    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     */
    private PreviewSizeKit() {

    }

    /**
     * 选择最佳预览尺寸
     *
     * @param previewSizes 预览尺寸集
     * @param targetWidth  目标宽
     * @param targetHeight 目标高
     * @param deviceModel  设备标识
     * @return 最佳预览尺寸
     */
    public static @Nullable PreviewSize selectBestPreviewSize(List<PreviewSize> previewSizes, int targetWidth, int targetHeight, String deviceModel) {
        if (ListUtils.listIsEmpty(previewSizes) || (targetWidth <= 0) || (targetHeight <= 0)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < previewSizes.size(); i++) {
            PreviewSize previewSize = previewSizes.get(i);
            if (previewSize != null) {
                stringBuilder.append("[").append(previewSize.getWidth()).append(" x ").append(previewSize.getHeight()).append("]");
                if (i < (previewSizes.size() - 1)) {
                    stringBuilder.append("\n");
                }
            }
        }
        Log.d(LogKit.TAG, "目标宽高 || " + targetWidth + " x " + targetHeight);
        Log.d(LogKit.TAG, "预览尺寸集 - " + previewSizes.size() + " 个\n" + stringBuilder.toString());
        // 统一按长边与短边归一化
        // 避免横竖屏差异影响精准匹配
        int targetMax = Math.max(targetWidth, targetHeight);
        int targetMin = Math.min(targetWidth, targetHeight);
        // A. 硬件支持的预览尺寸集中包含默认尺寸
        // 直接返回 -> 不再计算
        for (PreviewSize previewSize : previewSizes) {
            if ((previewSize == null) || (previewSize.getWidth() <= 0) || (previewSize.getHeight() <= 0)) {
                continue;
            }
            if (DeviceBlacklist.isBlacklisted(deviceModel, previewSize.getWidth(), previewSize.getHeight())) {
                Log.w(LogKit.TAG, "命中机型 [" + deviceModel + "] 黑名单分辨率 - 跳过 || " + previewSize.getWidth() + " x " + previewSize.getHeight());
                continue;
            }
            int previewSizeMax = Math.max(previewSize.getWidth(), previewSize.getHeight());
            int previewSizeMin = Math.min(previewSize.getWidth(), previewSize.getHeight());
            if ((previewSizeMax == targetMax) && (previewSizeMin == targetMin)) {
                Log.i(LogKit.TAG, "硬件预览尺寸匹配成功 - 获取最佳分辨率 || " + previewSize.getWidth() + " x " + previewSize.getHeight());
                return previewSize;
            }
        }
        // B. 硬件支持的预览尺寸集中不包含默认尺寸
        // 进行计算 -> 高清晰度优先
        long maxPixelCount = -1;
        double minRatioDiff = Double.MAX_VALUE;
        double targetRatio = (double) targetMax / targetMin;
        PreviewSize bestPreviewSize = null;
        for (PreviewSize previewSize : previewSizes) {
            if ((previewSize == null) || (previewSize.getWidth() <= 0) || (previewSize.getHeight() <= 0)) {
                continue;
            }
            if (DeviceBlacklist.isBlacklisted(deviceModel, previewSize.getWidth(), previewSize.getHeight())) {
                Log.w(LogKit.TAG, "命中机型 [" + deviceModel + "] 黑名单分辨率 - 跳过 || " + previewSize.getWidth() + " x " + previewSize.getHeight());
                continue;
            }
            int previewSizeMax = Math.max(previewSize.getWidth(), previewSize.getHeight());
            int previewSizeMin = Math.min(previewSize.getWidth(), previewSize.getHeight());
            // 计算像素量
            // 计算长宽比
            long pixelCount = (long) previewSizeMax * previewSizeMin;
            double ratio = (double) previewSizeMax / previewSizeMin;
            double ratioDiff = Math.abs(ratio - targetRatio);
            // 1. 清晰度更高
            // 直接胜出
            if (pixelCount > maxPixelCount) {
                maxPixelCount = pixelCount;
                minRatioDiff = ratioDiff;
                bestPreviewSize = previewSize;
            }
            // 2. 清晰度相同
            // 比较长宽比
            // 选择比例更接近目标尺寸的分辨率
            else if (pixelCount == maxPixelCount) {
                if (ratioDiff < minRatioDiff) {
                    minRatioDiff = ratioDiff;
                    bestPreviewSize = previewSize;
                }
            }
        }
        // 上述筛选失效 -> 降级默取头个
        if (bestPreviewSize == null) {
            for (PreviewSize previewSize : previewSizes) {
                if ((previewSize != null) && !DeviceBlacklist.isBlacklisted(deviceModel, previewSize.getWidth(), previewSize.getHeight())) {
                    bestPreviewSize = previewSize;
                    break;
                }
            }
            if ((bestPreviewSize == null) && !previewSizes.isEmpty()) {
                bestPreviewSize = previewSizes.get(0);
            }
            Log.w(LogKit.TAG, "筛选失效 + 默取头个 || " + ((bestPreviewSize != null) ? (bestPreviewSize.getWidth() + " x " + bestPreviewSize.getHeight()) : "null"));
        } else {
            Log.i(LogKit.TAG, "硬件预览尺寸匹配成功 - 获取最佳分辨率 || " + bestPreviewSize.getWidth() + " x " + bestPreviewSize.getHeight());
        }
        return bestPreviewSize;
    }

    /**
     * 设备黑名单
     */
    public enum DeviceBlacklist {
        /**
         * 荣耀 Magic6
         */
        HONOR_60("荣耀 Magic6", Collections.singletonList(new AspectSize(1280, 720)));
        /**
         * 设备标识
         */
        private final String deviceModel;
        /**
         * 设备黑名单
         */
        private final List<AspectSize> deviceBlacklist;

        /**
         * constructor
         *
         * @param deviceModel     设备标识
         * @param deviceBlacklist 设备黑名单
         */
        DeviceBlacklist(String deviceModel, List<AspectSize> deviceBlacklist) {
            this.deviceModel = deviceModel;
            this.deviceBlacklist = deviceBlacklist;
        }

        /**
         * 是否属于黑名单
         *
         * @param deviceModel 设备标识
         * @param width       宽
         * @param height      高
         * @return 是否属于黑名单
         */
        public static boolean isBlacklisted(String deviceModel, int width, int height) {
            if ((deviceModel == null) || deviceModel.trim().isEmpty()) {
                return false;
            }
            for (DeviceBlacklist deviceBlacklist : values()) {
                if (deviceBlacklist.getDeviceModel().equalsIgnoreCase(deviceModel.trim())) {
                    for (AspectSize aspectSize : deviceBlacklist.getDeviceBlacklist()) {
                        if (aspectSize.match(width, height)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * 获取设备标识
         *
         * @return 设备标识
         */
        public String getDeviceModel() {
            return deviceModel;
        }

        /**
         * 获取设备黑名单
         *
         * @return 设备黑名单
         */
        public List<AspectSize> getDeviceBlacklist() {
            return deviceBlacklist;
        }
    }

    /**
     * 比例尺寸
     */
    public static class AspectSize {
        /**
         * 宽
         */
        private final int width;
        /**
         * 高
         */
        private final int height;

        /**
         * constructor
         *
         * @param width  宽
         * @param height 高
         */
        public AspectSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * 是否匹配
         *
         * @param width  宽
         * @param height 高
         * @return 是否匹配
         */
        public boolean match(int width, int height) {
            return ((this.width == width) && (this.height == height)) || ((this.width == height) && (this.height == width));
        }
    }
}