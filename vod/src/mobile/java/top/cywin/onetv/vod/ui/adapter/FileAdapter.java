package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.databinding.VodAdapterFileBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<File> mItems;

    public FileAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(File file);
    }

    public void addAll(List<File> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = mItems.get(position);
        holder.binding.name.setText(file.getName());
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(file));
        holder.binding.image.setImageResource(file.isDirectory() ? R.drawable.vod_ic_folder : R.drawable.vod_ic_file);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterFileBinding binding;

        ViewHolder(@NonNull VodAdapterFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
