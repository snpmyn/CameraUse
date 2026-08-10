//package com.qtone.camerause.kit;
//
//import android.util.Log;
//
//import com.jiangdg.ausbc.camera.bean.PreviewSize;
//import com.qtone.camerause.util.list.ListUtils;
//import com.qtone.camerause.util.log.LogKit;
//
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
///**
// * Created on 2026/8/8.
// *
// * @author 郑少鹏
// * @desc 预览尺寸配套原件
// */
//public class PreviewSizeKit {
//    /**
//     * 比例容差
//     * <p>
//     * 允许判定 0.01 内误差为相同宽高比
//     */
//    private static final double ASPECT_RATIO_TOLERANCE = 0.01;
//
//    /**
//     * constructor
//     * <p>
//     * 私有构造函数 + 防止实例化
//     */
//    private PreviewSizeKit() {
//
//    }
//
//    /**
//     * 选择最佳预览尺寸
//     *
//     * @param previewSizes  预览尺寸集
//     * @param targetWidth   目标宽
//     * @param targetHeight  目标高
//     * @param fallbackFirst 是否退而求其次
//     *                      true - 退而求其次
//     *                      false - 最大清晰度
//     * @return 最佳预览尺寸
//     */
//    public static @Nullable PreviewSize selectBestPreviewSize(List<PreviewSize> previewSizes, int targetWidth, int targetHeight, boolean fallbackFirst) {
//        if (ListUtils.listIsEmpty(previewSizes) || (targetWidth <= 0) || (targetHeight <= 0)) {
//            return null;
//        }
//        StringBuilder stringBuilder = new StringBuilder();
//        for (int i = 0; i < previewSizes.size(); i++) {
//            PreviewSize previewSize = previewSizes.get(i);
//            if (previewSize != null) {
//                stringBuilder.append("[").append(previewSize.getWidth()).append(" x ").append(previewSize.getHeight()).append("]");
//                if (i < (previewSizes.size() - 1)) {
//                    stringBuilder.append("\n");
//                }
//            }
//        }
//        Log.d(LogKit.TAG, "目标宽高 || " + targetWidth + " x " + targetHeight);
//        Log.d(LogKit.TAG, "预览尺寸集 - " + previewSizes.size() + " 个\n" + stringBuilder.toString());
//        // 统一按长边与短边归一化
//        // 避免横竖屏差异影响精准匹配
//        int targetMax = Math.max(targetWidth, targetHeight);
//        int targetMin = Math.min(targetWidth, targetHeight);
//        // A. 硬件支持的预览尺寸集中包含默认尺寸
//        // 直接返回 -> 不再计算
//        for (PreviewSize previewSize : previewSizes) {
//            if ((previewSize == null) || (previewSize.getWidth() <= 0) || (previewSize.getHeight() <= 0)) {
//                continue;
//            }
//            int previewSizeMax = Math.max(previewSize.getWidth(), previewSize.getHeight());
//            int previewSizeMin = Math.min(previewSize.getWidth(), previewSize.getHeight());
//            if ((previewSizeMax == targetMax) && (previewSizeMin == targetMin)) {
//                Log.i(LogKit.TAG, "硬件预览尺寸匹配成功 - 获取最佳分辨率 || " + previewSize.getWidth() + " x " + previewSize.getHeight());
//                return previewSize;
//            }
//        }
//        // B. 硬件支持的预览尺寸集中不包含默认尺寸
//        long targetPixelCount = (long) targetMax * targetMin;
//        double targetRatio = (double) targetMax / targetMin;
//        long maxPixelCount = -1;
//        long minPixelDiff = Long.MAX_VALUE;
//        double minRatioDiff = Double.MAX_VALUE;
//        PreviewSize bestPreviewSize = null;
//        for (PreviewSize previewSize : previewSizes) {
//            if ((previewSize == null) || (previewSize.getWidth() <= 0) || (previewSize.getHeight() <= 0)) {
//                continue;
//            }
//            int previewSizeMax = Math.max(previewSize.getWidth(), previewSize.getHeight());
//            int previewSizeMin = Math.min(previewSize.getWidth(), previewSize.getHeight());
//            // 计算像素量与长宽比
//            long pixelCount = (long) previewSizeMax * previewSizeMin;
//            double ratio = (double) previewSizeMax / previewSizeMin;
//            double ratioDiff = Math.abs(ratio - targetRatio);
//            long pixelDiff = Math.abs(pixelCount - targetPixelCount);
//            if (fallbackFirst) {
//                // 方案一 - 退而求其次
//                // 宽高比优先 + 同比例下最接近目标像素
//                if (bestPreviewSize == null) {
//                    bestPreviewSize = previewSize;
//                    minRatioDiff = ratioDiff;
//                    minPixelDiff = pixelDiff;
//                    continue;
//                }
//                boolean currentHasBetterRatio = (ratioDiff < (minRatioDiff - ASPECT_RATIO_TOLERANCE));
//                boolean isSameRatioCategory = (Math.abs(ratioDiff - minRatioDiff) <= ASPECT_RATIO_TOLERANCE);
//                if (currentHasBetterRatio) {
//                    // 找到了比例显著更优的尺寸
//                    // 重置比例基准与像素差基准
//                    minRatioDiff = ratioDiff;
//                    minPixelDiff = pixelDiff;
//                    bestPreviewSize = previewSize;
//                } else if (isSameRatioCategory) {
//                    // 同比例下最接近目标像素
//                    if (pixelDiff < minPixelDiff) {
//                        minPixelDiff = pixelDiff;
//                        bestPreviewSize = previewSize;
//                    } else if (pixelDiff == minPixelDiff) {
//                        // 偏差距离相同 + 优先向上兼容
//                        // 像素稍大 + 保证画质
//                        if (pixelCount > ((long) Math.max(bestPreviewSize.getWidth(), bestPreviewSize.getHeight()) * Math.min(bestPreviewSize.getWidth(), bestPreviewSize.getHeight()))) {
//                            bestPreviewSize = previewSize;
//                        }
//                    }
//                }
//            } else {
//                // 方案二 - 最大清晰度
//                // 清晰度优先
//                if (pixelCount > maxPixelCount) {
//                    maxPixelCount = pixelCount;
//                    minRatioDiff = ratioDiff;
//                    bestPreviewSize = previewSize;
//                } else if (pixelCount == maxPixelCount) {
//                    if (ratioDiff < minRatioDiff) {
//                        minRatioDiff = ratioDiff;
//                        bestPreviewSize = previewSize;
//                    }
//                }
//            }
//        }
//        // 上述筛选失效 -> 降级默取头个
//        if (bestPreviewSize == null) {
//            for (PreviewSize previewSize : previewSizes) {
//                if ((previewSize != null) && (previewSize.getWidth() > 0) && (previewSize.getHeight() > 0)) {
//                    bestPreviewSize = previewSize;
//                    break;
//                }
//            }
//            if ((bestPreviewSize == null) && !previewSizes.isEmpty()) {
//                bestPreviewSize = previewSizes.get(0);
//            }
//            Log.w(LogKit.TAG, "筛选失效 + 默取头个 || " + ((bestPreviewSize != null) ? (bestPreviewSize.getWidth() + " x " + bestPreviewSize.getHeight()) : "null"));
//        } else {
//            Log.i(LogKit.TAG, "硬件预览尺寸匹配成功 - 获取最佳分辨率 || " + bestPreviewSize.getWidth() + " x " + bestPreviewSize.getHeight());
//        }
//        return bestPreviewSize;
//    }
//}