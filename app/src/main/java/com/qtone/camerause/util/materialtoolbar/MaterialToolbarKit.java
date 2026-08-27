package com.qtone.camerause.util.materialtoolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Created on 2025/8/23.
 *
 * @author 郑少鹏
 * @desc 材料工具栏配套原件
 */
public class MaterialToolbarKit {
    public static MaterialToolbarKit getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 设置菜单溢出图标色调颜色
     *
     * @param materialToolbar 材料工具栏
     * @param tintColor       色调颜色
     */
    public void setMenuOverflowIconTintColor(@NonNull MaterialToolbar materialToolbar, int tintColor) {
        Drawable drawable = materialToolbar.getOverflowIcon();
        if (null != drawable) {
            drawable.setTint(tintColor);
            materialToolbar.setOverflowIcon(drawable);
        }
    }

    /**
     * 设置菜单条目颜色
     * <p>
     * 图标颜色
     * 文字颜色
     *
     * @param context         上下文
     * @param materialToolbar MaterialToolbar
     * @param colorResId      颜色资源 ID
     *                        例如 R.color.white
     */
    public void setMenuItemColor(Context context, MaterialToolbar materialToolbar, int colorResId) {
        if ((null == materialToolbar) || (null == materialToolbar.getMenu())) {
            return;
        }
        int color = ContextCompat.getColor(context, colorResId);
        for (int i = 0; i < materialToolbar.getMenu().size(); i++) {
            MenuItem menuItem = materialToolbar.getMenu().getItem(i);
            // 设置图标颜色
            if (null != menuItem.getIcon()) {
                menuItem.getIcon().setTint(color);
            }
            // 设置文字颜色
            if (null != menuItem.getTitle()) {
                SpannableString spannableString = new SpannableString(menuItem.getTitle());
                spannableString.setSpan(new ForegroundColorSpan(color), 0, spannableString.length(), 0);
                menuItem.setTitle(spannableString);
            }
        }
    }

    private static final class InstanceHolder {
        static final MaterialToolbarKit INSTANCE = new MaterialToolbarKit();
    }
}