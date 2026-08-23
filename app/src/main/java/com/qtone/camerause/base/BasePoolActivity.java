package com.qtone.camerause.base;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.qtone.camerause.util.edittext.EditTextUtils;
import com.qtone.camerause.util.keyboard.KeyboardUtils;
import com.qtone.camerause.util.rxbus.RxBus;
import com.qtone.camerause.util.view.ViewUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2021/3/9
 *
 * @author zsp
 * @desc BasePoolActivity
 * 优点：
 * 提代码可读性，显井井有条、优美；
 * 优化代码结构，降耦合度，方便修改；
 * 方便代码编写，减重复代码，加快开发。
 * <p>
 * 下抽象法子类须实现：
 * {@link #stepUi()}
 * {@link #initConfiguration()}
 * {@link #setListener()}
 * {@link #startLogic()}
 */
public abstract class BasePoolActivity extends AppCompatActivity {
    protected ViewBinding viewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册事件
        RxBus.get().register(this);
        // 加载视图
        // 优先使用 ViewBinding
        viewBinding = viewBinding();
        if (null != viewBinding) {
            setContentView(viewBinding.getRoot());
        } else {
            setContentView(layoutResId());
        }
        // 初始控件
        stepUi();
        // 初始配置
        initConfiguration();
        // 设置监听
        setListener();
        // 开始逻辑
        startLogic();
    }

    /**
     * ViewBinding
     * <p>
     * Java 动态绑定
     * Java 运行时多态
     * Java 动态分派机制
     * <p>
     * 如果子类重写 viewBinding()
     * 那么 onCreate() 中调用时会优先执行子类的方法
     *
     * @return ViewBinding
     */
    protected ViewBinding viewBinding() {
        return null;
    }

    /**
     * 布局资源 ID
     * <p>
     * Java 动态绑定
     * Java 运行时多态
     * Java 动态分派机制
     * <p>
     * 如果子类重写 layoutResId()
     * 那么 onCreate() 中调用时会优先执行子类的方法
     *
     * @return 布局资源 ID
     */
    protected int layoutResId() {
        return 0;
    }

    /**
     * 初始控件
     */
    protected abstract void stepUi();

    /**
     * 初始配置
     */
    protected abstract void initConfiguration();

    /**
     * 设置监听
     */
    protected abstract void setListener();

    /**
     * 开始逻辑
     */
    protected abstract void startLogic();

    /**
     * 传 EditText 的 ID
     * 没传入 EditText 不处理
     *
     * @return ID 数组
     */
    protected int[] hideSoftByEditViewIds() {
        return null;
    }

    /**
     * 传需过滤 View
     * 过滤后点无隐软键盘操作
     *
     * @return ID 数组
     */
    protected View[] filterViewByIds() {
        return null;
    }

    /**
     * Note: return supportActivityDelegate.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
     *
     * @param ev 手势事件
     * @return boolean
     */
    @Override
    public boolean dispatchTouchEvent(@NotNull MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (ViewUtils.isTouchView(filterViewByIds(), ev)) {
                return super.dispatchTouchEvent(ev);
            }
            if ((null == hideSoftByEditViewIds()) || (hideSoftByEditViewIds().length == 0)) {
                return super.dispatchTouchEvent(ev);
            }
            View view = getCurrentFocus();
            if (EditTextUtils.isFocusEditText(view, hideSoftByEditViewIds())) {
                if (ViewUtils.isTouchView(this, hideSoftByEditViewIds(), ev)) {
                    return super.dispatchTouchEvent(ev);
                }
                // 隐键盘
                KeyboardUtils.closeKeyboardInActivity(this);
                EditTextUtils.clearViewFocus(view, hideSoftByEditViewIds());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 反注册事件
        RxBus.get().unregister(this);
    }
}