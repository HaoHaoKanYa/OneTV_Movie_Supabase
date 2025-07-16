package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.databinding.VodAdapterHeaderBinding;
import top.cywin.onetv.vod.utils.ResUtil;

public class HeaderPresenter extends Presenter {

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new HeaderPresenter.ViewHolder(VodAdapterHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        HeaderPresenter.ViewHolder holder = (HeaderPresenter.ViewHolder) viewHolder;
        holder.binding.text.setText(object instanceof String ? object.toString() : ResUtil.getString((int) object));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterHeaderBinding binding;

        public ViewHolder(@NonNull VodAdapterHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}