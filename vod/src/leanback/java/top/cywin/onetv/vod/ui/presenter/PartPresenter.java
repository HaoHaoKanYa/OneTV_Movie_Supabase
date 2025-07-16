package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.Product;
import top.cywin.onetv.vod.databinding.VodAdapterPartBinding;

public class PartPresenter extends Presenter {

    private final OnClickListener mListener;
    private int nextFocusUp;

    public PartPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(String item);
    }

    public void setNextFocusUp(int nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterPartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        String text = object.toString();
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(text);
        holder.binding.text.setMaxEms(Product.getEms());
        holder.binding.text.setNextFocusUpId(nextFocusUp);
        setOnClickListener(holder, view -> mListener.onItemClick(text));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterPartBinding binding;

        public ViewHolder(@NonNull VodAdapterPartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}