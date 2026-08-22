package com.qtone.camerause.util.intent;

import android.content.Intent;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

import okio.ByteString;

/**
 * Created on 2021/1/4
 *
 * @author zsp
 * @desc 意图核实
 */
@SuppressWarnings("unused")
public class IntentVerify {
    private final static String KEY = ByteString.encodeUtf8("IntentVerify").md5().hex();

    /**
     * 是否是坏意图
     *
     * @param intent 意图
     * @return 是否是坏意图
     */
    public static boolean badIntent(Intent intent) {
        if (null == intent) {
            return false;
        }
        try {
            intent.putExtra(KEY, "c");
            intent.removeExtra(KEY);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取 int 类型额外信息
     *
     * @param intent       意图
     * @param key          键
     * @param defaultValue 默值
     * @return int 类型额外信息
     */
    public static int getIntExtra(Intent intent, String key, int defaultValue) {
        if (badIntent(intent)) {
            return 0;
        }
        return intent.getIntExtra(key, defaultValue);
    }

    /**
     * 获取 double 类型额外信息
     *
     * @param intent       意图
     * @param key          键
     * @param defaultValue 默值
     * @return double 类型额外信息
     */
    public static double getDoubleExtra(Intent intent, String key, double defaultValue) {
        if (badIntent(intent)) {
            return 0.0;
        }
        return intent.getDoubleExtra(key, defaultValue);
    }

    /**
     * 获取 String 类型额外信息
     *
     * @param intent 意图
     * @param key    键
     * @return String 类型额外信息
     */
    @Nullable
    public static String getStringExtra(Intent intent, String key) {
        if (badIntent(intent)) {
            return null;
        }
        return intent.getStringExtra(key);
    }

    /**
     * 获取 Parcelable 类型额外信息
     *
     * @param intent 意图
     * @param key    键
     * @return Parcelable 类型额外信息
     */
    @Nullable
    public static <T extends Parcelable> T getParcelableExtra(Intent intent, String key) {
        if (badIntent(intent)) {
            return null;
        }
        return intent.getParcelableExtra(key);
    }

    /**
     * 获取 Parcelable Array 类型额外信息
     *
     * @param intent 意图
     * @param key    键
     * @return Parcelable Array 类型额外信息
     */
    @Nullable
    public static Parcelable[] getParcelableArrayExtra(Intent intent, String key) {
        if (badIntent(intent)) {
            return null;
        }
        return intent.getParcelableArrayExtra(key);
    }

    /**
     * 获取 Parcelable ArrayList 类型额外信息
     *
     * @param intent 意图
     * @param key    键
     * @param <T>    <T>
     * @return Parcelable ArrayList 类型额外信息
     */
    @Nullable
    public static <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(Intent intent, String key) {
        if (badIntent(intent)) {
            return null;
        }
        return intent.getParcelableArrayListExtra(key);
    }

    /**
     * 获取 Serializable 类型额外信息
     *
     * @param intent 意图
     * @param key    键
     * @return Serializable 类型额外信息
     */
    @Nullable
    public static Serializable getSerializableExtra(Intent intent, String key) {
        if (badIntent(intent)) {
            return null;
        }
        return intent.getSerializableExtra(key);
    }
}