package com.qtone.camerause.function.wechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qtone.camerause.function.storage.MediaStorageConfig;
import com.qtone.camerause.utils.log.LogKit;
import com.qtone.camerause.utils.media.MediaScanKit;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc 微信裁剪引擎
 */
public class WeChatCropEngine {
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
     * 处理
     * <p>
     * 传入原图路径
     * 自动进行微信 AI 识别与透视校正裁剪
     *
     * @param context              上下文
     * @param imagePath            图片路径
     *                             原图绝对路径
     *                             如 /sdcard/Pictures/test.jpg
     * @param autoSaveResult       是否自动保存结果
     *                             是否将裁剪后的结果自动保存为文件
     * @param onWeChatCropCallback 微信裁剪回调
     */
    public void process(Context context, String imagePath, boolean autoSaveResult, OnWeChatCropCallback onWeChatCropCallback) {
        if ((imagePath == null) || imagePath.trim().isEmpty()) {
            notifyError(onWeChatCropCallback, "图片路径不能为空");
            return;
        }
        File file = new File(imagePath);
        if (!file.exists() || !file.isFile()) {
            notifyError(onWeChatCropCallback, "找不到目标文件 - 请检查路径 || " + imagePath);
            return;
        }
        // 预先获取 ApplicationContext 安全保存
        final Context appContext = (context != null) ? context.getApplicationContext() : null;
        // 放到单线程池中排队做耗时的图像处理，保证频繁拍照时依次按序处理。
        executorService.execute(() -> {
            Mat srcMat = null;
            Mat resultMat = null;
            try {
                // A. 使用 OpenCV 安全读取绝对路径图片
                srcMat = Imgcodecs.imread(imagePath);
                if (srcMat.empty()) {
                    notifyError(onWeChatCropCallback, "OpenCV 读取图片失败 (请检查是否有存储读取权限或路径中是否包含中文)");
                    return;
                }
                // B. 调用 WeChatCropHelper 进行 AI 定位与透视校正裁剪
                resultMat = weChatCropHelper.detectAndCrop(srcMat);
                if (resultMat == null || resultMat.empty()) {
                    notifyError(onWeChatCropCallback, "微信 AI 未在图中定位到可矫正的目标");
                    return;
                }
                // C. 将处理好的 Mat 转换为 Android 的 Bitmap 供 UI 展示
                Bitmap resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resultMat, resultBitmap);
                String savePath = null;
                // D. 如果开启了自动保存，将结果写入统一托管目录。
                if (autoSaveResult && (appContext != null)) {
                    File mediaDir = MediaStorageConfig.getInstance().getImageDirectoryFile();
                    if ((mediaDir != null) && !mediaDir.exists()) {
                        boolean isCreated = mediaDir.mkdirs();
                        if (!isCreated && !mediaDir.exists()) {
                            Log.w(LogKit.TAG, "创建微信裁剪图片保存目录失败");
                        }
                    }
                    if (mediaDir != null) {
                        File outputFile = new File(mediaDir, "WECHAT_CROP" + System.currentTimeMillis() + ".jpg");
                        savePath = outputFile.getAbsolutePath();
                        // 用 Imgcodecs 写入图片
                        boolean saved = Imgcodecs.imwrite(savePath, resultMat);
                        if (saved) {
                            Log.d(LogKit.TAG, "微信裁剪拉平结果已成功存入 || " + savePath);
                            MediaScanKit.scanSingleFile(context, savePath, "image/jpeg");
                        } else {
                            Log.e(LogKit.TAG, "微信裁剪图片 Imgcodecs 写入失败");
                            savePath = null;
                        }
                    } else {
                        Log.e(LogKit.TAG, "无法获取微信裁剪图片保存目录");
                    }
                }
                // E. 切换回主线程回调结果
                final String finalSavePath = savePath;
                handler.post(() -> {
                    if (onWeChatCropCallback != null) {
                        Log.d(LogKit.TAG, "微信裁剪成功 - 保存路径 || " + finalSavePath);
                        onWeChatCropCallback.onWeChatCropSuccess(resultBitmap, finalSavePath);
                    }
                });
            } catch (Exception e) {
                notifyError(onWeChatCropCallback, "图像处理异常 || " + e.getMessage());
            } finally {
                // F. 必须手动释放 C++ 底层 Mat 内存，防止内存泄漏爆发崩溃。
                if (srcMat != null) {
                    srcMat.release();
                }
                if (resultMat != null) {
                    resultMat.release();
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
     * 微信裁剪回调
     */
    public interface OnWeChatCropCallback {
        /**
         * 微信裁剪成功
         *
         * @param resultBitmap 结果像素数据
         * @param savedPath    保存路径
         */
        void onWeChatCropSuccess(Bitmap resultBitmap, String savedPath);

        /**
         * 微信裁剪错误
         *
         * @param errorMessage 错误消息
         */
        void onWeChatCropError(String errorMessage);
    }
}