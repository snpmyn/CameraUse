package com.qtone.camerause.model.gallery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;
import com.qtone.camerause.function.media.MediaScanner;
import com.qtone.camerause.utils.density.DensityUtils;
import com.qtone.camerause.utils.image.GlideLoader;
import com.qtone.camerause.utils.list.ListUtils;
import com.qtone.camerause.utils.screen.ScreenUtils;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemInnerClickListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/8/21.
 *
 * @author 郑少鹏
 * @desc 媒体图片详情适配器
 */
public class MediaImageDetailAdapter extends RecyclerView.Adapter<MediaImageDetailAdapter.ViewHolder> {
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 跨距数
     */
    private final int spanCount;
    /**
     * 总外边距
     */
    private final int totalMargin;
    /**
     * 图片条目
     */
    private final List<MediaScanner.ImageItem> imageItemList = new ArrayList<>();
    /**
     * RecyclerView 条目内部短点监听
     */
    private OnRecyclerViewOnItemInnerClickListener onRecyclerViewOnItemInnerClickListener;

    /**
     * constructor
     *
     * @param context     上下文
     * @param spanCount   跨距数
     * @param totalMargin 总外边距
     */
    public MediaImageDetailAdapter(Context context, int spanCount, int totalMargin) {
        this.context = context;
        this.spanCount = spanCount;
        this.totalMargin = DensityUtils.dipToPxByInt(totalMargin);
    }

    /**
     * 提交数据
     *
     * @param folderItem 文件夹条目
     */
    public void submitData(MediaScanner.@NotNull FolderItem folderItem) {
        this.imageItemList.clear();
        List<MediaScanner.ImageItem> imageItemList = folderItem.imageItemList;
        if (ListUtils.listIsNotEmpty(imageItemList)) {
            this.imageItemList.addAll(imageItemList);
        }
        notifyDataSetChanged();
    }

    /**
     * 设置 RecyclerView 条目内部短点监听
     *
     * @param onRecyclerViewOnItemInnerClickListener RecyclerView 条目内部短点监听
     */
    public void setOnRecyclerViewOnItemInnerClickListener(OnRecyclerViewOnItemInnerClickListener onRecyclerViewOnItemInnerClickListener) {
        this.onRecyclerViewOnItemInnerClickListener = onRecyclerViewOnItemInnerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_image_detail, parent, false);
        // 宽高等同
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = ((ScreenUtils.screenWidth(context) - totalMargin) / spanCount);
        view.setLayoutParams(layoutParams);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(imageItemList.get(position));
        // 全屏
        holder.mediaImageDetailItemAcibFullScreen.setOnClickListener(v -> onRecyclerViewOnItemInnerClickListener.onItemInnerClick(v, position, imageItemList.get(position)));
    }

    @Override
    public int getItemCount() {
        return imageItemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mediaImageDetailItemIv;
        private final AppCompatImageButton mediaImageDetailItemAcibFullScreen;
        private final TextView mediaImageDetailItemTvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageDetailItemIv = itemView.findViewById(R.id.mediaImageDetailItemIv);
            mediaImageDetailItemAcibFullScreen = itemView.findViewById(R.id.mediaImageDetailItemAcibFullScreen);
            mediaImageDetailItemTvName = itemView.findViewById(R.id.mediaImageDetailItemTvName);
        }

        public void bind(MediaScanner.@NotNull ImageItem imageItem) {
            if (imageItem.file != null) {
                // 图片
                GlideLoader.with(itemView.getContext()).loadRounded(mediaImageDetailItemIv, imageItem.file.getAbsolutePath(), 8.0f);
                // 名称
                mediaImageDetailItemTvName.setText(imageItem.file.getName());
            }
        }
    }
}