package com.qtone.camerause.capture;

import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.OcrResponseResult;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.crop.DocumentCropProcessor;
import com.qtone.camerause.ocr.BaiDuOcrHelper;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.wechat.WeChatCropEngine;

/**
 * Created on 2026/8/6.
 *
 * @author 郑少鹏
 * @desc 拍照页配套原件
 */
public class CaptureActivityKit implements CaptureProcessor.OnCaptureCallBack, DocumentCropProcessor.OnExamCropCallback {
    /**
     * 拍照页
     */
    private final CaptureActivity captureActivity;
    /**
     * 拍照处理器
     */
    private CaptureProcessor captureProcessor;
    /**
     * 文档裁剪处理器
     */
    private DocumentCropProcessor documentCropProcessor;

    /**
     * constructor
     *
     * @param captureActivity 拍照页
     */
    public CaptureActivityKit(CaptureActivity captureActivity) {
        this.captureActivity = captureActivity;
        this.captureProcessor = new CaptureProcessor();
        this.documentCropProcessor = new DocumentCropProcessor();
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
     * 获取文档裁剪处理器
     *
     * @return 文档裁剪处理器
     */
    public DocumentCropProcessor getExamCropProcessor() {
        return documentCropProcessor;
    }

    /**
     * 释放
     */
    public void release() {
        if (captureProcessor != null) {
            captureProcessor.release();
            captureProcessor = null;
        }
        if (documentCropProcessor != null) {
            documentCropProcessor.destroy();
            documentCropProcessor = null;
        }
    }

    /**
     * 拍照开始
     */
    @Override
    public void onCaptureBegin() {
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
        if (documentCropProcessor != null) {
            try {
                // 文档裁剪处理器 - 异步处理 NV21
                documentCropProcessor.processNv21Async(captureActivity, nv21Data, width, height, CaptureActivityKit.this);
            } catch (Exception e) {
                Log.e(LogKit.TAG, "processNv21Async 失败 || " + e.getMessage());
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
        ToastUtils.show("拍照成功");
        if (documentCropProcessor != null) {
            try {
                // 1. 文档裁剪处理器 - 异步处理
                documentCropProcessor.processAsync(captureActivity, savePath, CaptureActivityKit.this);
            } catch (Exception e) {
                Log.e(LogKit.TAG, "processAsync 失败 || " + e.getMessage());
            }
        }
        // 2. 通用文字识别 (高精度含位置信息版)
        BaiDuOcrHelper.recognizeAccurate(captureActivity, savePath, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult generalResult) {
                Log.e(LogKit.TAG, "通用文字识别 (高精度含位置信息版) 结果\n" + generalResult);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.e(LogKit.TAG, "通用文字识别 (高精度含位置信息版) 错误\n" + ocrError.getMessage());
            }
        }, false);
        // 2. 通用文字识别 (含生僻字版)
        BaiDuOcrHelper.recognizeGeneralEnhanced(captureActivity, savePath, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult generalResult) {
                Log.e(LogKit.TAG, "通用文字识别 (含生僻字版) 结果\n" + generalResult);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.e(LogKit.TAG, "通用文字识别 (含生僻字版) 错误\n" + ocrError.getMessage());
            }
        }, false);
        // 2. 试卷分析与识别
        BaiDuOcrHelper.recognizeExampleDoc(captureActivity, savePath, new OnResultListener<OcrResponseResult>() {
            @Override
            public void onResult(OcrResponseResult ocrResponseResult) {
                Log.e(LogKit.TAG, "试卷分析与识别结果\n" + ocrResponseResult);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.e(LogKit.TAG, "试卷分析与识别错误\n" + ocrError.getMessage());
            }
        }, false);
        // 2. 手写文字识别
        BaiDuOcrHelper.recoginzeWrittenText(captureActivity, savePath, new OnResultListener<OcrResponseResult>() {
            @Override
            public void onResult(OcrResponseResult ocrResponseResult) {
                Log.e(LogKit.TAG, "手写文字识别结果\n" + ocrResponseResult);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.e(LogKit.TAG, "手写文字识别错误\n" + ocrError.getMessage());
            }
        }, false);
        // 3. 微信裁剪引擎 - 处理
        WeChatCropEngine.getInstance(captureActivity).process(captureActivity, savePath, true, new WeChatCropEngine.OnWeChatCropListener() {
            @Override
            public void onWeChatCropSuccess(Bitmap resultBitmap, String savedPath) {

            }

            @Override
            public void onWeChatCropError(String errorMessage) {

            }
        });
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
        ToastUtils.show("试卷矫正裁剪成功");
    }

    /**
     * 试卷裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onExamCropError(String errorMsg) {
        ToastUtils.show("试卷矫正失败");
    }
}