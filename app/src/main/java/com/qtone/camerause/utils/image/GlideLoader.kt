package com.qtone.camerause.utils.image

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.qtone.camerause.R
import java.util.*

/**
 * Glide 加载器
 *
 * @param target imageview owner
 * @author Created by jiangdg on 2022/3/16
 */
class GlideLoader<T>(target: T) : ILoader<ImageView> {
    private var requestManager: RequestManager? = null

    init {
        requestManager = when (target) {
            is Fragment -> Glide.with(target)
            is FragmentActivity -> Glide.with(target)
            is Activity -> Glide.with(target)
            is Context -> Glide.with(target)
            is View -> Glide.with(target)
            else -> throw IllegalArgumentException()
        }
    }

    override fun load(imageView: ImageView, url: String?, placeHolder: Int) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        requestManager!!.load(url).optionalTransform(centerCrop)
            .placeholder(placeHolder)
            .into(imageView)
    }

    override fun load(imageView: ImageView, url: String?) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        requestManager!!.load(url).optionalTransform(centerCrop)
            .placeholder(R.drawable.color_d7dae1_solid)
            .into(imageView)
    }

    override fun load(imageView: ImageView, resId: Int) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        requestManager!!.load(resId).optionalTransform(centerCrop)
            .placeholder(R.drawable.color_d7dae1_solid)
            .into(imageView)
    }

    override fun load(
        imageView: ImageView,
        url: String?,
        placeHolder: Int,
        bitmapTransformation: BitmapTransformation?
    ) {
        var request = requestManager!!.load(url)
        if (bitmapTransformation != null) {
            request = request.optionalTransform(bitmapTransformation)
        }
        request.placeholder(placeHolder).into(imageView)
    }

    @SuppressLint("CheckResult")
    override fun loadRounded(imageView: ImageView, url: String?, placeHolder: Int, radius: Float) {
        RequestOptions().apply {
            if (radius >= 0) {
                transform(CenterCrop(), RoundedCorners(dp2px(imageView.context, radius)))
            } else {
                transform(RoundedCorners(dp2px(imageView.context, radius)))
            }
        }.also { options ->
            requestManager!!.load(url)
                .placeholder(placeHolder)
                .apply(options)
                .into(imageView)
        }
    }

    @SuppressLint("CheckResult")
    override fun loadRounded(
        imageView: ImageView,
        url: String?,
        placeHolder: Drawable?,
        radius: Float
    ) {
        RequestOptions().apply {
            if (radius >= 0) {
                transform(CenterCrop(), RoundedCorners(dp2px(imageView.context, radius)))
            } else {
                transform(RoundedCorners(dp2px(imageView.context, radius)))
            }
        }.also { options ->
            requestManager!!.load(url)
                .placeholder(placeHolder)
                .apply(options)
                .into(imageView)
        }
    }

    override fun loadRounded(imageView: ImageView, url: String?, radius: Float) {
        loadRounded(imageView, url, R.drawable.color_d7dae1_solid, radius)
    }

    override fun loadCircle(imageView: ImageView, url: String?, placeHolder: Int) {
        requestManager?.apply {
            this.load(url)
                .placeholder(placeHolder)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, url: String?) {
        requestManager?.apply {
            this.load(url)
                .placeholder(R.drawable.color_d7dae1_solid)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, resId: Int, placeHolder: Int) {
        requestManager?.apply {
            this.load(resId)
                .placeholder(placeHolder)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, resId: Int) {
        requestManager?.apply {
            this.load(resId)
                .placeholder(R.drawable.color_d7dae1_solid)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadAsBitmap(
        url: String?,
        width: Int,
        height: Int,
        listener: ILoader.OnLoadedResultListener
    ) {
        requestManager?.apply {
            this.asBitmap()
                .centerCrop()
                .load(url)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onLoadedFailed(e)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onLoadedSuccess(resource)
                        return true
                    }
                })
                .submit(width, height)
        }
    }

    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    companion object {
        // 使用弱引用 HashMap 缓存，组件销毁时 Key 自动被垃圾回收，解决重复创建且绝不泄露。
        private val loaderCache = WeakHashMap<Any, GlideLoader<*>>()

        @JvmStatic
        fun <T : Any> with(target: T): GlideLoader<T> {
            @Suppress("UNCHECKED_CAST")
            return loaderCache.getOrPut(target) {
                GlideLoader(target)
            } as GlideLoader<T>
        }
    }
}