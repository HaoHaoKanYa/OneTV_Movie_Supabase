package top.cywin.onetv.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.tv.Setting;
import top.cywin.onetv.tv.bean.Vod;
import top.cywin.onetv.tv.databinding.AdapterVodOneBinding;
import top.cywin.onetv.tv.databinding.AdapterVodRectBinding;
import top.cywin.onetv.tv.ui.base.BaseVodHolder;
import top.cywin.onetv.tv.ui.base.ViewType;
import top.cywin.onetv.tv.ui.holder.VodOneHolder;
import top.cywin.onetv.tv.ui.holder.VodRectHolder;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<BaseVodHolder> {

    private final VodAdapter.OnClickListener mListener;
    private final List<Vod> mItems;
    private int viewType;
    private int[] size;

    public SearchAdapter(VodAdapter.OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public void setViewType(int viewType, int count) {
        if (this.viewType > 0 && this.viewType != viewType && count == 1) notifyDataSetChanged();
        Setting.putViewType(viewType);
        this.viewType = viewType;
    }

    public void setSize(int[] size) {
        this.size = size;
    }

    public int getWidth() {
        return size[0];
    }

    public boolean isList() {
        return viewType == ViewType.LIST;
    }

    public boolean isGrid() {
        return viewType == ViewType.GRID;
    }

    public void setAll(List<Vod> items) {
        clear().addAll(items);
    }

    public void addAll(List<Vod> items) {
        int position = mItems.size() + 1;
        mItems.addAll(items);
        notifyItemRangeInserted(position, items.size());
    }

    public SearchAdapter clear() {
        mItems.clear();
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseVodHolder holder, int position) {
        holder.initView(mItems.get(position));
    }

    @NonNull
    @Override
    public BaseVodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) return new VodOneHolder(AdapterVodOneBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        else return new VodRectHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
    }
}
