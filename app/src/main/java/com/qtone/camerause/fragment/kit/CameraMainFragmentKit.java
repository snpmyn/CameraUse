package com.qtone.camerause.fragment.kit;

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
import com.qtone.camerause.fragment.CameraMainFragment;
import com.qtone.camerause.function.capture.CaptureMode;
import com.qtone.camerause.function.capture.CaptureProcessor;
import com.qtone.camerause.function.capture.CaptureStrategy;
import com.qtone.camerause.function.crop.DocumentCropProcessor;
import com.qtone.camerause.function.ocr.BaiDuOcrHelper;
import com.qtone.camerause.function.scancode.ScanCodeProcessor;
import com.qtone.camerause.function.wechat.WeChatCropEngine;
import com.qtone.camerause.kit.list.ListKit;
import com.qtone.camerause.kit.log.LogKit;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机主碎片配套原件
 */
public class CameraMainFragmentKit implements CaptureProcessor.OnCaptureCallBack, DocumentCropProcessor.OnDocumentCropCallback, ScanCodeProcessor.OnScanCodeListener {
    /**
     * 允许扫码状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    protected final AtomicBoolean isAllowScanCode = new AtomicBoolean(true);
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
     * 处理帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        // 拍照处理器 - 处理帧
        captureProcessor.processFrame(data, width, height, dataFormat, this);
        if (isAllowScanCode.get()) {
            // 扫码处理器 - 处理帧
            scanCodeProcessor.processFrame(data, width, height, dataFormat, 0);
        }
    }

    /**
     * 单拍按钮点击事件
     */
    public void onSingleCaptureClicked() {
        if (!isAllowScanCode.compareAndSet(true, false)) {
            return;
        }
        // 停止连拍
        captureProcessor.stopBurstCapture();
        // 设置拍照策略
        captureProcessor.setCaptureStrategy(CaptureStrategy.FRAME_CAPTURE);
        // 开始单拍
        captureProcessor.startSingleCapture(cameraMainFragment.requireActivity(), cameraMainFragment.getCurrentCamera(), this);
    }

    /**
     * 连拍按钮点击事件
     *
     * @param interval 时间间隔
     *                 单位 - 毫秒
     */
    public void onBurstCaptureClicked(long interval) {
        if (!isAllowScanCode.compareAndSet(true, false)) {
            return;
        }
        // 开始连拍
        captureProcessor.startBurstCapture(cameraMainFragment.requireActivity(), cameraMainFragment.getCurrentCamera(), interval, this);
    }

    /**
     * 停止连拍按钮点击事件
     */
    public void onStopBurstCaptureClicked() {
        // 停止连拍
        captureProcessor.stopBurstCapture();
        // 允许扫码状态锁
        isAllowScanCode.set(true);
    }

    /**
     * 切换分辨率按钮点击事件
     */
    public void onSwitchResolutionClicked() {
        List<PreviewSize> previewSizes = cameraMainFragment.getAllPreviewSizes(null);
        if (ListKit.listIsEmpty(previewSizes)) {
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
     * @param interval 时间间隔
     *                 扫码成功冷却时间
     *                 单位 - 毫秒
     */
    public void onScanCodeClicked(long interval) {
        scanCodeProcessor.setScanInterval(interval);
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
        // 允许扫码状态锁
        isAllowScanCode.set(true);
        // 拍照处理器
        captureProcessor.release();
        // 文档裁剪处理器
        documentCropProcessor.release();
        // 扫码处理器
        scanCodeProcessor.release();
    }

    /**
     * 拍照开始
     */
    @Override
    public void onCaptureBegin() {
        ToastUtils.show("拍照开始");
    }

    /**
     * 拍照处理中
     *
     * @param data        图像帧字节数组
     * @param width       帧物理宽
     * @param height      帧物理高
     * @param captureMode 拍照模式
     */
    @Override
    public void onCaptureProcessing(byte[] data, int width, int height, CaptureMode captureMode) {
        if (needReturn()) {
            return;
        }
        try {
            // 文档裁剪处理器 - 异步处理 NV21
            documentCropProcessor.processNv21Async(cameraMainFragment.requireActivity(), data, width, height, this);
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
        if (captureMode == CaptureMode.SINGLE) {
            // 单拍成功立刻恢复允许扫码
            // 连拍需点击停拍按钮再恢复
            isAllowScanCode.set(true);
        }
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
        isAllowScanCode.set(true);
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