package com.qtone.camerause.widget.dialog.camerasetting;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;
import com.qtone.camerause.model.camera.CameraMainFragment;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.view.ViewUtils;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;
import com.qtone.camerause.widget.dialog.camerasetting.listener.CameraSettingDialogClickListener;
import com.qtone.seekbar.OnRangeChangedListener;
import com.qtone.seekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.List;

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
    private LinearLayout cameraSettingDialogLlBrightness;
    private TextView cameraSettingDialogTvBrightness;
    private RangeSeekBar cameraSettingDialogRsbBrightness;
    private LinearLayout cameraSettingDialogLlContrast;
    private TextView cameraSettingDialogTvContrast;
    private RangeSeekBar cameraSettingDialogRsbContrast;
    private LinearLayout cameraSettingDialogLlGain;
    private TextView cameraSettingDialogTvGain;
    private RangeSeekBar cameraSettingDialogRsbGain;
    private LinearLayout cameraSettingDialogLlGamma;
    private TextView cameraSettingDialogTvGamma;
    private RangeSeekBar cameraSettingDialogRsbGamma;
    private LinearLayout cameraSettingDialogLlHue;
    private TextView cameraSettingDialogTvHue;
    private RangeSeekBar cameraSettingDialogRsbHue;
    private LinearLayout cameraSettingDialogLlSharpness;
    private TextView cameraSettingDialogTvSharpness;
    private RangeSeekBar cameraSettingDialogRsbSharpness;
    private LinearLayout cameraSettingDialogLlSaturation;
    private TextView cameraSettingDialogTvSaturation;
    private RangeSeekBar cameraSettingDialogRsbSaturation;
    private LinearLayout cameraSettingDialogLlBottom;
    private Button cameraSettingDialogMbNegative;
    private Button cameraSettingDialogMbNeutral;
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
     * 中性文本
     */
    private String neutralText;
    /**
     * 显示中性
     */
    private boolean showNeutral = false;
    /**
     * 积极文本
     */
    private String positiveText;
    /**
     * 视图集
     */
    private List<View> viewList;
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
        cameraSettingDialogLlBrightness = findViewById(R.id.cameraSettingDialogLlBrightness);
        cameraSettingDialogTvBrightness = findViewById(R.id.cameraSettingDialogTvBrightness);
        cameraSettingDialogRsbBrightness = findViewById(R.id.cameraSettingDialogRsbBrightness);
        cameraSettingDialogLlContrast = findViewById(R.id.cameraSettingDialogLlContrast);
        cameraSettingDialogTvContrast = findViewById(R.id.cameraSettingDialogTvContrast);
        cameraSettingDialogRsbContrast = findViewById(R.id.cameraSettingDialogRsbContrast);
        cameraSettingDialogLlGain = findViewById(R.id.cameraSettingDialogLlGain);
        cameraSettingDialogTvGain = findViewById(R.id.cameraSettingDialogTvGain);
        cameraSettingDialogRsbGain = findViewById(R.id.cameraSettingDialogRsbGain);
        cameraSettingDialogLlGamma = findViewById(R.id.cameraSettingDialogLlGamma);
        cameraSettingDialogTvGamma = findViewById(R.id.cameraSettingDialogTvGamma);
        cameraSettingDialogRsbGamma = findViewById(R.id.cameraSettingDialogRsbGamma);
        cameraSettingDialogLlHue = findViewById(R.id.cameraSettingDialogLlHue);
        cameraSettingDialogTvHue = findViewById(R.id.cameraSettingDialogTvHue);
        cameraSettingDialogRsbHue = findViewById(R.id.cameraSettingDialogRsbHue);
        cameraSettingDialogLlSharpness = findViewById(R.id.cameraSettingDialogLlSharpness);
        cameraSettingDialogTvSharpness = findViewById(R.id.cameraSettingDialogTvSharpness);
        cameraSettingDialogRsbSharpness = findViewById(R.id.cameraSettingDialogRsbSharpness);
        cameraSettingDialogLlSaturation = findViewById(R.id.cameraSettingDialogLlSaturation);
        cameraSettingDialogTvSaturation = findViewById(R.id.cameraSettingDialogTvSaturation);
        cameraSettingDialogRsbSaturation = findViewById(R.id.cameraSettingDialogRsbSaturation);
        cameraSettingDialogLlBottom = findViewById(R.id.cameraSettingDialogLlBottom);
        cameraSettingDialogMbNegative = findViewById(R.id.cameraSettingDialogMbNegative);
        cameraSettingDialogMbNeutral = findViewById(R.id.cameraSettingDialogMbNeutral);
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
        // 消极
        if (showNeutral) {
            cameraSettingDialogMbNeutral.setText(TextUtils.isEmpty(neutralText) ? getContext().getText(R.string.neutral) : neutralText);
            cameraSettingDialogMbNeutral.setVisibility(View.VISIBLE);
        }
        // 积极
        cameraSettingDialogMbPositive.setText(TextUtils.isEmpty(positiveText) ? getContext().getText(R.string.ensure) : positiveText);
        // 视图集
        viewList = new ArrayList<>(9);
        viewList.add(cameraSettingDialogTvTitle);
        viewList.add(cameraSettingDialogLlBrightness);
        viewList.add(cameraSettingDialogLlContrast);
        viewList.add(cameraSettingDialogLlGain);
        viewList.add(cameraSettingDialogLlGamma);
        viewList.add(cameraSettingDialogLlHue);
        viewList.add(cameraSettingDialogLlSharpness);
        viewList.add(cameraSettingDialogLlSaturation);
        viewList.add(cameraSettingDialogLlBottom);
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
        cameraSettingDialogMbNeutral.setOnClickListener(v -> {
            if (cameraSettingDialogClickListener != null) {
                cameraSettingDialogClickListener.onNeutral(this);
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
     * 设置中性文本
     *
     * @param neutralText 中性文本
     * @return 相机设置对话框
     */
    public CameraSettingDialog setNeutralText(String neutralText) {
        this.neutralText = neutralText;
        return this;
    }

    /**
     * 设置显示中性
     *
     * @param showNeutral 显示中性
     * @return 相机设置对话框
     */
    public CameraSettingDialog setShowNeutral(boolean showNeutral) {
        this.showNeutral = showNeutral;
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
    public void setProgress() {
        // 亮度
        int brightness = CameraController.getInstance().getBrightness(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvBrightness.setText(String.valueOf(brightness));
        cameraSettingDialogRsbBrightness.setProgress(Integer.valueOf(brightness).floatValue());
        Log.d(LogKit.TAG, "亮度 - " + brightness);
        // 对比度
        int contrast = CameraController.getInstance().getContrast(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvContrast.setText(String.valueOf(contrast));
        cameraSettingDialogRsbContrast.setProgress(Integer.valueOf(contrast).floatValue());
        Log.d(LogKit.TAG, "对比度 - " + contrast);
        // 增益
        int gain = CameraController.getInstance().getGain(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvGain.setText(String.valueOf(gain));
        cameraSettingDialogRsbGain.setProgress(Integer.valueOf(gain).floatValue());
        Log.d(LogKit.TAG, "增益 - " + gain);
        // Gamma
        int gamma = CameraController.getInstance().getGamma(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvGamma.setText(String.valueOf(gamma));
        cameraSettingDialogRsbGamma.setProgress(Integer.valueOf(gamma).floatValue());
        Log.d(LogKit.TAG, "Gamma - " + gamma);
        // 色调
        int hue = CameraController.getInstance().getHue(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvHue.setText(String.valueOf(hue));
        cameraSettingDialogRsbHue.setProgress(Integer.valueOf(hue).floatValue());
        Log.d(LogKit.TAG, "色调 - " + hue);
        // 锐度
        int sharpness = CameraController.getInstance().getSharpness(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvSharpness.setText(String.valueOf(sharpness));
        cameraSettingDialogRsbSharpness.setProgress(Integer.valueOf(sharpness).floatValue());
        Log.d(LogKit.TAG, "锐度 - " + sharpness);
        // 饱和度
        int saturation = CameraController.getInstance().getSaturation(cameraMainFragment.getCurrentCamera());
        cameraSettingDialogTvSaturation.setText(String.valueOf(saturation));
        cameraSettingDialogRsbSaturation.setProgress(Integer.valueOf(saturation).floatValue());
        Log.d(LogKit.TAG, "饱和度 - " + saturation);
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
                    cameraSettingDialogTvBrightness.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setBrightness(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlBrightness, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlBrightness);
            }
        });
        // 对比度
        cameraSettingDialogRsbContrast.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvContrast.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setContrast(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlContrast, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlContrast);
            }
        });
        // 增益
        cameraSettingDialogRsbGain.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvGain.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setGain(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlGain, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlGain);
            }
        });
        // Gamma
        cameraSettingDialogRsbGamma.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvGamma.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setGamma(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlGamma, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlGamma);
            }
        });
        // 色调
        cameraSettingDialogRsbHue.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvHue.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setHue(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlHue, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlHue);
            }
        });
        // 锐度
        cameraSettingDialogRsbSharpness.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvSharpness.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setSharpness(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlSharpness, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlSharpness);
            }
        });
        // 饱和度
        cameraSettingDialogRsbSaturation.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (isFromUser) {
                    cameraSettingDialogTvSaturation.setText(String.valueOf(leftValue));
                    CameraController.getInstance().setSaturation(cameraMainFragment.getCurrentCamera(), Float.valueOf(leftValue).intValue());
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.hideView(viewList, cameraSettingDialogLlSaturation, View.GONE);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                ViewUtils.showView(viewList, cameraSettingDialogLlSaturation);
            }
        });
    }
}