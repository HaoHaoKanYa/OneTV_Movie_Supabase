package com.fongmi.onetv.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.onetv.tv.api.config.LiveConfig;
import com.fongmi.onetv.tv.bean.Live;
import com.fongmi.onetv.tv.databinding.DialogLiveBinding;
import com.fongmi.onetv.tv.impl.LiveCallback;
import com.fongmi.onetv.tv.ui.adapter.LiveAdapter;
import com.fongmi.onetv.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.onetv.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LiveDialog implements LiveAdapter.OnClickListener {

    private final LiveCallback callback;
    private DialogLiveBinding binding;
    private LiveAdapter adapter;
    private AlertDialog dialog;
    private boolean full;

    public static LiveDialog create(Activity activity) {
        return new LiveDialog(activity);
    }

    public static LiveDialog create(Fragment fragment) {
        return new LiveDialog(fragment);
    }

    private LiveDialog(Activity activity) {
        this.callback = (LiveCallback) activity;
        this.full = true;
        init(activity);
    }

    private LiveDialog(Fragment fragment) {
        this.callback = (LiveCallback) fragment;
        init(fragment.getActivity());
    }

    private void init(Activity activity) {
        this.binding = DialogLiveBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new LiveAdapter(this);
    }

    public LiveDialog action() {
        adapter.setAction(true);
        return this;
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(LiveConfig.getHomeIndex()));
        if (full) binding.recycler.setMaxHeight(264);
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        if (full && ResUtil.isLand(dialog.getContext())) params.width = (int) (ResUtil.getScreenWidth() * 0.5f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(Live item) {
        callback.setLive(item);
        dialog.dismiss();
    }

    @Override
    public void onBootClick(int position, Live item) {
        item.boot(!item.isBoot()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onPassClick(int position, Live item) {
        item.pass(!item.isPass()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onBootLongClick(Live item) {
        boolean result = !item.isBoot();
        for (Live live : LiveConfig.get().getLives()) live.boot(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }

    @Override
    public boolean onPassLongClick(Live item) {
        boolean result = !item.isPass();
        for (Live live : LiveConfig.get().getLives()) live.pass(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }
}
