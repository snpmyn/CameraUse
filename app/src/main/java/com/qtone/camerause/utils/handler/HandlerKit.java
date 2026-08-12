package com.qtone.camerause.utils.handler;

import android.os.Handler;
import android.os.Looper;

/**
 * Created on 2025/8/25.
 *
 * @author 郑少鹏
 * @desc Handler 配套元件
 */
public class HandlerKit extends Handler {
    /**
     * constructor
     * <p>
     * Use the provided {@link Looper} instead of the default one.
     */
    public HandlerKit() {
        super(Looper.getMainLooper());
    }

    public static HandlerKit getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final class InstanceHolder {
        static final HandlerKit INSTANCE = new HandlerKit();
    }
}