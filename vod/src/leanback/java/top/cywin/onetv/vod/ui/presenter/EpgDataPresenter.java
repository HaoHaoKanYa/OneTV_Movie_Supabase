package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.EpgData;
import top.cywin.onetv.vod.databinding.VodAdapterEpgDataBinding;

public class EpgDataPresenter extends Presenter {

    private final OnClickListener mListener;

    public EpgDataPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void hideEpg();

        void onItemClick(EpgData item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterEpgDataBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        EpgData item = (EpgData) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.time.setText(item.getTime());
        holder.binding.title.setText(item.getTitle());
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setLeftListener(mListener::hideEpg);
        setOnClickListener(holder, view -> {
            if (!item.isFuture()) mListener.onItemClick(item);
        });
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterEpgDataBinding binding;

        public ViewHolder(@NonNull VodAdapterEpgDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}