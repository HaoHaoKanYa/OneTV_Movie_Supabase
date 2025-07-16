package top.cywin.onetv.vod.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.Product;
import top.cywin.onetv.vod.bean.Style;
import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.AdapterVodListBinding;
import top.cywin.onetv.vod.databinding.AdapterVodOvalBinding;
import top.cywin.onetv.vod.databinding.AdapterVodRectBinding;
import top.cywin.onetv.vod.ui.base.BaseVodHolder;
import top.cywin.onetv.vod.ui.base.ViewType;
import top.cywin.onetv.vod.ui.holder.VodListHolder;
import top.cywin.onetv.vod.ui.holder.VodOvalHolder;
import top.cywin.onetv.vod.ui.holder.VodRectHolder;

public class VodPresenter extends Presenter {

    private final OnClickListener mListener;
    private final Style style;
    private final int[] size;

    public VodPresenter(OnClickListener listener) {
        this(listener, Style.rect());
    }

    public VodPresenter(OnClickListener listener, Style style) {
        this.mListener = listener;
        this.style = style;
        this.size = Product.getSpec(style);
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        switch (style.getViewType()) {
            case ViewType.LIST:
                return new VodListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.OVAL:
                return new VodOvalHolder(AdapterVodOvalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
            default:
                return new VodRectHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ((BaseVodHolder) viewHolder).initView((Vod) object);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }
}