package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Parse;
import top.cywin.onetv.vod.databinding.VodAdapterParseDarkBinding;
import top.cywin.onetv.vod.databinding.VodAdapterParseLightBinding;
import top.cywin.onetv.vod.ui.base.ViewType;

import java.util.List;

public class ParseAdapter extends RecyclerView.Adapter<ParseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Parse> mItems;
    private final int viewType;

    public ParseAdapter(OnClickListener listener, int viewType) {
        this.mItems = VodConfig.get().getParses();
        this.mListener = listener;
        this.viewType = viewType;
    }

    public interface OnClickListener {

        void onItemClick(Parse item);
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Parse get(int position) {
        return mItems.get(position);
    }

    public Parse first() {
        return mItems.get(0);
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.DARK) return new ViewHolder(VodAdapterParseDarkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        return new ViewHolder(VodAdapterParseLightBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Parse item = mItems.get(position);
        if (holder.darkBinding != null) holder.initView(holder.darkBinding.text, item);
        if (holder.lightBinding != null) holder.initView(holder.lightBinding.text, item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private VodAdapterParseDarkBinding darkBinding;
        private VodAdapterParseLightBinding lightBinding;

        ViewHolder(@NonNull VodAdapterParseDarkBinding binding) {
            super(binding.getRoot());
            this.darkBinding = binding;
        }

        ViewHolder(@NonNull VodAdapterParseLightBinding binding) {
            super(binding.getRoot());
            this.lightBinding = binding;
        }

        void initView(TextView view, Parse item) {
            view.setText(item.getName());
            view.setActivated(item.isActivated());
            view.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }
}