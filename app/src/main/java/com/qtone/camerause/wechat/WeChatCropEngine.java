package com.qtone.camerause.wechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
    private static final String TAG = WeChatCropEngine.class.getSimpleName();
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
     * 私有构造函数 + 防止外部直接 new
     *
     * @param context 上下文
     */
    private WeChatCropEngine(@NotNull Context context) {
        // 使用 ApplicationContext 防止 Context 泄漏
        Context appContext = context.getApplicationContext();
        weChatCropHelper = new WeChatCropHelper();
        weChatCropHelper.init(appContext);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取单例实例
     *
     * @param context 上下文
     * @return WeChatCropEngine 单例
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
     * 过程
     * <p>
     * 传入原图路径
     * 自动进行微信 AI 识别与透视校正裁剪
     *
     * @param context              上下文
     * @param imagePath            图像路径
     *                             原图绝对路径
     *                             如 /sdcard/Pictures/test.jpg
     * @param autoSaveResult       是否自动保存结果
     *                             是否将裁剪后的结果自动保存为文件
     * @param onWeChatCropListener 微信裁剪监听
     */
    public void process(Context context, String imagePath, boolean autoSaveResult, OnWeChatCropListener onWeChatCropListener) {
        if ((imagePath == null) || imagePath.trim().isEmpty()) {
            notifyError(onWeChatCropListener, "图片路径不能为空");
            return;
        }
        File file = new File(imagePath);
        if (!file.exists() || !file.isFile()) {
            notifyError(onWeChatCropListener, "找不到目标文件，请检查路径: " + imagePath);
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
                    notifyError(onWeChatCropListener, "OpenCV 读取图片失败 (请检查是否有存储读取权限或路径中是否包含中文)");
                    return;
                }
                // B. 调用 WeChatCropHelper 进行 AI 定位与透视校正裁剪
                resultMat = weChatCropHelper.detectAndCrop(srcMat);
                if (resultMat == null || resultMat.empty()) {
                    notifyError(onWeChatCropListener, "微信 AI 未在图中定位到可矫正的目标");
                    return;
                }
                // C. 将处理好的 Mat 转换为 Android 的 Bitmap 供 UI 展示
                Bitmap resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resultMat, resultBitmap);
                String savePath = null;
                // D. 如果开启了自动保存，将结果写入 app 的 cache/Pictures 目录。
                if (autoSaveResult && appContext != null) {
                    File outputDir = new File(appContext.getExternalFilesDir(null), "CroppedImages");
                    if (!outputDir.exists() && !outputDir.mkdirs()) {
                        Log.e(TAG, "创建输出目录失败: " + outputDir.getAbsolutePath());
                    }
                    File outputFile = new File(outputDir, "CROP_" + System.currentTimeMillis() + ".jpg");
                    savePath = outputFile.getAbsolutePath();
                    // 用 Imgcodecs 写入图片
                    Imgcodecs.imwrite(savePath, resultMat);
                    Log.d(TAG, "裁剪拉平结果已成功存入 || " + savePath);
                }
                // E. 切换回主线程回调结果
                final String finalSavePath = savePath;
                handler.post(() -> {
                    if (onWeChatCropListener != null) {
                        onWeChatCropListener.onSuccess(resultBitmap, finalSavePath);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "图像处理过程发生异常", e);
                notifyError(onWeChatCropListener, "图像处理异常: " + e.getMessage());
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
     * @param onWeChatCropListener 微信裁剪监听
     * @param errorMsg             错误消息
     */
    private void notifyError(OnWeChatCropListener onWeChatCropListener, String errorMsg) {
        Log.e(TAG, errorMsg);
        handler.post(() -> {
            if (onWeChatCropListener != null) {
                onWeChatCropListener.onError(errorMsg);
            }
        });
    }

    /**
     * 微信裁剪监听
     */
    public interface OnWeChatCropListener {
        /**
         * 成功
         *
         * @param resultBitmap 结果像素数据
         * @param savedPath    保存路径
         */
        void onSuccess(Bitmap resultBitmap, String savedPath);

        /**
         * 错误
         *
         * @param errorMessage 错误消息
         */
        void onError(String errorMessage);
    }
}