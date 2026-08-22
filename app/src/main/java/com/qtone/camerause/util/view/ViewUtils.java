package com.qtone.camerause.util.view;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.util.handler.HandlerKit;
import com.qtone.camerause.util.theme.ThemeUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 * Created on 2018/12/21.
 *
 * @author 郑少鹏
 * @desc ViewUtils
 */
public class ViewUtils {
    /**
     * 显示视图
     *
     * @param view 视图
     */
    public static void showView(@NotNull View view) {
        int visibility = view.getVisibility();
        if ((visibility == View.GONE) || (visibility == View.INVISIBLE)) {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏视图
     *
     * @param view  视图
     * @param state 状态
     */
    public static void hideView(@NotNull View view, int state) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(state);
        }
    }

    /**
     * 视图高
     *
     * @param view 视图
     * @return 视图高
     */
    public static int viewHeight(@NotNull View view) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredHeight();
    }

    /**
     * 视图宽
     *
     * @param view 视图
     * @return 视图宽
     */
    public static int viewWidth(@NotNull View view) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredWidth();
    }

    /**
     * 设布局高
     *
     * @param view   视图
     * @param height 高
     */
    public static void setViewHeight(@NotNull View view, int height) {
        if (view.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            layoutParams.height = height;
            view.setLayoutParams(layoutParams);
        }
        if (view.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
            layoutParams.height = height;
            view.setLayoutParams(layoutParams);
        }
    }

    /**
     * 触摸指定 View 否（过滤控件）
     *
     * @param views       视图
     * @param motionEvent 手势事件
     * @return boolean
     */
    public static boolean isTouchView(View[] views, MotionEvent motionEvent) {
        if ((null == views) || (views.length == 0)) {
            return false;
        }
        int[] location = new int[2];
        for (View view : views) {
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            boolean flag = ((motionEvent.getX() > x) && (motionEvent.getX() < (x + view.getWidth())) && (motionEvent.getY() > y && motionEvent.getY() < (y + view.getHeight())));
            if (flag) {
                return true;
            }
        }
        return false;
    }

    /**
     * 触摸指定 View 否（过滤控件）
     *
     * @param appCompatActivity 活动
     * @param ids               控件数组
     * @param motionEvent       手势事件
     * @return boolean
     */
    public static boolean isTouchView(AppCompatActivity appCompatActivity, @NonNull int[] ids, MotionEvent motionEvent) {
        WeakReference<AppCompatActivity> weakReference = new WeakReference<>(appCompatActivity);
        int[] location = new int[2];
        for (int id : ids) {
            View view = weakReference.get().findViewById(id);
            if (null == view) {
                continue;
            }
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            boolean flag = ((motionEvent.getX() > x) && (motionEvent.getX() < (x + view.getWidth())) && (motionEvent.getY() > y && motionEvent.getY() < (y + view.getHeight())));
            if (flag) {
                return true;
            }
        }
        return false;
    }

    /**
     * 闪烁
     *
     * @param context     上下文
     * @param flashView   闪烁视图
     * @param delayMillis 延迟毫秒
     */
    public static void flash(Context context, View flashView, long delayMillis) {
        if (null != flashView) {
            flashView.setBackgroundColor(Color.RED);
            HandlerKit.getInstance().postDelayed(() -> flashView.setBackgroundColor(ThemeUtils.getColorSurfaceColorFromAttrResIdWithTypedArray(context)), delayMillis);
        }
    }
}