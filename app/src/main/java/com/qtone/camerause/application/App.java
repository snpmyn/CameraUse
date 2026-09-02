package com.qtone.camerause.application;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.jiangdg.ausbc.base.BaseApplication;
import com.qtone.camerause.util.activity.ActivitySuperviseManager;
import com.qtone.camerause.util.app.AppListener;
import com.qtone.camerause.util.density.DensityUtils;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.log.LogUtils;
import com.qtone.camerause.util.mmkv.MmkvKit;
import com.qtone.camerause.widget.storage.MediaStorageConfig;

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
        // 日志工具类
        LogUtils.Builder.initConfiguration(true, true, true, true);
        // 全局监听 Activity 生命周期
        registerActivityListener();
        // 初始化 MMKV
        MmkvKit.INSTANCE.init(this);
        // 应用监听
        AppListener.getInstance().initConfiguration(this);
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
        // DPI
        String dpi = "X DPI = " + DensityUtils.getDpiOnX(getApplicationContext())
                + " Y DPI = " + DensityUtils.getDpiOnY(getApplicationContext())
                + " DPI = " + DensityUtils.getDensityDpi(getApplicationContext());
        Log.d(LogKit.TAG, dpi);
    }

    /**
     * Activity 全局监听
     */
    private void registerActivityListener() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                // 添监听到创事件 Activity 至集合
                ActivitySuperviseManager.getInstance().pushActivity(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                // 移监听到销事件 Activity 出集合
                ActivitySuperviseManager.getInstance().removeActivity(activity);
            }
        });
    }
}