package com.qtone.camerause.scancode;

import android.util.Log;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.utils.ToastUtils;

/**
 * Created on 2026/8/6.
 *
 * @author 郑少鹏
 * @desc 扫码页配套原件
 */
public class ScanCodeActivityKit implements ScanCodeProcessor.OnScanCodeListener {
    private static final String TAG = ScanCodeActivityKit.class.getSimpleName();
    /**
     * 扫码处理器
     */
    private ScanCodeProcessor scanCodeProcessor;

    /**
     * constructor
     */
    public ScanCodeActivityKit() {
        // 扫码处理器
        this.scanCodeProcessor = new ScanCodeProcessor(this);
        // 设置扫描间隔
        this.scanCodeProcessor.setScanInterval(1200);
    }

    /**
     * 获取扫码处理器
     *
     * @return 扫码处理器
     */
    public ScanCodeProcessor getScanCodeProcessor() {
        return scanCodeProcessor;
    }

    /**
     * 释放
     */
    public void release() {
        if (scanCodeProcessor != null) {
            scanCodeProcessor.release();
            scanCodeProcessor = null;
        }
    }

    /**
     * 扫码成功
     *
     * @param result  结果
     * @param barcode Barcode
     */
    @Override
    public void onScanCodeSuccess(String result, Barcode barcode) {
        Log.d(TAG, "扫码成功 || " + result);
        ToastUtils.show(result);
    }

    /**
     * 扫码失败
     *
     * @param e 异常
     */
    @Override
    public void onScanCodeFailure(Exception e) {
        Log.e(TAG, "扫码失败 || ", e);
        ToastUtils.show("扫码失败");
    }
}