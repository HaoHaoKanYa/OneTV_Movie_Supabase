package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Group;
import top.cywin.onetv.vod.databinding.VodAdapterGroupBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Group> mItems;

    public GroupAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void setWidth(Group item);

        void onItemClick(Group item);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void add(Group item) {
        mItems.add(item);
        notifyItemInserted(getItemCount() - 1);
    }

    public Group get(int position) {
        return mItems.get(position);
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isSelected()) return i;
        return 0;
    }

    public int indexOf(Group group) {
        return mItems.indexOf(group);
    }

    public void setSelected(Group group) {
        setSelected(indexOf(group));
    }

    public void setSelected(int position) {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(i == position);
        notifyItemRangeChanged(0, getItemCount());
        mListener.setWidth(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterGroupBinding binding;

        ViewHolder(@NonNull VodAdapterGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
