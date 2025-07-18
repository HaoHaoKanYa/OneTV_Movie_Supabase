package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.bean.Class;
import top.cywin.onetv.vod.databinding.VodAdapterTypeBinding;
import top.cywin.onetv.vod.utils.ResUtil;

public class TypePresenter extends Presenter {

    private final OnClickListener mListener;

    public TypePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Class item);

        void onRefresh(Class item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(VodAdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Class item = (Class) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getTypeName());
        holder.binding.text.setCompoundDrawablePadding(ResUtil.dp2px(4));
        holder.binding.text.setCompoundDrawablesWithIntrinsicBounds(0, 0, getIcon(item), 0);
        holder.binding.text.setListener(() -> mListener.onRefresh(item));
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    private int getIcon(Class item) {
        return item.getFilter() == null ? 0 : item.getFilter() ? R.drawable.vod_ic_vod_filter_off : R.drawable.vod_ic_vod_filter_on;
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final VodAdapterTypeBinding binding;

        public ViewHolder(@NonNull VodAdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}