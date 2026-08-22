package com.qtone.camerause.util.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;

/**
 * Created on 2025/9/11.
 *
 * @author 郑少鹏
 * @desc 主题工具类
 */
public class ThemeUtils {
    /**
     * 从 attr 资源 ID 获取颜色
     *
     * @param context   上下文
     * @param attrResId attr 资源 ID
     * @return 颜色
     */
    @ColorInt
    public static int getColorFromAttrResIdWithMaterialColors(@NonNull Context context, @AttrRes int attrResId) {
        return MaterialColors.getColor(context, attrResId, 0);
    }

    /**
     * 从 attr 资源 ID 获取 ColorPrimary 颜色
     *
     * @param context 上下文
     * @return 颜色
     */
    @ColorInt
    public static int getColorPrimaryColorFromAttrResIdWithTypedArray(@NonNull Context context) {
        return getColorFromAttrResIdWithTypedArray(context, androidx.appcompat.R.attr.colorPrimary);
    }

    /**
     * 从 attr 资源 ID 获取 ColorSurface 颜色
     *
     * @param context 上下文
     * @return 颜色
     */
    @ColorInt
    public static int getColorSurfaceColorFromAttrResIdWithTypedArray(@NonNull Context context) {
        return getColorFromAttrResIdWithTypedArray(context, com.google.android.material.R.attr.colorSurface);
    }

    /**
     * 从 attr 资源 ID 获取 ColorOnSurface 颜色
     *
     * @param context 上下文
     * @return 颜色
     */
    @ColorInt
    public static int getColorOnSurfaceColorFromAttrResIdWithTypedArray(@NonNull Context context) {
        return getColorFromAttrResIdWithTypedArray(context, com.google.android.material.R.attr.colorOnSurface);
    }

    /**
     * 从 attr 资源 ID 获取 ColorSecondaryContainer 颜色
     *
     * @param context 上下文
     * @return 颜色
     */
    /*@ColorInt
    public static int getColorSecondaryContainerColorFromAttrResIdWithTypedArray(@NonNull Context context) {
        return getColorFromAttrResIdWithTypedArray(context, com.google.android.material.R.attr.colorSecondaryContainer);
    }*/

    /**
     * 从 attr 资源 ID 获取颜色
     *
     * @param context   上下文
     * @param attrResId attr 资源 ID
     * @return 颜色
     */
    @ColorInt
    private static int getColorFromAttrResIdWithTypedArray(@NonNull Context context, @AttrRes int attrResId) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrResId});
        try {
            return typedArray.getColor(0, 0);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * 从 attr 资源 ID 获取位图
     *
     * @param context   上下文
     * @param attrResId attr 资源 ID
     * @return 位图
     */
    public static Drawable getDrawableFromAttrResId(@NonNull Context context, @AttrRes int attrResId) {
        int[] attrs = new int[]{attrResId};
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        try {
            return typedArray.getDrawable(0);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * 从 attr 资源 ID 获取资源 ID
     *
     * @param context   上下文
     * @param attrResId attr 资源 ID
     * @return 资源 ID
     */
    public static int getResourceIdFromAttrResId(@NonNull Context context, @AttrRes int attrResId) {
        int[] attrs = new int[]{attrResId};
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        try {
            return typedArray.getResourceId(0, 0);
        } finally {
            typedArray.recycle();
        }
    }
}