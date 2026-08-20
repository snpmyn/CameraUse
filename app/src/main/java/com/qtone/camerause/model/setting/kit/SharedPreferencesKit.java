package com.qtone.camerause.model.setting.kit;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/20.
 *
 * @author 郑少鹏
 * @desc SharedPreferences 配套原件
 */
public class SharedPreferencesKit {
    /**
     * 标识
     */
    public static final String KEY_ROI_OVERLAY = "roi_overlay";
    public static final String KEY_ROI_CROP = "roi_crop";
    public static final String KEY_BAIDU_OCR = "baidu_ocr";
    public static final String KEY_DOCUMENT_CROP = "document_crop";
    public static final String KEY_WECHAT_CROP = "wechat_crop";
    /**
     * 默认值
     * <p>
     * 同 XML 中 defaultValue 一致
     */
    private static final boolean DEFAULT_ROI_OVERLAY = false;
    private static final boolean DEFAULT_ROI_CROP = false;
    private static final boolean DEFAULT_BAIDU_OCR = false;
    private static final boolean DEFAULT_DOCUMENT_CROP = false;
    private static final boolean DEFAULT_WECHAT_CROP = false;

    /**
     * 获取 SharedPreferences
     *
     * @param context 上下文
     * @return SharedPreferences
     */
    private static SharedPreferences getSharedPreferences(@NotNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * 是否允许 ROI 覆盖
     *
     * @param context 上下文
     * @return 是否允许 ROI 覆盖
     */
    public static boolean isRoiOverlayEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_ROI_OVERLAY, DEFAULT_ROI_OVERLAY);
    }

    /**
     * 是否允许 ROI 裁剪
     *
     * @param context 上下文
     * @return 是否允许 ROI 裁剪
     */
    public static boolean isRoiCropEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_ROI_CROP, DEFAULT_ROI_CROP);
    }

    /**
     * 是否允许百度 OCR
     *
     * @param context 上下文
     * @return 是否允许百度 OCR
     */
    public static boolean isBaiduOcrEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_BAIDU_OCR, DEFAULT_BAIDU_OCR);
    }

    /**
     * 是否允许文档裁剪
     *
     * @param context 上下文
     * @return 是否允许文档裁剪
     */
    public static boolean isDocumentCropEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_DOCUMENT_CROP, DEFAULT_DOCUMENT_CROP);
    }

    /**
     * 是否允许微信裁剪
     *
     * @param context 上下文
     * @return 是否允许微信裁剪
     */
    public static boolean isWechatCropEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_WECHAT_CROP, DEFAULT_WECHAT_CROP);
    }
}