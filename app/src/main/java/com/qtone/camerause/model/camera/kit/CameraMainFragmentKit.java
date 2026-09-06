package com.qtone.camerause.model.camera.kit;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.OcrResponseResult;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.R;
import com.qtone.camerause.widget.gesture.GestureRecognizerManager;
import com.qtone.camerause.model.camera.CameraMainFragment;
import com.qtone.camerause.model.gallery.GalleryActivity;
import com.qtone.camerause.model.setting.kit.SharedPreferencesKit;
import com.qtone.camerause.util.intent.IntentJump;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.view.ViewUtils;
import com.qtone.camerause.widget.camera.CameraFpsKit;
import com.qtone.camerause.widget.capture.CaptureMode;
import com.qtone.camerause.widget.capture.CaptureProcessor;
import com.qtone.camerause.widget.capture.CaptureStrategy;
import com.qtone.camerause.widget.crop.DocumentCropProcessor;
import com.qtone.camerause.widget.ocr.BaiDuOcrHelper;
import com.qtone.camerause.widget.roi.ImageRoiProcessor;
import com.qtone.camerause.widget.scan.ScanCodeProcessor;
import com.qtone.camerause.widget.scan.ViewFinderView;
import com.qtone.camerause.widget.wechat.WeChatCropEngine;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机主碎片配套原件
 */
public class CameraMainFragmentKit implements CaptureProcessor.OnCaptureCallback, DocumentCropProcessor.OnDocumentCropCallback, ScanCodeProcessor.OnScanCodeCallBack, CameraFpsKit.OnCameraFpsCallback, GestureRecognizerManager.OnGestureRecognizedListener {
    /**
     * 允许扫码状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    protected final AtomicBoolean isAllowScanCode = new AtomicBoolean(false);
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
     * 相机帧率配套原件
     */
    private final CameraFpsKit cameraFpsKit;
    /**
     * 连拍计数器
     * <p>
     * 使用 AtomicInteger 保证多线程并发环境下的绝对原子性
     */
    private final AtomicInteger burstCaptureCount = new AtomicInteger(0);

    private GestureRecognizerManager gestureRecognizerManager;

    /**
     * constructor
     *
     * @param cameraMainFragment 相机主碎片
     */
    public CameraMainFragmentKit(@NotNull CameraMainFragment cameraMainFragment) {
        // 相机主碎片
        this.cameraMainFragment = cameraMainFragment;
        // 拍照处理器
        this.captureProcessor = new CaptureProcessor();
        // 文档裁剪处理器
        this.documentCropProcessor = new DocumentCropProcessor();
        // 扫码处理器
        this.scanCodeProcessor = new ScanCodeProcessor(this);
        // 相机帧率配套原件
        this.cameraFpsKit = new CameraFpsKit(this);

        cameraMainFragment.safeRun(new Consumer<AppCompatActivity>() {
            @Override
            public void accept(AppCompatActivity appCompatActivity) {
                gestureRecognizerManager = new GestureRecognizerManager(appCompatActivity, CameraMainFragmentKit.this);
            }
        });
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
        // 统计帧数
        cameraFpsKit.countFrame();
        // 直接塞入原始数据
        // 内部自动异步转码、丢帧与手势推理
        gestureRecognizerManager.processPreviewFrame(data, width, height);
    }

    /**
     * 单拍按钮点击事件
     */
    public void onSingleCaptureClicked() {
        // 设置拍照策略
        captureProcessor.setCaptureStrategy(CaptureStrategy.FRAME_CAPTURE);
        // 开始单拍
        cameraMainFragment.safeRun(appCompatActivity -> captureProcessor.startSingleCapture(appCompatActivity, cameraMainFragment.getCurrentCamera(), CameraMainFragmentKit.this));
    }

    /**
     * 连拍按钮点击事件
     *
     * @param intervalMs 间隔毫秒
     */
    public void onBurstCaptureClicked(long intervalMs) {
        // 开始连拍
        cameraMainFragment.safeRun(appCompatActivity -> captureProcessor.startBurstCapture(appCompatActivity, cameraMainFragment.getCurrentCamera(), intervalMs, CameraMainFragmentKit.this));
    }

    /**
     * 停止连拍按钮点击事件
     */
    public void onStopBurstCaptureClicked() {
        // 停止连拍
        captureProcessor.stopBurstCapture();
    }

    /**
     * 扫码按钮点击事件
     *
     * @param viewFinderView 取景框视图
     * @param scanIntervalMs 扫描间隔毫秒
     */
    public void onScanCodeClicked(ViewFinderView viewFinderView, long scanIntervalMs) {
        if (isAllowScanCode.get()) {
            ToastUtils.show(R.string.doNotTriggerScanningRepeatedly);
            return;
        }
        // 显示视图
        ViewUtils.showView(viewFinderView);
        // 设置扫描框的宽度和高度
        viewFinderView.setFrameWidthAndHeight(cameraMainFragment.getTextureView().getWidth(), cameraMainFragment.getTextureView().getHeight(), 0.8f);
        // 设置扫描间隔毫秒
        scanCodeProcessor.setScanIntervalMs(scanIntervalMs);
        // 允许扫码状态锁
        isAllowScanCode.set(true);
        // 显示扫描
        viewFinderView.showScanner();
    }

    /**
     * 停止扫码按钮点击事件
     *
     * @param viewFinderView 取景框视图
     */
    public void onStopScanCodeClicked(@NotNull ViewFinderView viewFinderView) {
        // 允许扫码状态锁
        isAllowScanCode.set(false);
        // 停止扫描
        viewFinderView.stopScanner();
        // 隐藏视图
        ViewUtils.hideView(viewFinderView, View.GONE);
    }

    /**
     * 图库按钮点击事件
     */
    public void onGalleryClicked() {
        cameraMainFragment.safeRun(appCompatActivity -> IntentJump.getInstance().jumpWithAnimation(null, appCompatActivity, false, GalleryActivity.class, 0, 0));
    }

    /**
     * 释放
     */
    public void release() {
        // 允许扫码状态锁
        isAllowScanCode.set(false);
        // 拍照处理器
        captureProcessor.release();
        // 文档裁剪处理器
        documentCropProcessor.release();
        // 扫码处理器
        scanCodeProcessor.release();
        // 微信裁剪引擎
        WeChatCropEngine.getInstance(cameraMainFragment.getContext()).release();
        // MultiRoiOverlayView
        cameraMainFragment.getMultiRoiOverlayView().clearAllRoi();

        // 5. 销毁时释放线程池与 AI 模型内存
        if (gestureRecognizerManager != null) {
            gestureRecognizerManager.release();
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
     * 拍照处理中
     *
     * @param data        图像帧字节数组
     * @param width       帧物理宽
     * @param height      帧物理高
     * @param captureMode 拍照模式
     */
    @Override
    public void onCaptureProcessing(byte[] data, int width, int height, CaptureMode captureMode) {
        cameraMainFragment.safeRun(appCompatActivity -> {
            try {
                // 文档裁剪处理器 - 通过图像帧字节数组处理
                /*documentCropProcessor.processByData(appCompatActivity, data, width, height, CameraMainFragmentKit.this);*/
            } catch (Exception e) {
                Log.e(LogKit.TAG, "通过图像帧字节数组处理 - 失败 || " + e.getMessage());
            }
        });
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
        ToastUtils.show("拍照成功");
        if (captureProcessor.captureSuccessFromSingleCapture(captureMode)) {
            cameraMainFragment.cameraMainFragmentSbSingleCapture.stop();
        }
        if (captureProcessor.captureSuccessFromBurstCapture(captureMode)) {
            burstCaptureCount.incrementAndGet();
            cameraMainFragment.cameraMainFragmentSbBurstCapture.setText(burstCaptureCount.get() + "\n当前已拍");
        }
        cameraMainFragment.safeRun(appCompatActivity -> {
            // 1. 是否允许文档裁剪
            if (SharedPreferencesKit.isDocumentCropEnabled(appCompatActivity)) {
                try {
                    // 文档裁剪处理器 - 通过路径处理
                    documentCropProcessor.processByPath(appCompatActivity, savePath, CameraMainFragmentKit.this);
                } catch (Exception e) {
                    Log.e(LogKit.TAG, "通过路径处理 - 失败 || " + e.getMessage());
                }
            }
            // 2. 是否允许百度 OCR
            if (SharedPreferencesKit.isBaiduOcrEnabled(appCompatActivity)) {
                // 通用文字识别 (高精度含位置信息版)
                BaiDuOcrHelper.recognizeAccurate(appCompatActivity, savePath, new OnResultListener<GeneralResult>() {
                    @Override
                    public void onResult(GeneralResult generalResult) {
                        Log.e(LogKit.TAG, "通用文字识别 (高精度含位置信息版) 结果\n" + generalResult);
                    }

                    @Override
                    public void onError(OCRError ocrError) {
                        Log.e(LogKit.TAG, "通用文字识别 (高精度含位置信息版) 错误\n" + ocrError.getMessage());
                    }
                }, false);
                // 通用文字识别 (含生僻字版)
                BaiDuOcrHelper.recognizeGeneralEnhanced(appCompatActivity, savePath, new OnResultListener<GeneralResult>() {
                    @Override
                    public void onResult(GeneralResult generalResult) {
                        Log.e(LogKit.TAG, "通用文字识别 (含生僻字版) 结果\n" + generalResult);
                    }

                    @Override
                    public void onError(OCRError ocrError) {
                        Log.e(LogKit.TAG, "通用文字识别 (含生僻字版) 错误\n" + ocrError.getMessage());
                    }
                }, false);
                // 试卷分析与识别
                BaiDuOcrHelper.recognizeExampleDoc(appCompatActivity, savePath, new OnResultListener<OcrResponseResult>() {
                    @Override
                    public void onResult(OcrResponseResult ocrResponseResult) {
                        Log.e(LogKit.TAG, "试卷分析与识别结果\n" + ocrResponseResult);
                    }

                    @Override
                    public void onError(OCRError ocrError) {
                        Log.e(LogKit.TAG, "试卷分析与识别错误\n" + ocrError.getMessage());
                    }
                }, false);
                // 手写文字识别
                BaiDuOcrHelper.recoginzeWrittenText(appCompatActivity, savePath, new OnResultListener<OcrResponseResult>() {
                    @Override
                    public void onResult(OcrResponseResult ocrResponseResult) {
                        Log.e(LogKit.TAG, "手写文字识别结果\n" + ocrResponseResult);
                    }

                    @Override
                    public void onError(OCRError ocrError) {
                        Log.e(LogKit.TAG, "手写文字识别错误\n" + ocrError.getMessage());
                    }
                }, false);
            }
            // 3. 是否允许微信裁剪
            if (SharedPreferencesKit.isWechatCropEnabled(appCompatActivity)) {
                // 微信裁剪引擎 - 处理
                WeChatCropEngine.getInstance(appCompatActivity).process(appCompatActivity, savePath, true, false, new WeChatCropEngine.OnWeChatCropCallback() {
                    @Override
                    public void onWeChatCropSuccess(List<Bitmap> resultBitmaps, List<String> savedPaths) {
                        ToastUtils.show("微信裁剪成功");
                    }

                    @Override
                    public void onWeChatCropError(String errorMessage) {
                        ToastUtils.show("微信裁剪错误");
                    }
                });
            }
            // 4. 是否允许 ROI 裁剪
            if (SharedPreferencesKit.isRoiCropEnabled(appCompatActivity)) {
                // 图片 ROI 处理器 - 从图片文件裁剪 ROI
                ImageRoiProcessor.cropRoiFromImageFile(appCompatActivity, savePath, cameraMainFragment.getMultiRoiOverlayView());
            }
            // 5. 是否允许 ROI 覆盖
            if (SharedPreferencesKit.isRoiOverlayEnabled(appCompatActivity)) {
                // 图片 ROI 处理器 - 绘制 ROI 到图片文件
                ImageRoiProcessor.drawRoiToImageFile(appCompatActivity, savePath, cameraMainFragment.getMultiRoiOverlayView(), false);
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
        ToastUtils.show("拍照错误 - " + errorMsg);
        //cameraMainFragment.safeRun(appCompatActivity -> CommonDialogKit.showInfoDialog(appCompatActivity, "拍照错误", errorMsg, false, appCompatActivity.getString(R.string.iKonw), null));
        if (captureProcessor.captureErrorFromSingleCapture()) {
            cameraMainFragment.cameraMainFragmentSbSingleCapture.stop();
        }
    }

    /**
     * 文档裁剪成功
     *
     * @param croppedPath  已拷路径
     * @param resultBitmap 结果像素数据
     */
    @Override
    public void onDocumentCropSuccess(String croppedPath, Bitmap resultBitmap) {
        ToastUtils.show("文档裁剪成功");
    }

    /**
     * 文档裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onDocumentCropError(String errorMsg) {
        ToastUtils.show("文档裁剪错误 - " + errorMsg);
        //cameraMainFragment.safeRun(appCompatActivity -> CommonDialogKit.showInfoDialog(appCompatActivity, "文档裁剪错误", errorMsg, false, appCompatActivity.getString(R.string.iKonw), null));
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
    public void onScanCodeFailure(@NotNull Exception e) {
        ToastUtils.show("扫码失败 - " + e.getMessage());
    }

    /**
     * 相机帧率变化
     *
     * @param fps 帧率
     */
    @Override
    public void onCameraFpsChange(float fps) {
        Log.d(LogKit.TAG, "帧率 = " + fps);
    }

    /**
     * 识别结果回调 (已自动切换至主线程)
     *
     * @param gestureName             手势名称 (如 "Victory", "Open_Palm", "Closed_Fist", "Thumb_Up", "None")
     * @param gestureRecognizerResult MediaPipe 原始结果对象 (包含手部 21 个关节点坐标)
     * @param inferenceTimeMs         推理耗时 (毫秒)
     */
    @Override
    public void onGestureRecognized(String gestureName, GestureRecognizerResult gestureRecognizerResult, long inferenceTimeMs) {
        Log.d("Gesture", gestureName);
        switch (gestureName) {
            case "Victory":
                // 剪刀手 ✌
                Log.d(LogKit.TAG, "剪刀手 ✌");
                ToastUtils.show("剪刀手");
                break;
            case "Open_Palm":
                // 张开手掌 🖐
                Log.d(LogKit.TAG, "张开手掌 \uD83D\uDD90");
                ToastUtils.show("张开手掌");
                break;
            case "Closed_Fist":
                // 握拳 ✊
                Log.d(LogKit.TAG, "握拳 ✊");
                ToastUtils.show("握拳");
                break;
            case "Thumb_Up":
                // 点赞 👍
                Log.d(LogKit.TAG, "点赞 \uD83D\uDC4D");
                ToastUtils.show("点赞");
                break;
            case "None":
            default:
                // 未识别到特定手势
                break;
        }
    }

    /**
     * 错误回调
     *
     * @param error
     */
    @Override
    public void onError(String error) {
        Log.e("Gesture", "手势识别异常: " + error);
    }
}