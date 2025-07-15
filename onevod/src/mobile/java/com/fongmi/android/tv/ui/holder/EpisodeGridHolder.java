package com.fongmi.onetv.tv.ui.holder;

import androidx.annotation.NonNull;

import com.fongmi.onetv.tv.bean.Episode;
import com.fongmi.onetv.tv.databinding.AdapterEpisodeGridBinding;
import com.fongmi.onetv.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.onetv.tv.ui.base.BaseEpisodeHolder;

public class EpisodeGridHolder extends BaseEpisodeHolder {

    private final EpisodeAdapter.OnClickListener listener;
    private final AdapterEpisodeGridBinding binding;

    public EpisodeGridHolder(@NonNull AdapterEpisodeGridBinding binding, EpisodeAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    @Override
    public void initView(Episode item) {
        binding.text.setSelected(item.isSelected());
        binding.text.setActivated(item.isActivated());
        binding.text.setText(item.getDesc().concat(item.getName()));
        binding.text.setOnClickListener(v -> listener.onItemClick(item));
    }
}
