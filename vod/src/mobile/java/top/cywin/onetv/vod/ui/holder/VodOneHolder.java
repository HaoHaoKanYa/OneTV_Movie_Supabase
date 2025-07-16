package top.cywin.onetv.vod.ui.holder;

import androidx.annotation.NonNull;

import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.VodAdapterVodOneBinding;
import top.cywin.onetv.vod.ui.adapter.VodAdapter;
import top.cywin.onetv.vod.ui.base.BaseVodHolder;
import top.cywin.onetv.vod.utils.ImgUtil;

public class VodOneHolder extends BaseVodHolder {

    private final VodAdapter.OnClickListener listener;
    private final VodAdapterVodOneBinding binding;

    public VodOneHolder(@NonNull VodAdapterVodOneBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    @Override
    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.site.setText(item.getSiteName());
        binding.remark.setText(item.getVodRemarks());
        binding.site.setVisibility(item.getSiteVisible());
        binding.remark.setVisibility(item.getRemarkVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.rect(item.getVodName(), item.getVodPic(), binding.image);
    }
}
