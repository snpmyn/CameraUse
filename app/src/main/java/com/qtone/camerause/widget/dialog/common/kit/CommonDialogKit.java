package com.qtone.camerause.widget.dialog.common.kit;

import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.R;
import com.qtone.camerause.widget.dialog.common.CommonDialog;
import com.qtone.camerause.widget.dialog.common.listener.CommonDialogClickListener;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created on 2026/8/25.
 *
 * @author 郑少鹏
 * @desc 普通对话框配套原件
 */
public class CommonDialogKit {
    /**
     * WeakHashMap
     */
    private static final WeakHashMap<CommonDialog, Boolean> DISMISSED_BY_REPLACE_MAP = new WeakHashMap<>(1);
    /**
     * WeakReference
     */
    private static WeakReference<CommonDialog> commonDialogWeakReference;

    /**
     * 显示信息对话框
     *
     * @param appCompatActivity     活动
     * @param content               内容
     * @param centerLongestLeftRest 最长行居中其余行靠左
     * @param cancelable            可取消否
     * @param runnable              可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String content, boolean centerLongestLeftRest, boolean cancelable, Runnable runnable) {
        showInfoDialog(appCompatActivity, appCompatActivity.getString(R.string.notice), content, centerLongestLeftRest, appCompatActivity.getString(R.string.iKonw), cancelable, runnable);
    }

    /**
     * 显示信息对话框
     *
     * @param appCompatActivity     活动
     * @param title                 标题
     * @param content               内容
     * @param centerLongestLeftRest 最长行居中其余行靠左
     * @param positiveText          积极文本
     * @param cancelable            可取消否
     * @param runnable              可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String title, String content, boolean centerLongestLeftRest, String positiveText, boolean cancelable, Runnable runnable) {
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed()) {
            return;
        }
        if (commonDialogWeakReference != null) {
            CommonDialog oldCommonDialog = commonDialogWeakReference.get();
            if ((oldCommonDialog != null) && oldCommonDialog.isShowing()) {
                DISMISSED_BY_REPLACE_MAP.put(oldCommonDialog, Boolean.TRUE);
                oldCommonDialog.dismiss();
            }
            commonDialogWeakReference.clear();
            commonDialogWeakReference = null;
        }
        CommonDialog commonDialog = new CommonDialog(appCompatActivity);
        commonDialog.setCancelable(cancelable);
        commonDialog.setCanceledOnTouchOutside(cancelable);
        commonDialog.setTitle(title)
                .setContent(content)
                .setCenterLongestLeftRest(centerLongestLeftRest)
                .setPositiveText(positiveText)
                .setCommonDialogClickListener(new CommonDialogClickListener() {
                    @Override
                    public void onConfirm(CommonDialog commonDialog) {
                        commonDialog.dismiss();
                    }
                });
        commonDialogWeakReference = new WeakReference<>(commonDialog);
        commonDialog.setOnDismissListener(dialog -> {
            if ((commonDialogWeakReference != null) && (commonDialogWeakReference.get() == commonDialog)) {
                commonDialogWeakReference.clear();
                commonDialogWeakReference = null;
            }
            // 从 DISMISSED_BY_REPLACE_MAP 删除 commonDialog 对应标记记录
            // 同时取出删掉的值并保存到 dismissedByReplace 变量
            // 不存在值则返 null
            // 即 dismissedByReplace = null
            Boolean dismissedByReplace = DISMISSED_BY_REPLACE_MAP.remove(commonDialog);
            if (!Boolean.TRUE.equals(dismissedByReplace) && (runnable != null)) {
                runnable.run();
            }
        });
        commonDialog.show();
    }
}