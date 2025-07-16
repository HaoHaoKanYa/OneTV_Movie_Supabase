package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Collect;
import top.cywin.onetv.vod.databinding.VodAdapterFilterBinding;

public class CollectPresenter extends Presenter {

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Collect item = (Collect) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getSite().getName());
        setOnClickListener(holder, null);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterFilterBinding binding;

        public ViewHolder(@NonNull VodAdapterFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}