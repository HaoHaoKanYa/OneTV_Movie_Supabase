package top.cywin.onetv.tv.ui.holder;

import androidx.annotation.NonNull;

import top.cywin.onetv.tv.bean.Vod;
import com.fongmi.onetv.tv.databinding.AdapterVodOvalBinding;
import top.cywin.onetv.tv.ui.base.BaseVodHolder;
import top.cywin.onetv.tv.ui.presenter.VodPresenter;
import top.cywin.onetv.tv.utils.ImgUtil;

public class VodOvalHolder extends BaseVodHolder {

    private final VodPresenter.OnClickListener listener;
    private final AdapterVodOvalBinding binding;

    public VodOvalHolder(@NonNull AdapterVodOvalBinding binding, VodPresenter.OnClickListener listener) {
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
