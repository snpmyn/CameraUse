package com.qtone.camerause.util.preference;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2026/8/24.
 *
 * @author 郑少鹏
 * @desc preference 配套原件
 */
public class PreferenceKit {
    /**
     * constructor
     * <p>
     * private - 防外部实例化
     * throw - 防内部误调用
     */
    private PreferenceKit() {
        throw new UnsupportedOperationException("PreferenceKit class instantiation is forbidden!");
    }

    /**
     * 优化 RecyclerView
     *
     * @param preferenceFragmentCompat PreferenceFragmentCompat
     */
    public static void optimizeRecyclerView(@NonNull PreferenceFragmentCompat preferenceFragmentCompat) {
        RecyclerView recyclerView = preferenceFragmentCompat.getListView();
        if (recyclerView != null) {
            // 隐藏垂直滑动条
            recyclerView.setVerticalScrollBarEnabled(false);
            // 隐藏水平滑动条
            recyclerView.setHorizontalScrollBarEnabled(false);
            // 禁用拉到底部的波纹 / 阴影效果
            /*recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);*/
        }
    }
}