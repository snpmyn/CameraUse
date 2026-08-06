package com.qtone.camerause.capture;

import android.graphics.Bitmap;
import android.util.Log;

import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.crop.ExamCropProcessor;

/**
 * Created on 2026/8/6.
 *
 * @author 郑少鹏
 * @desc 拍照页配套原件
 */
public class CaptureActivityKit implements CaptureProcessor.OnCaptureCallBack, ExamCropProcessor.OnExamCropCallback {
    private static final String TAG = CaptureActivityKit.class.getSimpleName();
    /**
     * 拍照页
     */
    private final CaptureActivity captureActivity;
    /**
     * 拍照处理器
     */
    private CaptureProcessor captureProcessor;
    /**
     * 试卷裁剪处理器
     */
    private ExamCropProcessor examCropProcessor;

    /**
     * constructor
     *
     * @param captureActivity 拍照页
     */
    public CaptureActivityKit(CaptureActivity captureActivity) {
        this.captureActivity = captureActivity;
        this.captureProcessor = new CaptureProcessor();
        this.examCropProcessor = new ExamCropProcessor();
    }

    /**
     * 获取拍照处理器
     *
     * @return 拍照处理器
     */
    public CaptureProcessor getCaptureProcessor() {
        return captureProcessor;
    }

    /**
     * 释放
     */
    public void release() {
        if (captureProcessor != null) {
            captureProcessor.release();
            captureProcessor = null;
        }
        if (examCropProcessor != null) {
            examCropProcessor.destroy();
            examCropProcessor = null;
        }
    }

    /**
     * 拍照开始
     */
    @Override
    public void onCaptureBegin() {
        Log.d(TAG, "拍照开始");
        ToastUtils.show("拍照开始");
    }

    /**
     * 拍照 NV21
     * <p>
     * 成功捕获并隔离 NV21 原始数据帧时回调
     *
     * @param nv21Data    深拷贝后的 NV21 字节数据
     * @param width       帧物理宽
     * @param height      帧物理高
     * @param captureMode 拍照模式
     */
    @Override
    public void onCaptureNv21(byte[] nv21Data, int width, int height, CaptureMode captureMode) {
        // 页面已销毁或正在销毁时终止回调
        if (captureActivity.isFinishing() || captureActivity.isDestroyed()) {
            return;
        }
        if (examCropProcessor != null) {
            try {
                examCropProcessor.processNv21Async(captureActivity, nv21Data, width, height, CaptureActivityKit.this);
            } catch (Exception e) {
                Log.e(TAG, "processNv21Async 失败 || " + e.getMessage());
            }
        }
    }

    /**
     * 拍照成功
     *
     * @param savePath    保存路径
     * @param width       帧物理宽
     * @param height      帧物理高
     * @param captureMode 拍照模式
     */
    @Override
    public void onCaptureSuccess(String savePath, int width, int height, CaptureMode captureMode) {
        // 页面已销毁或正在销毁时终止回调
        if (captureActivity.isFinishing() || captureActivity.isDestroyed()) {
            return;
        }
        Log.d(TAG, "物理 1:1 无损图片生成成功\n模式 " + captureMode.name() + "\n分辨率 " + width + "x" + height + "\n路径 " + savePath);
        ToastUtils.show("拍照成功");
        if (examCropProcessor != null) {
            try {
                examCropProcessor.processAsync(captureActivity, savePath, CaptureActivityKit.this);
            } catch (Exception e) {
                Log.e(TAG, "processAsync 失败 || " + e.getMessage());
            }
        }
    }

    /**
     * 拍照错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onCaptureError(String errorMsg) {
        // 页面已销毁或正在销毁时终止回调
        if (captureActivity.isFinishing() || captureActivity.isDestroyed()) {
            return;
        }
        Log.e(TAG, "拍照错误 || " + errorMsg);
        ToastUtils.show("拍照错误");
    }

    /**
     * 试卷裁剪成功
     *
     * @param croppedPath  已拷贝路径
     * @param resultBitmap 结果像素数据
     */
    @Override
    public void onExamCropSuccess(String croppedPath, Bitmap resultBitmap) {

    }

    /**
     * 试卷裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onExamCropError(String errorMsg) {

    }
}