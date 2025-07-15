package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Style;
import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.AdapterVodListBinding;
import top.cywin.onetv.vod.databinding.AdapterVodOvalBinding;
import top.cywin.onetv.vod.databinding.AdapterVodRectBinding;
import top.cywin.onetv.vod.ui.base.BaseVodHolder;
import top.cywin.onetv.vod.ui.base.ViewType;
import top.cywin.onetv.vod.ui.holder.VodListHolder;
import top.cywin.onetv.vod.ui.holder.VodOvalHolder;
import com.fongmi.onetv.tv.ui.holder.VodRectHolder;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<BaseVodHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private final Style style;
    private final int[] size;

    public VodAdapter(OnClickListener listener, Style style, int[] size) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.style = style;
        this.size = size;
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    public Style getStyle() {
        return style;
    }

    public void addAll(List<Vod> items) {
        int position = mItems.size() + 1;
        mItems.addAll(items);
        notifyItemRangeInserted(position, items.size());
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return style.getViewType();
    }

    @Override
    public void onBindViewHolder(@NonNull BaseVodHolder holder, int position) {
        holder.initView(mItems.get(position));
    }

    @NonNull
    @Override
    public BaseVodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ViewType.LIST:
                return new VodListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.OVAL:
                return new VodOvalHolder(AdapterVodOvalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
            default:
                return new VodRectHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
        }
    }
}
