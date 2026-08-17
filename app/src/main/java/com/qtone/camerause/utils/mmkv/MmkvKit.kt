package com.qtone.camerause.utils.mmkv

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV

/**
 * MMKV 配套原件
 *
 * @author Created by jiangdg on 2022/3/30
 */
object MmkvKit {
    private val mKv: MMKV by lazy {
        MMKV.defaultMMKV()
    }

    /**
     * Init MMKV
     *
     * @param context
     */
    fun init(context: Context) {
        MMKV.initialize(context.applicationContext)
    }

    /**
     * save value to SharedPreference
     *
     * @param key key
     * @param value value, such as Int、String、Boolean...etc.
     */
    fun set(key: String, value: Any) {
        when (value) {
            is String -> mKv.encode(key, value)
            is Int -> mKv.encode(key, value)
            is Double -> mKv.encode(key, value)
            is Float -> mKv.encode(key, value)
            is Boolean -> mKv.encode(key, value)
            is Long -> mKv.encode(key, value)
            is ByteArray -> mKv.encode(key, value)
            is Parcelable -> mKv.encode(key, value)
            else -> throw IllegalStateException("unsupported value type")
        }
    }

    /**
     * get sharedPreference string value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference string value
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return mKv.decodeString(key, defaultValue)
    }

    /**
     * get sharedPreference Int value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Int value
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return mKv.decodeInt(key, defaultValue)
    }

    /**
     * get sharedPreference Long value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Long value
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return mKv.decodeLong(key, defaultValue)
    }

    /**
     * get sharedPreference Double value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Double value
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return mKv.decodeDouble(key, defaultValue)
    }

    /**
     * get sharedPreference Float value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Float value
     */
    fun getFloat(key: String, defaultValue: Float = 0F): Float {
        return mKv.decodeFloat(key, defaultValue)
    }

    /**
     * get sharedPreference Boolean value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Boolean value
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return mKv.decodeBool(key, defaultValue)
    }

    /**
     * get sharedPreference ByteArray value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference ByteArray value
     */
    fun getByteArray(key: String, defaultValue: ByteArray? = null): ByteArray? {
        return mKv.decodeBytes(key, defaultValue)
    }

    /**
     * get sharedPreference Parcelable value
     *
     * @param key key
     * @param clz Parcelable class
     * @return sharedPreference Parcelable value
     */
    fun <T : Parcelable> getParcelable(key: String, clz: Class<T>): T? {
        return mKv.decodeParcelable(key, clz)
    }
}