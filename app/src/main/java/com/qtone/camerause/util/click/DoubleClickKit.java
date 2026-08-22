package com.qtone.camerause.util.click;

import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.util.datetime.CurrentTimeMillisClock;

/**
 * Created on 2026/6/10.
 *
 * @author 郑少鹏
 * @desc 双击配套原件
 */
public class DoubleClickKit {
    /**
     * 等待时间
     */
    private static final long WAIT_TIME = 2000;
    /**
     * 上次点击时间
     */
    private static long lastClickTime = 0;

    /**
     * 双击
     *
     * @param firstClickHint     第一次点击提示
     * @param firstClickNeedHint 第一次点击是否需要提示
     * @param secondClickAction  第二次点击动作
     */
    public static void doubleClick(String firstClickHint, boolean firstClickNeedHint, Runnable secondClickAction) {
        long currentTime = CurrentTimeMillisClock.getInstance().now();
        if (currentTime - lastClickTime < WAIT_TIME) {
            secondClickAction.run();
        } else {
            lastClickTime = currentTime;
            if (firstClickNeedHint) {
                ToastUtils.show(firstClickHint);
            }
        }
    }
}