package com.qtone.camerause.model.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.qtone.camerause.R;
import com.qtone.camerause.util.preference.PreferenceKit;

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
        // 优化 RecyclerView
        PreferenceKit.optimizeRecyclerView(this);
    }
}