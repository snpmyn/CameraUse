package com.qtone.camerause.model.gallery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;
import com.qtone.camerause.function.media.MediaScanner;
import com.qtone.camerause.util.density.DensityUtils;
import com.qtone.camerause.util.image.GlideLoader;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.util.screen.ScreenUtils;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemClickListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/8/20.
 *
 * @author 郑少鹏
 * @desc 媒体图片组适配器
 */
public class MediaImageGroupAdapter extends RecyclerView.Adapter<MediaImageGroupAdapter.ViewHolder> {
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
     * 文件夹条目集
     */
    private List<MediaScanner.FolderItem> folderItemList;
    /**
     * RecyclerView 条目短点监听
     */
    private OnRecyclerViewOnItemClickListener onRecyclerViewOnItemClickListener;

    /**
     * constructor
     *
     * @param context     上下文
     * @param spanCount   跨距数
     * @param totalMargin 总外边距
     */
    public MediaImageGroupAdapter(Context context, int spanCount, int totalMargin) {
        this.context = context;
        this.spanCount = spanCount;
        this.totalMargin = DensityUtils.dipToPxByInt(totalMargin);
    }

    /**
     * 提交数据
     *
     * @param folderItemList 文件夹条目集
     */
    public void submitData(List<MediaScanner.FolderItem> folderItemList) {
        if (this.folderItemList == null) {
            this.folderItemList = new ArrayList<>();
        } else {
            this.folderItemList.clear();
        }
        if (ListUtils.listIsNotEmpty(folderItemList)) {
            this.folderItemList.addAll(folderItemList);
        }
        notifyDataSetChanged();
    }

    /**
     * 设置 RecyclerView 条目短点监听
     *
     * @param onRecyclerViewOnItemLongClickListener RecyclerView 条目短点监听
     */
    public void setOnRecyclerViewOnItemClickListener(OnRecyclerViewOnItemClickListener onRecyclerViewOnItemLongClickListener) {
        this.onRecyclerViewOnItemClickListener = onRecyclerViewOnItemLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_image_group, parent, false);
        // 宽高等同
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = ((ScreenUtils.screenWidth(context) - totalMargin) / spanCount);
        view.setLayoutParams(layoutParams);
        // 点击监听
        view.setOnClickListener(v -> {
            int position = (Integer) view.getTag();
            onRecyclerViewOnItemClickListener.onItemClick(v, position, folderItemList.get(position));
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        holder.bind(folderItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return ListUtils.listIsNotEmpty(folderItemList) ? folderItemList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mediaImageGroupItemIv;
        private final TextView mediaImageGroupItemTvFolder;
        private final TextView mediaImageGroupItemTvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageGroupItemIv = itemView.findViewById(R.id.mediaImageGroupItemIv);
            mediaImageGroupItemTvFolder = itemView.findViewById(R.id.mediaImageGroupItemTvFolder);
            mediaImageGroupItemTvCount = itemView.findViewById(R.id.mediaImageGroupItemTvCount);
        }

        public void bind(MediaScanner.@NotNull FolderItem folderItem) {
            // 图片
            if (ListUtils.listIsNotEmpty(folderItem.imageItemList)) {
                MediaScanner.ImageItem firstImageItem = folderItem.imageItemList.get(0);
                if ((firstImageItem != null) && (firstImageItem.file != null)) {
                    GlideLoader.with(itemView.getContext()).loadRounded(mediaImageGroupItemIv, firstImageItem.file.getAbsolutePath(), 8.0f);
                }
            }
            // 目录
            mediaImageGroupItemTvFolder.setText(folderItem.folderName);
            // 数量
            int count = ListUtils.listIsNotEmpty(folderItem.imageItemList) ? folderItem.imageItemList.size() : 0;
            String countText = (count + " 项");
            mediaImageGroupItemTvCount.setText(countText);
        }
    }
}