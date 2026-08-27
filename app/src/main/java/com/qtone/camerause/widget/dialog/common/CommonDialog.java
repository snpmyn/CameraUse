package com.qtone.camerause.widget.dialog.common;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;
import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;
import com.qtone.camerause.widget.dialog.common.listener.CommonDialogClickListener;
import com.qtone.camerause.widget.textview.AlignTextView;

/**
 * Created on 2026/4/23.
 *
 * @author 郑少鹏
 * @desc 普通对话框
 */
public class CommonDialog extends BaseLifecycleDialog {
    /**
     * 控件
     */
    private TextView commonDialogTvTitle;
    private AlignTextView commonDialogAtvContent;
    private Button commonDialogMbNegative;
    private Button commonDialogMbPositive;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 最长行居中其余行靠左
     */
    private boolean centerLongestLeftRest = false;
    /**
     * 消极文本
     */
    private String negativeText;
    /**
     * 积极文本
     */
    private String positiveText;
    /**
     * 显示消极
     */
    private boolean showNegative = false;
    /**
     * 普通对话框点击监听
     */
    private CommonDialogClickListener commonDialogClickListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public CommonDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.dialog_common;
    }

    /**
     * 初始化控件
     */
    @Override
    protected void initView() {
        commonDialogTvTitle = findViewById(R.id.commonDialogTvTitle);
        commonDialogAtvContent = findViewById(R.id.commonDialogAtvContent);
        commonDialogMbNegative = findViewById(R.id.commonDialogMbNegative);
        commonDialogMbPositive = findViewById(R.id.commonDialogMbPositive);
    }

    /**
     * 初始化数据
     */
    @Override
    protected void initData() {
        // 标题
        if (!TextUtils.isEmpty(title)) {
            commonDialogTvTitle.setText(title);
            commonDialogTvTitle.setVisibility(View.VISIBLE);
        }
        // 内容
        if (!TextUtils.isEmpty(content)) {
            commonDialogAtvContent.setText(content);
            commonDialogAtvContent.setVisibility(View.VISIBLE);
        }
        // 最长行居中其余行靠左
        commonDialogAtvContent.setCenterLongestLeftRest(centerLongestLeftRest);
        // 消极
        if (showNegative) {
            commonDialogMbNegative.setText(TextUtils.isEmpty(negativeText) ? getContext().getText(R.string.cancel) : negativeText);
            commonDialogMbNegative.setVisibility(View.VISIBLE);
        }
        // 积极
        commonDialogMbPositive.setText(TextUtils.isEmpty(positiveText) ? getContext().getText(R.string.ensure) : positiveText);
    }

    /**
     * 初始化事件
     */
    @Override
    protected void initEvent() {
        commonDialogMbNegative.setOnClickListener(v -> {
            if (commonDialogClickListener != null) {
                commonDialogClickListener.onCancel(this);
            }
        });
        commonDialogMbPositive.setOnClickListener(v -> {
            if (commonDialogClickListener != null) {
                commonDialogClickListener.onConfirm(this);
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
        // 置空普通对话框点击监听
        commonDialogClickListener = null;
    }

    /**
     * 设置标题
     *
     * @param title 标题
     * @return 普通对话框
     */
    public CommonDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * 设置内容
     *
     * @param content 内容
     * @return 普通对话框
     */
    public CommonDialog setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * 设置最长行居中其余行靠左
     *
     * @param centerLongestLeftRest 最长行居中其余行靠左
     * @return 普通对话框
     */
    public CommonDialog setCenterLongestLeftRest(boolean centerLongestLeftRest) {
        this.centerLongestLeftRest = centerLongestLeftRest;
        return this;
    }

    /**
     * 设置消极文本
     *
     * @param negativeText 消极文本
     * @return 普通对话框
     */
    public CommonDialog setNegativeText(String negativeText) {
        this.negativeText = negativeText;
        return this;
    }

    /**
     * 设置积极文本
     *
     * @param positiveText 积极文本
     * @return 普通对话框
     */
    public CommonDialog setPositiveText(String positiveText) {
        this.positiveText = positiveText;
        return this;
    }

    /**
     * 设置显示消极
     *
     * @param showNegative 显示消极
     * @return 普通对话框
     */
    public CommonDialog setShowNegative(boolean showNegative) {
        this.showNegative = showNegative;
        return this;
    }

    /**
     * 设置普通对话框点击监听
     *
     * @param commonDialogClickListener 普通对话框点击监听
     * @return 普通对话框
     */
    public CommonDialog setCommonDialogClickListener(CommonDialogClickListener commonDialogClickListener) {
        this.commonDialogClickListener = commonDialogClickListener;
        return this;
    }
}