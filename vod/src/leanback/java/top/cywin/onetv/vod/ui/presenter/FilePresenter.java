package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.databinding.VodAdapterFileBinding;

import java.io.File;

public class FilePresenter extends Presenter {

    private final OnClickListener mListener;

    public FilePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(File file);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        File file = (File) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(file.getName());
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(file));
        holder.binding.image.setImageResource(file.isDirectory() ? R.drawable.vod_ic_folder : R.drawable.vod_ic_file);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterFileBinding binding;

        public ViewHolder(@NonNull VodAdapterFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}