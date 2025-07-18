package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.bean.Class;
import top.cywin.onetv.vod.bean.Result;
import top.cywin.onetv.vod.databinding.VodAdapterTypeBinding;
import top.cywin.onetv.vod.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Class> mItems;

    public TypeAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(int position, Class item);
    }

    private Class home() {
        Class type = new Class();
        type.setTypeName(ResUtil.getString(R.string.vod_home));
        type.setTypeId("home");
        return type;
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(Result result) {
        mItems.addAll(result.getTypes());
        if (!result.getList().isEmpty()) mItems.add(0, home());
        if (!mItems.isEmpty()) mItems.get(0).setActivated(true);
        notifyDataSetChanged();
    }

    public void setActivated(int position) {
        for (Class item : mItems) item.setActivated(false);
        mItems.get(position).setActivated(true);
        notifyItemRangeChanged(0, mItems.size());
    }

    public Class get(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = mItems.get(position);
        holder.binding.text.setText(item.getTypeName());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(position, item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterTypeBinding binding;

        ViewHolder(@NonNull VodAdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}