package com.jiangdg.ausbc.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import kotlin.system.exitProcess

/** App operator utils
 *
 * @author Created by jiangdg on 2022/3/1
 */
object AppUtils {
    @SuppressLint("UnspecifiedImmutableFlag")
    fun restartApp(ctx: Context?) {
        ctx ?: return
        val pckgManager: PackageManager = ctx.applicationContext.packageManager
        val intent: Intent? = pckgManager.getLaunchIntentForPackage(ctx.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            ctx.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT
        )
        val manager: AlarmManager =
            ctx.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent)
    }

    fun releaseAppResource() {
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    fun removeAllActivity() {
        ActivityStackUtils.popAllActivity()
    }

    fun getAppName(ctx: Context): String? {
        val packageManager: PackageManager = ctx.packageManager
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(ctx.packageName, 0)
            val labelRes: Int = packageInfo.applicationInfo.labelRes
            return ctx.getString(labelRes)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}