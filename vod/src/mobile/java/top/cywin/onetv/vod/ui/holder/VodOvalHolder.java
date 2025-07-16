package top.cywin.onetv.vod.ui.holder;

import androidx.annotation.NonNull;

import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.VodAdapterVodOvalBinding;
import top.cywin.onetv.vod.ui.adapter.VodAdapter;
import top.cywin.onetv.vod.ui.base.BaseVodHolder;
import top.cywin.onetv.vod.utils.ImgUtil;

public class VodOvalHolder extends BaseVodHolder {

    private final VodAdapter.OnClickListener listener;
    private final VodAdapterVodOvalBinding binding;

    public VodOvalHolder(@NonNull VodAdapterVodOvalBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public VodOvalHolder size(int[] size) {
        binding.image.getLayoutParams().width = size[0];
        binding.image.getLayoutParams().height = size[1];
        return this;
    }

    @Override
    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.name.setVisibility(item.getNameVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.oval(item.getVodName(), item.getVodPic(), binding.image);
    }
}
