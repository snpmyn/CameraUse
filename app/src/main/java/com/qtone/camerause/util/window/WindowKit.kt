package com.qtone.camerause.util.window

import android.view.Window

/**
 * Created on 2026/3/30.
 * @author 郑少鹏
 * @desc 窗口配套原件
 */
class WindowKit {
    companion object {
        /**
         * 设置背景位图资源透明
         *
         * @param window 窗口
         */
        fun setBackgroundDrawableResourceTransparent(window: Window?) {
            // 去掉背景
            // 默认白底圆角
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}