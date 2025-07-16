package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Group;
import top.cywin.onetv.vod.databinding.VodAdapterGroupBinding;

public class GroupPresenter extends Presenter {

    private final OnClickListener mListener;

    public GroupPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Group item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Group item = (Group) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getName());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterGroupBinding binding;

        public ViewHolder(@NonNull VodAdapterGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}