package com.qtone.camerause.base.kit;

import com.qtone.camerause.base.BaseCameraFragment;
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
}