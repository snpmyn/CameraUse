package com.qtone.camerause.util.datetime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/9/21.
 *
 * @author 郑少鹏
 * @desc CurrentTimeMillisClock
 * 调 {@link CurrentTimeMillisClock#getInstance()#now()}
 * 调 {@link CurrentTimeMillisClock#getInstance()#shutdown()}
 * <p>
 * 用在客户端内部跟网络无关的高频触发场景
 * 场景一
 * 自定义相机 / 红外扫描头的 Camera 逐帧数据回调解析 (onPreviewFrame 每秒 30-60 帧高频计算)
 * 场景二
 * 扫描核心算法 (条形码、二维码识别核心 Loop 里的高频耗时统计)
 */
public class CurrentTimeMillisClock {
    private volatile long now;
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * constructor
     */
    private CurrentTimeMillisClock() {
        this.now = System.currentTimeMillis();
        startScheduler();
    }

    /**
     * 单例方式
     *
     * @return CurrentTimeMillisClock
     */
    public static CurrentTimeMillisClock getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 启动调度器
     */
    private void startScheduler() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "current-time-millis");
            thread.setDaemon(true);
            return thread;
        });
        scheduledExecutorService.scheduleWithFixedDelay(() -> now = System.currentTimeMillis(), 1, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public long now() {
        return now;
    }

    /**
     * 停止定时器
     */
    public void shutdown() {
        synchronized (this) {
            if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
                try {
                    // 停止新的任务执行，并等待当前的任务完成。
                    scheduledExecutorService.shutdown();
                    if (!scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        scheduledExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    // 在等待过程中被中断时，强制关闭。
                    scheduledExecutorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 重新启动定时任务
     */
    public void restartScheduler() {
        if (scheduledExecutorService != null && scheduledExecutorService.isShutdown()) {
            startScheduler();
        }
    }

    /**
     * 单例持有者
     */
    private static class SingletonHolder {
        private static final CurrentTimeMillisClock INSTANCE = new CurrentTimeMillisClock();
    }
}