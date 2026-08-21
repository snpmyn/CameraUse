package com.qtone.camerause.model.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2025/9/27.
 *
 * @author 郑少鹏
 * @desc 设置碎片
 */
public class SettingFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 获取 PreferenceFragmentCompat 底层 RecyclerView
        RecyclerView recyclerView = getListView();
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