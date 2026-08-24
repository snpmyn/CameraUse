package com.qtone.camerause.widget.wechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qtone.camerause.util.datetime.CurrentTimeMillisClock;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.media.MediaScanKit;
import com.qtone.camerause.widget.storage.MediaStorageConfig;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc 微信裁剪引擎
 */
public class WeChatCropEngine {
    /**
     * 时间戳及序号正则表达式
     */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{10,13}(_\\d+)?");
    /**
     * 单例
     */
    private static volatile WeChatCropEngine instance;
    /**
     * 微信裁剪辅助者
     */
    private final WeChatCropHelper weChatCropHelper;
    /**
     * 增强实现
     */
    private final ExecutorService executorService;
    /**
     * 线程消息调度器
     */
    private final Handler handler;

    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     *
     * @param context 上下文
     */
    private WeChatCropEngine(@NotNull Context context) {
        // 使用 ApplicationContext 规避 Context 泄漏
        Context appContext = context.getApplicationContext();
        weChatCropHelper = new WeChatCropHelper();
        weChatCropHelper.init(appContext);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取单例
     *
     * @param context 上下文
     * @return 单例
     */
    public static WeChatCropEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (WeChatCropEngine.class) {
                if (instance == null) {
                    instance = new WeChatCropEngine(context);
                }
            }
        }
        return instance;
    }

    /**
     * 内部处理
     * <p>
     * 支持单图 / 多图
     * 处理 AI 定位、裁剪并进行 2 倍 SR 深度重建放大
     *
     * @param context              上下文
     * @param imagePath            图片路径
     * @param autoSaveResult       是否自动保存结果
     * @param enableSR             是否允许 SR
     *                             是否在裁剪后强制再做一次独立 2倍 SR 超分辨率重建
     * @param onWeChatCropCallback 微信裁剪回调
     */
    public void process(Context context, String imagePath, boolean autoSaveResult, boolean enableSR, OnWeChatCropCallback onWeChatCropCallback) {
        if ((imagePath == null) || imagePath.trim().isEmpty()) {
            notifyError(onWeChatCropCallback, "图片路径不能为空 - 微信裁剪");
            return;
        }
        File file = new File(imagePath);
        if (!file.exists() || !file.isFile()) {
            notifyError(onWeChatCropCallback, "找不到目标文件 + 请检查路径 - 微信裁剪 || " + imagePath);
            return;
        }
        if (executorService.isShutdown()) {
            notifyError(onWeChatCropCallback, "微信裁剪引擎已释放");
            return;
        }
        // 预先获取 ApplicationContext 安全保存
        // 规避 Context 泄漏
        final Context appContext = (context != null) ? context.getApplicationContext() : null;
        // 放到单线程池中排队做耗时的图像处理，保证频繁拍照时依次按序处理。
        executorService.execute(() -> {
            Mat srcMat = null;
            List<Mat> cropMatList = null;
            List<Mat> finalMatList = new ArrayList<>();
            List<Bitmap> resultBitmapList = new ArrayList<>();
            List<String> savePathList = new ArrayList<>();
            try {
                srcMat = Imgcodecs.imread(imagePath);
                if (srcMat.empty()) {
                    notifyError(onWeChatCropCallback, "OpenCV 读取图片失败 - 微信裁剪");
                    return;
                }
                // 1. AI 二维码 / 条形码多目标定位与透视矫正
                cropMatList = weChatCropHelper.detectAndCrop(srcMat);
                if (cropMatList.isEmpty()) {
                    notifyError(onWeChatCropCallback, "微信 AI 未在图中定位到可矫正的目标");
                    return;
                }
                // 2. 针对每一个定位到的目标分别做处理
                for (Mat cropMat : cropMatList) {
                    if (cropMat == null || cropMat.empty()) {
                        continue;
                    }
                    Mat processedMat = cropMat;
                    if (enableSR) {
                        Mat srMat = weChatCropHelper.superResolve(cropMat);
                        if ((srMat != null) && !srMat.empty()) {
                            processedMat = srMat;
                        } else {
                            Log.w(LogKit.TAG, "SR 重建失败 + 降级使用裁剪原图 - 微信裁剪");
                        }
                    }
                    finalMatList.add(processedMat);
                    // 3. 转 Bitmap 供 UI 展示
                    Bitmap bitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(processedMat, bitmap);
                    resultBitmapList.add(bitmap);
                }
                if (resultBitmapList.isEmpty()) {
                    notifyError(onWeChatCropCallback, "图像转换 Bitmap 失败 - 微信裁剪");
                    return;
                }
                // 4. 自动保存结果
                // 多图依次拼接 _1 _2 等后缀
                if (autoSaveResult && (appContext != null)) {
                    File mediaDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(MediaStorageConfig.StorageType.WE_CHAT_CROP);
                    if ((mediaDir != null) && !mediaDir.exists()) {
                        boolean created = mediaDir.mkdirs();
                        if (!created && !mediaDir.exists()) {
                            Log.e(LogKit.TAG, "微信裁剪图片保存目录创建失败 || " + mediaDir.getAbsolutePath());
                        }
                    }
                    if (mediaDir != null && mediaDir.exists()) {
                        String originalFileName = file.getName();
                        String timestampStr = null;
                        Matcher matcher = TIMESTAMP_PATTERN.matcher(originalFileName);
                        if (matcher.find()) {
                            timestampStr = matcher.group();
                        }
                        if ((timestampStr == null) || timestampStr.isEmpty()) {
                            timestampStr = String.valueOf(CurrentTimeMillisClock.getInstance().now());
                        }
                        for (int i = 0; i < finalMatList.size(); i++) {
                            // 1, 2, 3...
                            int index = i + 1;
                            File outputFile = new File(mediaDir, "WECHAT_CROP_" + timestampStr + "_" + index + ".jpg");
                            String savePath = outputFile.getAbsolutePath();
                            boolean saved = Imgcodecs.imwrite(savePath, finalMatList.get(i));
                            if (saved) {
                                MediaScanKit.scanSingleFile(appContext, savePath, "image/jpeg");
                                savePathList.add(savePath);
                            }
                        }
                    }
                }
                handler.post(() -> {
                    if (onWeChatCropCallback != null) {
                        onWeChatCropCallback.onWeChatCropSuccess(resultBitmapList, savePathList);
                    }
                });
            } catch (Exception e) {
                notifyError(onWeChatCropCallback, "图像处理异常 - 微信裁剪 || " + e.getMessage());
            } finally {
                if (srcMat != null) srcMat.release();
                if (cropMatList != null) {
                    for (Mat mat : cropMatList) {
                        if (mat != null) mat.release();
                    }
                }
                for (Mat mat : finalMatList) {
                    if ((mat != null) && ((cropMatList == null) || !cropMatList.contains(mat))) {
                        mat.release();
                    }
                }
            }
        });
    }

    /**
     * 通知错误
     *
     * @param onWeChatCropCallback 微信裁剪回调
     * @param errorMessage         错误消息
     */
    private void notifyError(OnWeChatCropCallback onWeChatCropCallback, String errorMessage) {
        handler.post(() -> {
            if (onWeChatCropCallback != null) {
                Log.e(LogKit.TAG, "微信裁剪错误 || " + errorMessage);
                onWeChatCropCallback.onWeChatCropError(errorMessage);
            }
        });
    }

    /**
     * 释放资源
     */
    public void release() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 微信裁剪回调
     */
    public interface OnWeChatCropCallback {
        /**
         * 微信裁剪成功
         *
         * @param resultBitmaps 结果像素数据集
         * @param savedPaths    保存路径集
         */
        void onWeChatCropSuccess(List<Bitmap> resultBitmaps, List<String> savedPaths);

        /**
         * 微信裁剪错误
         *
         * @param errorMessage 错误消息
         */
        void onWeChatCropError(String errorMessage);
    }
}