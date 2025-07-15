package top.cywin.onetv.vod.ui.holder;

import androidx.annotation.NonNull;

import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.AdapterVodRectBinding;
import top.cywin.onetv.vod.ui.adapter.VodAdapter;
import top.cywin.onetv.vod.ui.base.BaseVodHolder;
import top.cywin.onetv.vod.utils.ImgUtil;

public class VodRectHolder extends BaseVodHolder {

    private final VodAdapter.OnClickListener listener;
    private final AdapterVodRectBinding binding;

    public VodRectHolder(@NonNull AdapterVodRectBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public VodRectHolder size(int[] size) {
        binding.getRoot().getLayoutParams().width = size[0];
        binding.getRoot().getLayoutParams().height = size[1];
        return this;
    }

    @Override
    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.year.setText(item.getVodYear());
        binding.site.setText(item.getSiteName());
        binding.remark.setText(item.getVodRemarks());
        binding.site.setVisibility(item.getSiteVisible());
        binding.name.setVisibility(item.getNameVisible());
        binding.year.setVisibility(item.getYearVisible());
        binding.remark.setVisibility(item.getRemarkVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.rect(item.getVodName(), item.getVodPic(), binding.image);
    }
}
