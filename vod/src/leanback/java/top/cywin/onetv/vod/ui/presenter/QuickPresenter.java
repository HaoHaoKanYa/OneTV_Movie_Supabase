package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.VodAdapterQuickBinding;
import top.cywin.onetv.vod.utils.ResUtil;

public class QuickPresenter extends Presenter {

    private final OnClickListener mListener;
    private int width;

    public QuickPresenter(OnClickListener listener) {
        this.mListener = listener;
        setLayoutSize();
    }

    private void setLayoutSize() {
        int space = ResUtil.dp2px(24) + ResUtil.dp2px(32);
        int base = ResUtil.getScreenWidth() - space;
        width = base / 4;
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder holder = new ViewHolder(VodAdapterQuickBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        return holder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod item = (Vod) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterQuickBinding binding;

        public ViewHolder(@NonNull VodAdapterQuickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}