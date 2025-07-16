package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.bean.Flag;
import top.cywin.onetv.vod.databinding.VodAdapterFlagBinding;

public class FlagPresenter extends Presenter {

    private final OnClickListener mListener;
    private int nextFocusDown;

    public FlagPresenter(OnClickListener listener) {
        this.mListener = listener;
        this.nextFocusDown = R.id.episode;
    }

    public interface OnClickListener {
        void onItemClick(Flag item);
    }

    public void setNextFocusDown(int nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterFlagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Flag item = (Flag) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getShow());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setNextFocusDownId(nextFocusDown);
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterFlagBinding binding;

        public ViewHolder(@NonNull VodAdapterFlagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}