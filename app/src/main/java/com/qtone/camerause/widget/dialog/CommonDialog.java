package com.qtone.camerause.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;
import com.qtone.camerause.util.density.DensityUtils;
import com.qtone.camerause.util.window.WindowKit;

/**
 * Created on 2026/4/23.
 *
 * @author 郑少鹏
 * @desc 普通对话框
 */
public class CommonDialog extends Dialog {
    /**
     * 控件
     */
    private TextView commonDialogTvTitle;
    private TextView commonDialogTvContent;
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
     * 内容位置
     */
    private int contentGravity;
    /**
     * 内容左内边距
     */
    private int contentPaddingStart;
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
     * 对话框点击监听
     */
    private OnDialogClickListener onDialogClickListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public CommonDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_common);
        // 设置背景位图资源透明
        WindowKit.Companion.setBackgroundDrawableResourceTransparent(getWindow());
        // 初始化控件
        initView();
        // 初始化数据
        initData();
        // 初始化事件
        initEvent();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        commonDialogTvTitle = findViewById(R.id.commonDialogTvTitle);
        commonDialogTvContent = findViewById(R.id.commonDialogTvContent);
        commonDialogMbNegative = findViewById(R.id.commonDialogMbNegative);
        commonDialogMbPositive = findViewById(R.id.commonDialogMbPositive);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 标题
        if (!TextUtils.isEmpty(title)) {
            commonDialogTvTitle.setText(title);
            commonDialogTvTitle.setVisibility(View.VISIBLE);
        }
        // 内容
        if (!TextUtils.isEmpty(content)) {
            commonDialogTvContent.setText(content);
            commonDialogTvContent.setVisibility(View.VISIBLE);
        }
        // 内容位置
        if (contentGravity != 0) {
            commonDialogTvContent.setGravity(contentGravity);
        }
        // 内容左内边距
        if (contentPaddingStart > 0) {
            commonDialogTvContent.setPaddingRelative(
                    contentPaddingStart,
                    commonDialogTvContent.getPaddingTop(),
                    commonDialogTvContent.getPaddingEnd(),
                    commonDialogTvContent.getPaddingBottom()
            );
        }
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
    private void initEvent() {
        commonDialogMbNegative.setOnClickListener(v -> {
            dismiss();
            if (onDialogClickListener != null) {
                onDialogClickListener.onCancel();
            }
        });
        commonDialogMbPositive.setOnClickListener(v -> {
            dismiss();
            if (onDialogClickListener != null) {
                onDialogClickListener.onConfirm();
            }
        });
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
     * 设置内容位置
     *
     * @param contentGravity 内容位置
     * @return 普通对话框
     */
    public CommonDialog setContentGravity(int contentGravity) {
        this.contentGravity = contentGravity;
        return this;
    }

    /**
     * 设置内容左内边距
     *
     * @param contentPaddingStart 内容左内边距
     * @return 普通对话框
     */
    public CommonDialog contentPaddingStart(int contentPaddingStart) {
        this.contentPaddingStart = DensityUtils.dipToPxByInt(contentPaddingStart);
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
     * 设置对话框点击监听
     *
     * @param onDialogClickListener 对话框点击监听
     * @return 普通对话框
     */
    public CommonDialog setOnDialogClickListener(OnDialogClickListener onDialogClickListener) {
        this.onDialogClickListener = onDialogClickListener;
        return this;
    }

    /**
     * 对话框点击监听
     */
    public interface OnDialogClickListener {
        /**
         * 取消
         */
        default void onCancel() {
        }

        /**
         * 确认
         */
        default void onConfirm() {
        }
    }
}