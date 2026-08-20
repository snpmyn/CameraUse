package com.qtone.camerause.model.setting;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.R;
import com.qtone.camerause.model.setting.fragment.SettingFragment;

/**
 * @decs: 设置页
 * @author: 郑少鹏
 * @date: 2026/8/19 17:42
 * @version: v 1.0
 */
public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getSupportFragmentManager().beginTransaction().replace(R.id.settingActivityFl, new SettingFragment()).commit();
    }
}