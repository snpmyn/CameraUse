package com.qtone.camerause.application;

import android.util.Log;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.jiangdg.ausbc.base.BaseApplication;
import com.qtone.camerause.function.storage.MediaStorageConfig;
import com.qtone.camerause.utils.log.LogKit;
import com.qtone.camerause.utils.mmkv.MmkvKit;

import org.opencv.android.OpenCVLoader;

/**
 * Created on 2026/7/29.
 *
 * @author 郑少鹏
 * @desc 应用
 */
public class App extends BaseApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 MMKV
        MmkvKit.INSTANCE.init(this);
        // 初始化媒体存储配置
        MediaStorageConfig.getInstance().init(this, "CU");
        // 初始化 OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.d(LogKit.TAG, "OpenCV 初始化成功");
        } else {
            Log.e(LogKit.TAG, "OpenCV 初始化失败");
        }
        // 初始化百度 OCR
        OCR.getInstance(this).initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                Log.d(LogKit.TAG, "百度 OCR 初始化成功 || " + accessToken);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.d(LogKit.TAG, "百度 OCR 初始化错误 || " + ocrError.getMessage());
            }
        }, this);
    }
}