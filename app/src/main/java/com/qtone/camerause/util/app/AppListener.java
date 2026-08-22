package com.qtone.camerause.util.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qtone.camerause.util.log.LogUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 2020-09-10
 *
 * @author zsp
 * @desc 应用监听
 */
public class AppListener {
    private final String TAG = AppListener.class.getSimpleName();
    /**
     * 回调集
     * <p>
     * HashSet 非线程安全
     * 如果应用在某些特殊情况下从不同线程（比如子线程）去注册或解绑回调
     * 可能触发 ConcurrentModificationException
     * 可将其替换为线程安全的 CopyOnWriteArraySet
     */
    private final Set<Callback> callbackSet = new CopyOnWriteArraySet<>();
    private boolean hasInitConfiguration = false;
    private boolean areForeground = false;
    private int activityCount = 0;

    public static AppListener getInstance() {
        return AppListenerHolder.APP_LISTENER;
    }

    public void initConfiguration(Application application) {
        if (hasInitConfiguration) {
            return;
        }
        hasInitConfiguration = true;
        // Application 和整个应用进程同生命周期
        // ActivityLifecycleCallbacks 也是跟着 Application 一直存在
        // 当应用进程被杀死时，Application 和回调都会一起释放。
        application.registerActivityLifecycleCallbacks(new ActivityLifecycle());
    }

    /**
     * 是否前台
     *
     * @return 是否前台
     */
    public boolean areForeground() {
        return areForeground;
    }

    /**
     * 唤醒前台
     *
     * @param areForeground 是否前台
     */
    private void notifyForeground(boolean areForeground) {
        if (this.areForeground == areForeground) {
            return;
        }
        this.areForeground = areForeground;
        for (Callback callback : callbackSet) {
            callback.onStateChange(areForeground);
        }
    }

    /**
     * 注册回调
     * <p>
     * {@link Activity} 中配对调用
     *
     * @param callback 回调
     */
    public void registerCallback(Callback callback) {
        callbackSet.add(callback);
    }

    /**
     * 反注册回调
     * <p>
     * {@link Activity} 中配对调用
     *
     * @param callback 回调
     */
    public void unregisterCallback(Callback callback) {
        callbackSet.remove(callback);
    }

    public interface Callback {
        /**
         * 状态变化
         *
         * @param areForeground 是否前台
         */
        void onStateChange(boolean areForeground);
    }

    private static final class AppListenerHolder {
        static final AppListener APP_LISTENER = new AppListener();
    }

    private class ActivityLifecycle implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // ++activityCount == 1 等同于 activityCount = activityCount + 1
            // Java 前置自增运算符
            if (++activityCount == 1) {
                notifyForeground(true);
                LogUtils.d(TAG, "唤醒前台 " + activity.getClass().getSimpleName() + " || " + activity.getPackageName());
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            if (activityCount > 0) {
                activityCount--;
            }
            if (activityCount == 0) {
                notifyForeground(false);
                LogUtils.d(TAG, "回到后台 " + activity.getClass().getSimpleName() + " || " + activity.getPackageName());
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }
    }
}