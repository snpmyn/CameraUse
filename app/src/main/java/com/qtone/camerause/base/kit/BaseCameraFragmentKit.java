package com.qtone.camerause.base.kit;

import android.util.Log;

import com.qtone.camerause.base.BaseCameraFragment;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.widget.camera.CameraController;

/**
 * Created on 2026/8/28.
 *
 * @author 郑少鹏
 * @desc 相机碎片基类配套原件
 */
public class BaseCameraFragmentKit {
    /**
     * 相机碎片基类
     */
    private final BaseCameraFragment baseCameraFragment;

    /**
     * constructor
     *
     * @param baseCameraFragment 相机碎片基类
     */
    public BaseCameraFragmentKit(BaseCameraFragment baseCameraFragment) {
        this.baseCameraFragment = baseCameraFragment;
    }

    /**
     * 自动对焦
     */
    public void setAutoFocus() {
        CameraController.getInstance().setAutoFocus(baseCameraFragment.getCurrentCamera(), true);
    }

    /**
     * 相机设置
     */
    public void cameraSetting() {
        // 亮度
        int brightness = 43;
        CameraController.getInstance().setBrightness(baseCameraFragment.getCurrentCamera(), brightness);
        Log.d(LogKit.TAG, "亮度 - 最优 || " + brightness);
        // 对比度
        int contrast = 58;
        CameraController.getInstance().setContrast(baseCameraFragment.getCurrentCamera(), contrast);
        Log.d(LogKit.TAG, "对比度 - 最优 || " + contrast);
        // 增益
        int gain = 6;
        CameraController.getInstance().setGain(baseCameraFragment.getCurrentCamera(), gain);
        Log.d(LogKit.TAG, "增益 - 最优 || " + gain);
        // Gamma
        int gamma = 31;
        CameraController.getInstance().setGamma(baseCameraFragment.getCurrentCamera(), gamma);
        Log.d(LogKit.TAG, "Gamma - 最优 || " + gamma);
        // 色调
        int hue = 52;
        CameraController.getInstance().setHue(baseCameraFragment.getCurrentCamera(), hue);
        Log.d(LogKit.TAG, "色调 - 最优 || " + hue);
        // 锐度
        int sharpness = 60;
        CameraController.getInstance().setSharpness(baseCameraFragment.getCurrentCamera(), sharpness);
        Log.d(LogKit.TAG, "锐度 - 最优 || " + sharpness);
        // 饱和度
        int saturation = 82;
        CameraController.getInstance().setSaturation(baseCameraFragment.getCurrentCamera(), saturation);
        Log.d(LogKit.TAG, "饱和度 - 最优 || " + saturation);
    }
}