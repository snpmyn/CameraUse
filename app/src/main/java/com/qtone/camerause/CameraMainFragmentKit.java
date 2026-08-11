package com.qtone.camerause;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.OcrResponseResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.bean.PreviewSize;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.capture.CaptureMode;
import com.qtone.camerause.capture.CaptureProcessor;
import com.qtone.camerause.capture.CaptureStrategy;
import com.qtone.camerause.crop.DocumentCropProcessor;
import com.qtone.camerause.ocr.BaiDuOcrHelper;
import com.qtone.camerause.scancode.ScanCodeProcessor;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.wechat.WeChatCropEngine;

import java.util.List;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机主碎片配套原件
 */
public class CameraMainFragmentKit implements CaptureProcessor.OnCaptureCallBack, DocumentCropProcessor.OnDocumentCropCallback, ScanCodeProcessor.OnScanCodeListener {
    /**
     * 相机主碎片
     */
    private final CameraMainFragment cameraMainFragment;
    /**
     * 拍照处理器
     */
    private final CaptureProcessor captureProcessor;
    /**
     * 文档裁剪处理器
     */
    private final DocumentCropProcessor documentCropProcessor;
    /**
     * 扫码处理器
     */
    private final ScanCodeProcessor scanCodeProcessor;

    /**
     * constructor
     *
     * @param cameraMainFragment 相机主碎片
     */
    public CameraMainFragmentKit(CameraMainFragment cameraMainFragment) {
        // 相机主碎片
        this.cameraMainFragment = cameraMainFragment;
        // 拍照处理器
        this.captureProcessor = new CaptureProcessor();
        // 文档裁剪处理器
        this.documentCropProcessor = new DocumentCropProcessor();
        // 扫码处理器
        this.scanCodeProcessor = new ScanCodeProcessor(this);
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
     * 预览帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        captureProcessor.onPreviewFrame(data, width, height, dataFormat, this);

        scanCodeProcessor.processFrame(data, width, height, dataFormat, 0);
    }

    /**
     * 单拍按钮点击事件
     */
    public void onSingleCaptureClicked() {
        captureProcessor.setCaptureStrategy(CaptureStrategy.NV21_FRAME);
        captureProcessor.startSingleCapture(cameraMainFragment.requireActivity(), cameraMainFragment.getCurrentCamera(), this);
    }

    /**
     * 连拍按钮点击事件
     *
     * @param intervalMs 连拍时间间隔
     *                   单位 - 毫秒
     */
    public void onBurstCaptureClicked(long intervalMs) {
        captureProcessor.startBurstCapture(cameraMainFragment.requireActivity(), cameraMainFragment.getCurrentCamera(), intervalMs, this);
    }

    /**
     * 停止连拍按钮点击事件
     */
    public void onStopBurstCaptureClicked() {
        captureProcessor.stopBurstCapture();
    }

    /**
     * 切换分辨率按钮点击事件
     */
    public void onSwitchResolutionClicked() {
        List<PreviewSize> previewSizes = cameraMainFragment.getAllPreviewSizes(null);
        if (ListUtils.listIsEmpty(previewSizes)) {
            ToastUtils.show("获取预览分辨率失败");
            return;
        }
        int selectedIndex = -1;
        String[] items = new String[previewSizes.size()];
        PreviewSize currentPreviewSize = cameraMainFragment.getCurrentPreviewSize();
        for (int i = 0; i < previewSizes.size(); i++) {
            PreviewSize previewSize = previewSizes.get(i);
            int previewSizeWidth = previewSize.getWidth();
            int previewSizeHeight = previewSize.getHeight();
            if ((currentPreviewSize != null) && (currentPreviewSize.getWidth() == previewSizeWidth) && (currentPreviewSize.getHeight() == previewSizeHeight)) {
                selectedIndex = i;
            }
            items[i] = (previewSizeWidth + " x " + previewSizeHeight);
        }
        final int initialSelectedIndex = selectedIndex;
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(cameraMainFragment.requireActivity())
                .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                    if (which != initialSelectedIndex) {
                        PreviewSize selectedPreviewSize = previewSizes.get(which);
                        cameraMainFragment.updateResolution(selectedPreviewSize.getWidth(), selectedPreviewSize.getHeight());
                    }
                    dialog.dismiss();
                })
                .show();
        if (alertDialog.getListView() != null) {
            alertDialog.getListView().setVerticalScrollBarEnabled(false);
        }
    }

    /**
     * 扫码按钮点击事件
     *
     * @param scanInterval 扫描间隔
     *                     扫码成功冷却时间
     *                     单位 - 毫秒
     */
    public void onScanCodeClicked(long scanInterval) {
        scanCodeProcessor.setScanInterval(scanInterval);
    }

    /**
     * 是否需要返回
     *
     * @return 是否需要返回
     */
    private boolean needReturn() {
        Activity activity = cameraMainFragment.requireActivity();
        return (activity.isFinishing() || activity.isDestroyed());
    }

    /**
     * 释放
     */
    public void release() {
        captureProcessor.release();
        documentCropProcessor.release();
        scanCodeProcessor.release();
    }

    /*回调*/

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
        if (needReturn()) {
            return;
        }
        try {
            // 文档裁剪处理器 - 异步处理 NV21
            documentCropProcessor.processNv21Async(cameraMainFragment.requireActivity(), nv21Data, width, height, this);
        } catch (Exception e) {
            Log.e(LogKit.TAG, "processNv21Async 失败 || " + e.getMessage());
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
        if (needReturn()) {
            return;
        }
        ToastUtils.show("拍照成功");
        Activity activity = cameraMainFragment.requireActivity();
        try {
            // 1. 文档裁剪处理器 - 异步处理
            documentCropProcessor.processAsync(activity, savePath, this);
        } catch (Exception e) {
            Log.e(LogKit.TAG, "processAsync 失败 || " + e.getMessage());
        }
        // 2. 通用文字识别 (高精度含位置信息版)
        BaiDuOcrHelper.recognizeAccurate(activity, savePath, new OnResultListener<GeneralResult>() {
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
        BaiDuOcrHelper.recognizeGeneralEnhanced(activity, savePath, new OnResultListener<GeneralResult>() {
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
        BaiDuOcrHelper.recognizeExampleDoc(activity, savePath, new OnResultListener<OcrResponseResult>() {
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
        BaiDuOcrHelper.recoginzeWrittenText(activity, savePath, new OnResultListener<OcrResponseResult>() {
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
        WeChatCropEngine.getInstance(activity).process(activity, savePath, true, new WeChatCropEngine.OnWeChatCropListener() {
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
        if (needReturn()) {
            return;
        }
        ToastUtils.show("拍照错误");
    }

    /**
     * 文档裁剪成功
     *
     * @param croppedPath  已拷路径
     * @param resultBitmap 结果像素数据
     */
    @Override
    public void onDocumentCropSuccess(String croppedPath, Bitmap resultBitmap) {
        ToastUtils.show("试卷矫正裁剪成功");
    }

    /**
     * 文档裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onDocumentCropError(String errorMsg) {
        ToastUtils.show("试卷矫正失败");
    }

    /**
     * 扫码成功
     *
     * @param result  结果
     * @param barcode Barcode
     */
    @Override
    public void onScanCodeSuccess(String result, Barcode barcode) {
        ToastUtils.show(result);
    }

    /**
     * 扫码失败
     *
     * @param e 异常
     */
    @Override
    public void onScanCodeFailure(Exception e) {
        ToastUtils.show("扫码失败");
    }
}