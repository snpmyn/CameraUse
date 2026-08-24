package com.qtone.camerause.util.app;

import android.content.Context;
import android.content.Intent;

import com.qtone.camerause.util.activity.ActivitySuperviseManager;
import com.qtone.camerause.util.click.DoubleClickKit;

import timber.log.Timber;

/**
 * Created on 2026/6/10.
 *
 * @author 郑少鹏
 * @desc APP 配套原件
 */
public class AppKit {
    public static AppKit getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 启动 APP
     *
     * @param context           上下文
     * @param targetPackageName 目标包名
     */
    public void launchApp(Context context, String targetPackageName) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackageName);
            if (launchIntent != null) {
                // 确保是从外部全新拉起
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * 双击退出
     *
     * @param exitHint 退出提示
     */
    public void twoClickToExit(String exitHint) {
        DoubleClickKit.doubleClick(exitHint, true, this::killApp);
    }

    /**
     * 杀死 APP
     * <p>
     * 按 HOME (或者打开最近任务) 看到的是任务 (Task)
     * 不是进程
     * 即使 Activity 已经 finish
     * 进程已经被杀死
     * Android 仍然可能保留这个 Task 的缩略图
     * 方便用户再次点击恢复 (系统会重新创建进程)
     */
    public void killApp() {
        try {
            ActivitySuperviseManager.getInstance().finishAllActivity();
            // 让当前进程的虚拟机彻底退出 (传 0 表示正常退出)
            // 这会触发 JVM 的垃圾回收和底层本地库 (.so) 的 JNI_OnUnload
            // 释放 C/C++ 层的句柄
            System.exit(0);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            // 无论是何种情况挂起 (通过 Linux 内核信号量抹除本进程所有痕迹)
            // android.os.Process.myPid() 获取当前进程 PID
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private static final class InstanceHolder {
        static final AppKit INSTANCE = new AppKit();
    }
}