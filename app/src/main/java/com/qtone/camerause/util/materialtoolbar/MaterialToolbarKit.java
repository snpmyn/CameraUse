package com.qtone.camerause.util.materialtoolbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.qtone.camerause.util.density.DensityUtils;

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
     * @param context         上下文
     * @param materialToolbar 材料工具栏
     * @param tintColorResId  色调颜色资源 ID
     */
    public void setMenuOverflowIconTintColor(@NonNull Context context, @NonNull MaterialToolbar materialToolbar, @ColorRes int tintColorResId) {
        Drawable drawable = materialToolbar.getOverflowIcon();
        if (null != drawable) {
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable.mutate());
            wrappedDrawable.setTint(ContextCompat.getColor(context, tintColorResId));
            materialToolbar.setOverflowIcon(wrappedDrawable);
        }
    }

    /**
     * 设置菜单溢出图标
     *
     * @param context         上下文
     * @param materialToolbar 材料工具栏
     * @param iconResId       图标资源 ID
     */
    public void setMenuOverflowIcon(@NonNull Context context, @NonNull MaterialToolbar materialToolbar, @DrawableRes int iconResId) {
        Drawable drawable = ContextCompat.getDrawable(context, iconResId);
        if (null != drawable) {
            materialToolbar.setOverflowIcon(drawable.mutate());
        }
    }

    /**
     * 设置菜单溢出图标尺寸
     *
     * @param context         上下文
     * @param materialToolbar 材料工具栏
     * @param sizeInDpOrRes   尺寸 DP 值或尺寸资源 ID
     */
    public void setMenuOverflowIconSize(@NonNull Context context, @NonNull MaterialToolbar materialToolbar, int sizeInDpOrRes) {
        Drawable overflowIcon = materialToolbar.getOverflowIcon();
        if (null == overflowIcon) {
            return;
        }
        int sizeInPx;
        try {
            // 首先当作资源 ID 读取
            sizeInPx = context.getResources().getDimensionPixelSize(sizeInDpOrRes);
        } catch (Resources.NotFoundException e) {
            // 非资源 ID 时当作纯 dp 值计算
            sizeInPx = DensityUtils.dipToPxByInt(sizeInDpOrRes);
        }
        if (sizeInPx <= 0) {
            return;
        }
        // 使用 mutate() 隔离 Drawable 状态
        // 避免影响全局
        Drawable mutableDrawable = overflowIcon.mutate();
        Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mutableDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        mutableDrawable.draw(canvas);
        Drawable resizedDrawable = new BitmapDrawable(context.getResources(), bitmap);
        Drawable wrappedDrawable = DrawableCompat.wrap(resizedDrawable);
        materialToolbar.setOverflowIcon(wrappedDrawable);
    }

    /**
     * 设置菜单条目颜色
     * <p>
     * 图标颜色
     * 文字颜色
     *
     * @param context         上下文
     * @param materialToolbar 材料工具栏
     * @param colorResId      颜色资源 ID
     */
    public void setMenuItemColor(Context context, MaterialToolbar materialToolbar, @ColorRes int colorResId) {
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