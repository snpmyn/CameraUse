package com.qtone.camerause.widget.dialog.camerasetting;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;
import com.qtone.camerause.model.camera.CameraMainFragment;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;
import com.qtone.camerause.widget.dialog.camerasetting.listener.CameraSettingDialogClickListener;
import com.qtone.seekbar.OnRangeChangedListener;
import com.qtone.seekbar.RangeSeekBar;

/**
 * Created on 2026/8/27.
 *
 * @author 郑少鹏
 * @desc 相机设置对话框
 */
public class CameraSettingDialog extends BaseLifecycleDialog {
    /**
     * 控件
     */
    private TextView cameraSettingDialogTvTitle;
    private RangeSeekBar cameraSettingDialogRsbBrightness;
    private RangeSeekBar cameraSettingDialogRsbContrast;
    private RangeSeekBar cameraSettingDialogRsbGain;
    private RangeSeekBar cameraSettingDialogRsbGamma;
    private RangeSeekBar cameraSettingDialogRsbHue;
    private RangeSeekBar cameraSettingDialogRsbSharpness;
    private RangeSeekBar cameraSettingDialogRsbSaturation;
    private Button cameraSettingDialogMbNegative;
    private Button cameraSettingDialogMbPositive;
    /**
     * 标题
     */
    private String title;
    /**
     * 相机主碎片
     */
    private CameraMainFragment cameraMainFragment;
    /**
     * 消极文本
     */
    private String negativeText;
    /**
     * 显示消极
     */
    private boolean showNegative = false;
    /**
     * 积极文本
     */
    private String positiveText;
    /**
     * 相机设置对话框点击监听
     */
    private CameraSettingDialogClickListener cameraSettingDialogClickListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public CameraSettingDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.dialog_camera_setting;
    }

    /**
     * 初始化控件
     */
    @Override
    protected void initView() {
        cameraSettingDialogTvTitle = findViewById(R.id.cameraSettingDialogTvTitle);
        cameraSettingDialogRsbBrightness = findViewById(R.id.cameraSettingDialogRsbBrightness);
        cameraSettingDialogRsbBrightness.getLeftSeekBar().setIndicatorTextDecimalFormat("亮度 0");
        cameraSettingDialogRsbContrast = findViewById(R.id.cameraSettingDialogRsbContrast);
        cameraSettingDialogRsbContrast.getLeftSeekBar().setIndicatorTextDecimalFormat("对比度 0");
        cameraSettingDialogRsbGain = findViewById(R.id.cameraSettingDialogRsbGain);
        cameraSettingDialogRsbGain.getLeftSeekBar().setIndicatorTextDecimalFormat("增益 0");
        cameraSettingDialogRsbGamma = findViewById(R.id.cameraSettingDialogRsbGamma);
        cameraSettingDialogRsbGamma.getLeftSeekBar().setIndicatorTextDecimalFormat("Gamma 0");
        cameraSettingDialogRsbHue = findViewById(R.id.cameraSettingDialogRsbHue);
        cameraSettingDialogRsbHue.getLeftSeekBar().setIndicatorTextDecimalFormat("色调 0");
        cameraSettingDialogRsbSharpness = findViewById(R.id.cameraSettingDialogRsbSharpness);
        cameraSettingDialogRsbSharpness.getLeftSeekBar().setIndicatorTextDecimalFormat("锐度 0");
        cameraSettingDialogRsbSaturation = findViewById(R.id.cameraSettingDialogRsbSaturation);
        cameraSettingDialogRsbSaturation.getLeftSeekBar().setIndicatorTextDecimalFormat("饱和度 0");
        cameraSettingDialogMbNegative = findViewById(R.id.cameraSettingDialogMbNegative);
        cameraSettingDialogMbPositive = findViewById(R.id.cameraSettingDialogMbPositive);
    }

    /**
     * 初始化数据
     */
    @Override
    protected void initData() {
        // 标题
        if (!TextUtils.isEmpty(title)) {
            cameraSettingDialogTvTitle.setText(title);
            cameraSettingDialogTvTitle.setVisibility(View.VISIBLE);
        }
        // 设置
        if (cameraMainFragment != null) {
            // 设置进度
            setProgress();
            // 设置监听
            setListener();
        }
        // 消极
        if (showNegative) {
            cameraSettingDialogMbNegative.setText(TextUtils.isEmpty(negativeText) ? getContext().getText(R.string.cancel) : negativeText);
            cameraSettingDialogMbNegative.setVisibility(View.VISIBLE);
        }
        // 积极
        cameraSettingDialogMbPositive.setText(TextUtils.isEmpty(positiveText) ? getContext().getText(R.string.ensure) : positiveText);
    }

    /**
     * 初始化事件
     */
    @Override
    protected void initEvent() {
        cameraSettingDialogMbNegative.setOnClickListener(v -> {
            if (cameraSettingDialogClickListener != null) {
                cameraSettingDialogClickListener.onCancel(this);
            }
        });
        cameraSettingDialogMbPositive.setOnClickListener(v -> {
            if (cameraSettingDialogClickListener != null) {
                cameraSettingDialogClickListener.onConfirm(this);
            }
        });
    }

    /**
     * 清理资源
     * <p>
     * 对话框从 Window 移除时触发
     */
    @Override
    protected void onClearResource() {
        super.onClearResource();
        // 规避内存泄漏
        // 置空相机设置对话框点击监听
        cameraSettingDialogClickListener = null;
    }

    /**
     * 设置标题
     *
     * @param title 标题
     * @return 相机设置对话框
     */
    public CameraSettingDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * 设置相机主碎片
     *
     * @param cameraMainFragment 相机主碎片
     * @return 相机设置对话框
     */
    public CameraSettingDialog setCameraMainFragment(CameraMainFragment cameraMainFragment) {
        this.cameraMainFragment = cameraMainFragment;
        return this;
    }

    /**
     * 设置消极文本
     *
     * @param negativeText 消极文本
     * @return 相机设置对话框
     */
    public CameraSettingDialog setNegativeText(String negativeText) {
        this.negativeText = negativeText;
        return this;
    }

    /**
     * 设置显示消极
     *
     * @param showNegative 显示消极
     * @return 相机设置对话框
     */
    public CameraSettingDialog setShowNegative(boolean showNegative) {
        this.showNegative = showNegative;
        return this;
    }

    /**
     * 设置积极文本
     *
     * @param positiveText 积极文本
     * @return 相机设置对话框
     */
    public CameraSettingDialog setPositiveText(String positiveText) {
        this.positiveText = positiveText;
        return this;
    }

    /**
     * 设置相机设置对话框点击监听
     *
     * @param cameraSettingDialogClickListener 相机设置对话框点击监听
     * @return 相机设置对话框
     */
    public CameraSettingDialog setCameraSettingDialogClickListener(CameraSettingDialogClickListener cameraSettingDialogClickListener) {
        this.cameraSettingDialogClickListener = cameraSettingDialogClickListener;
        return this;
    }

    /**
     * 设置进度
     */
    private void setProgress() {
        // 亮度
        cameraSettingDialogRsbBrightness.setProgress(43.0f);
        // 对比度
        cameraSettingDialogRsbContrast.setProgress(58.0f);
        // 增益
        cameraSettingDialogRsbGain.setProgress(6.0f);
        // Gamma
        cameraSettingDialogRsbGamma.setProgress(31.0f);
        // 色调
        cameraSettingDialogRsbHue.setProgress(52.0f);
        // 锐度
        cameraSettingDialogRsbSharpness.setProgress(60.0f);
        // 饱和度
        cameraSettingDialogRsbSaturation.setProgress(82.0f);
    }

    /**
     * 设置监听
     */
    private void setListener() {
        // 亮度
        cameraSettingDialogRsbBrightness.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setBrightness(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // 对比度
        cameraSettingDialogRsbContrast.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setContrast(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // 增益
        cameraSettingDialogRsbGain.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setGain(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // Gamma
        cameraSettingDialogRsbGamma.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setGamma(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // 色调
        cameraSettingDialogRsbHue.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setHue(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // 锐度
        cameraSettingDialogRsbSharpness.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setSharpness(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        // 饱和度
        cameraSettingDialogRsbSaturation.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    CameraController.getInstance().setSaturation(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 开始拖动滑块 -> 禁止 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                // 停止拖动滑块 -> 允许 ScrollView 拦截事件
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
    }

    /**
     * 重置
     */
    public void reset() {
        // 重置亮度
        CameraController.getInstance().resetBrightness(cameraMainFragment.getCurrentCamera());
        float brightness = CameraController.getInstance().getBrightness(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbBrightness.setProgress(brightness);
        Log.d(LogKit.TAG, "亮度 - " + brightness);
        // 重置对比度
        CameraController.getInstance().resetContrast(cameraMainFragment.getCurrentCamera());
        float contrast = CameraController.getInstance().getContrast(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbContrast.setProgress(contrast);
        Log.d(LogKit.TAG, "对比度 - " + contrast);
        // 重置增益
        CameraController.getInstance().resetGain(cameraMainFragment.getCurrentCamera());
        float gain = CameraController.getInstance().getGain(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbGain.setProgress(gain);
        Log.d(LogKit.TAG, "增益 - " + gain);
        // 重置 Gamma
        CameraController.getInstance().resetGamma(cameraMainFragment.getCurrentCamera());
        float gamma = CameraController.getInstance().getGamma(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbGamma.setProgress(gamma);
        Log.d(LogKit.TAG, "Gamma - " + gamma);
        // 重置色调
        CameraController.getInstance().resetHue(cameraMainFragment.getCurrentCamera());
        float hue = CameraController.getInstance().getHue(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbHue.setProgress(hue);
        Log.d(LogKit.TAG, "色调 - " + hue);
        // 重置锐度
        CameraController.getInstance().resetSharpness(cameraMainFragment.getCurrentCamera());
        float sharpness = CameraController.getInstance().getSharpness(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbSharpness.setProgress(sharpness);
        Log.d(LogKit.TAG, "锐度 - " + sharpness);
        // 重置饱和度
        CameraController.getInstance().resetSaturation(cameraMainFragment.getCurrentCamera());
        float saturation = CameraController.getInstance().getSaturation(cameraMainFragment.getCurrentCamera()).floatValue();
        cameraSettingDialogRsbSaturation.setProgress(saturation);
        Log.d(LogKit.TAG, "饱和度 - " + saturation);
    }
}