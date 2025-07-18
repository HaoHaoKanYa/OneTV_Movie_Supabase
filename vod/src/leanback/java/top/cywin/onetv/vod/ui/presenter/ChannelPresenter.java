package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Channel;
import top.cywin.onetv.vod.databinding.VodAdapterChannelBinding;

public class ChannelPresenter extends Presenter {

    private final OnClickListener mListener;

    public ChannelPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void showEpg(Channel item);

        void onItemClick(Channel item);

        boolean onLongClick(Channel item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterChannelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Channel item = (Channel) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        item.loadLogo(holder.binding.logo);
        holder.binding.name.setText(item.getName());
        holder.binding.number.setText(item.getNumber());
        holder.binding.getRoot().setSelected(item.isSelected());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
        holder.view.setOnLongClickListener(view -> mListener.onLongClick(item));
        holder.binding.getRoot().setRightListener(() -> mListener.showEpg(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterChannelBinding binding;

        public ViewHolder(@NonNull VodAdapterChannelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}