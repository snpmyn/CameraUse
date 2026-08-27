package com.qtone.camerause.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.qtone.camerause.util.window.WindowKit;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 生命周期对话框基类
 */
public abstract class BaseLifecycleDialog extends Dialog {
    /**
     * 资源是否已清理
     */
    private boolean isResourceCleared = false;
    /**
     * 默认生命周期观察者
     */
    private final DefaultLifecycleObserver defaultLifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onDestroy(@NonNull @NotNull LifecycleOwner owner) {
            if (isShowing()) {
                dismiss();
            }
            // 清理资源
            clearResource();
            // 移除观察者
            owner.getLifecycle().removeObserver(this);
        }
    };

    /**
     * constructor
     *
     * @param context 上下文
     */
    public BaseLifecycleDialog(@NonNull Context context) {
        super(context);
        initLifecycle(context);
    }

    /**
     * constructor
     *
     * @param context    上下文
     * @param themeResId 主题资源 ID
     */
    public BaseLifecycleDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initLifecycle(context);
    }

    /**
     * 初始化生命周期
     *
     * @param context 上下文
     */
    private void initLifecycle(Context context) {
        LifecycleOwner lifecycleOwner = getLifecycleOwner(context);
        if (lifecycleOwner != null) {
            lifecycleOwner.getLifecycle().addObserver(defaultLifecycleObserver);
        }
    }

    /**
     * 获取生命周期所有者
     *
     * @param context 上下文
     * @return 生命周期所有者
     */
    @Nullable
    private LifecycleOwner getLifecycleOwner(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof LifecycleOwner) {
                return (LifecycleOwner) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getLayoutId());
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
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @LayoutRes
    protected abstract int getLayoutId();

    /**
     * 初始化控件
     */
    protected abstract void initView();

    /**
     * 初始化数据
     */
    protected abstract void initData();

    /**
     * 初始化事件
     */
    protected abstract void initEvent();

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 清理资源
        clearResource();
    }

    /**
     * 清理资源
     */
    private void clearResource() {
        if (!isResourceCleared) {
            isResourceCleared = true;
            onClearResource();
        }
    }

    /**
     * 清理资源
     * <p>
     * 对话框从 Window 移除触发
     */
    protected void onClearResource() {

    }

    /**
     * 获取活动
     *
     * @return 活动
     */
    @Nullable
    public AppCompatActivity getAppCompatActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}