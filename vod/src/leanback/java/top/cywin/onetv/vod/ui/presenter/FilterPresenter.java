package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Value;
import top.cywin.onetv.vod.databinding.VodAdapterFilterBinding;

public class FilterPresenter extends Presenter {

    private OnClickListener mListener;
    private final String mKey;

    public FilterPresenter(String key) {
        mKey = key;
    }

    public interface OnClickListener {
        void onItemClick(String key, Value item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Value item = (Value) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getN());
        holder.binding.text.setActivated(item.isActivated());
        setOnClickListener(holder, view -> mListener.onItemClick(mKey, item));
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