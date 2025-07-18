package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.databinding.VodAdapterArrayBinding;
import top.cywin.onetv.vod.utils.ResUtil;

public class ArrayPresenter extends Presenter {

    private final OnClickListener mListener;
    private final String backward;
    private final String forward;
    private final String reverse;

    public ArrayPresenter(OnClickListener listener) {
        this.mListener = listener;
        this.backward = ResUtil.getString(R.string.vod_play_backward);
        this.forward = ResUtil.getString(R.string.vod_play_forward);
        this.reverse = ResUtil.getString(R.string.vod_play_reverse);
    }

    public interface OnClickListener {

        void onRevSort();

        void onRevPlay(TextView view);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterArrayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ViewHolder holder = (ViewHolder) viewHolder;
        String text = object.toString();
        holder.binding.text.setText(text);
        if (text.equals(reverse)) setOnClickListener(holder, view -> mListener.onRevSort());
        else if (text.equals(backward) || text.equals(forward)) setOnClickListener(holder, view -> mListener.onRevPlay(holder.binding.text));
        else setOnClickListener(holder, null);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterArrayBinding binding;

        public ViewHolder(@NonNull VodAdapterArrayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}