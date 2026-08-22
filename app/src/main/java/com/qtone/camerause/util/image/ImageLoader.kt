package com.qtone.camerause.util.image

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment

/**
 * 图片加载器
 *
 * @author Created by jiangdg on 2022/3/16
 */
object ImageLoader {
    /**
     * create a glide image loader
     *
     * @param fragment target is fragment
     * @return [GlideLoader]
     */
    fun of(fragment: Fragment): ILoader<ImageView> = GlideLoader(fragment)

    /**
     * create a glide image loader
     *
     * @param activity target is activity
     * @return [GlideLoader]
     */
    fun of(activity: Activity): ILoader<ImageView> = GlideLoader(activity)

    /**
     * create a glide image loader
     *
     * @param context target is context
     * @return [GlideLoader]
     */
    fun of(context: Context): ILoader<ImageView> = GlideLoader(context)

    /**
     * create a glide image loader
     *
     * @param view target is view
     * @return [GlideLoader]
     */
    fun of(view: View): ILoader<ImageView> = GlideLoader(view)
}