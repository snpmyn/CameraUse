package com.qtone.camerause.util.rxbus;

import com.qtone.camerause.util.rxbus.thread.ThreadEnforcer;

/**
 * @decs: RxBus
 * Instance of {@link Bus}.
 * Simply use {@link #get()} to get the instance of {@link Bus}.
 * @author: 郑少鹏
 * @date: 2019/8/28 11:26
 */
public class RxBus {
    /**
     * Instance of {@link Bus}.
     */
    private static Bus mBus;

    /**
     * Get the instance of {@link Bus}.
     *
     * @return Bus
     */
    public static synchronized Bus get() {
        if (null == mBus) {
            mBus = new Bus(ThreadEnforcer.ANY);
        }
        return mBus;
    }
}