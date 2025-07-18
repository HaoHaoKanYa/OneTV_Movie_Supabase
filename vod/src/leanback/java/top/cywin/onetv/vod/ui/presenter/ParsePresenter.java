package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Parse;
import top.cywin.onetv.vod.databinding.VodAdapterParseBinding;

public class ParsePresenter extends Presenter {

    private final OnClickListener mListener;

    public ParsePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Parse parse);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterParseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Parse item = (Parse) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setActivated(item.isActivated());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterParseBinding binding;

        public ViewHolder(@NonNull VodAdapterParseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}