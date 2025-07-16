package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.Product;
import top.cywin.onetv.vod.bean.Episode;
import top.cywin.onetv.vod.databinding.VodAdapterEpisodeBinding;

public class EpisodePresenter extends Presenter {

    private final OnClickListener mListener;
    private int nextFocusDown;
    private int nextFocusUp;

    public EpisodePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Episode item);
    }

    public void setNextFocusDown(int nextFocus) {
        this.nextFocusDown = nextFocus;
    }

    public void setNextFocusUp(int nextFocus) {
        this.nextFocusUp = nextFocus;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Episode item = (Episode) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setMaxEms(Product.getEms());
        holder.binding.text.setNextFocusUpId(nextFocusUp);
        holder.binding.text.setNextFocusDownId(nextFocusDown);
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setText(item.getDesc().concat(item.getName()));
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterEpisodeBinding binding;

        public ViewHolder(@NonNull VodAdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}