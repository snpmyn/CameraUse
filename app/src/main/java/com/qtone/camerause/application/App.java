package com.qtone.camerause.application;

import android.app.Application;

import com.jiangdg.ausbc.utils.ToastUtils;

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
        ToastUtils.INSTANCE.init(this);
    }
}