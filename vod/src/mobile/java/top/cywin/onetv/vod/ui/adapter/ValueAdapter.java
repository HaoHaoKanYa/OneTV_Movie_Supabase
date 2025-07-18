package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Filter;
import top.cywin.onetv.vod.bean.Value;
import top.cywin.onetv.vod.databinding.VodAdapterValueBinding;
import top.cywin.onetv.vod.impl.FilterCallback;

import java.util.List;

public class ValueAdapter extends RecyclerView.Adapter<ValueAdapter.ViewHolder> {

    private final FilterCallback mListener;
    private final List<Value> mItems;
    private final String mKey;

    public ValueAdapter(FilterCallback listener, Filter filter) {
        this.mListener = listener;
        this.mItems = filter.getValue();
        this.mKey = filter.getKey();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterValueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Value item = mItems.get(position);
        holder.binding.text.setText(item.getN());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> onItemClick(item));
    }

    private void onItemClick(Value value) {
        for (Value item : mItems) item.setActivated(value);
        notifyItemRangeChanged(0, getItemCount());
        mListener.setFilter(mKey, value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterValueBinding binding;

        ViewHolder(@NonNull VodAdapterValueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}