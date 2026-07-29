package com.jiangdg.bluetooth;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jiangdg.common.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class BluetoothDeviceInfoRecyclerAdapter extends RecyclerView.Adapter<BluetoothDeviceInfoRecyclerAdapter.ViewHolder> {
    // set false on production
    private static final boolean DEBUG = true;
    private static final String TAG = BluetoothDeviceInfoRecyclerAdapter.class.getSimpleName();
    /**
     * 項目表示用レイアウトリソースID
     */
    private final int mLayoutId;
    @NonNull
    private final List<BluetoothDeviceInfo> mValues;
    @Nullable
    private final OnItemClickListener mListener;

    public BluetoothDeviceInfoRecyclerAdapter(@LayoutRes final int resource, @Nullable final OnItemClickListener listener) {
        this(resource, null, listener);
    }

    public BluetoothDeviceInfoRecyclerAdapter(@LayoutRes final int resource,
                                              @Nullable final List<BluetoothDeviceInfo> list,
                                              @Nullable final OnItemClickListener listener) {
        mLayoutId = resource;
        mValues = list != null ? list : new ArrayList<>();
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.position = position;
        holder.mItem = mValues.get(position);
        if (holder.addressTv != null) {
            holder.addressTv.setText(mValues.get(position).address);
        }
        if (holder.nameTv != null) {
            holder.nameTv.setText(mValues.get(position).name);
        }
//		if (holder.icon != null) {
//			// FIXME 接続状態によるアイコンの変更は未実装
//			holder.icon.setImageResource(item.isPaired() ? R.mipmap.ic_paired : R.mipmap.ic_not_paired);
//		}

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onClick(holder.position, holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public BluetoothDeviceInfo getItem(final int index) throws IndexOutOfBoundsException {
        return mValues.get(index);
    }

    public void add(@NonNull BluetoothDeviceInfo info) {
        mValues.add(info);
        notifyDataSetChanged();
    }

    public void add(final int index, @NonNull BluetoothDeviceInfo info) {
        mValues.add(index, info);
        notifyDataSetChanged();
    }

    public void addAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
        mValues.addAll(collection);
        notifyDataSetChanged();
    }

    public void remove(final BluetoothDeviceInfo info) {
        mValues.remove(info);
        notifyDataSetChanged();
    }

    public void remove(final int index) {
        mValues.remove(index);
        notifyDataSetChanged();
    }

    public void removeAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
        mValues.removeAll(collection);
        notifyDataSetChanged();
    }

    public void retainAll(@NonNull Collection<? extends BluetoothDeviceInfo> collection) {
        mValues.retainAll(collection);
        notifyDataSetChanged();
    }

    public void clear() {
        mValues.clear();
        notifyDataSetChanged();
    }

    public void sort(final Comparator<? super BluetoothDeviceInfo> comparator) {
        mValues.sort(comparator);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        public void onClick(final int position, final BluetoothDeviceInfo item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView icon;
        public final TextView nameTv;
        public final TextView addressTv;
        public BluetoothDeviceInfo mItem;
        public int position;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            nameTv = view.findViewById(R.id.name);
            addressTv = view.findViewById(R.id.address);
            icon = view.findViewById(R.id.icon);
        }
    }
}