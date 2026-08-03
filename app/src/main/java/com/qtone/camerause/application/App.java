package com.qtone.camerause.application;

import android.app.Application;
import android.util.Log;

import com.jiangdg.ausbc.utils.ToastUtils;

import org.opencv.android.OpenCVLoader;

/**
 * Created on 2026/7/29.
 *
 * @author 郑少鹏
 * @desc 应用
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 ToastUtils
        ToastUtils.INSTANCE.init(this);
        // 初始化 OpenCV
        // 验证 OpenCV 依赖与 .so 库是否正常加载
        if (OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "OpenCV 初始化成功");
        } else {
            Log.e("OpenCV", "OpenCV 初始化失败");
        }
    }
}