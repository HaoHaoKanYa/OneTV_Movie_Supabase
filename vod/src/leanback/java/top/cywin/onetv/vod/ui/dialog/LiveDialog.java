package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import top.cywin.onetv.vod.api.config.LiveConfig;
import top.cywin.onetv.vod.bean.Live;
import top.cywin.onetv.vod.databinding.VodDialogLiveBinding;
import top.cywin.onetv.vod.impl.LiveCallback;
import top.cywin.onetv.vod.ui.adapter.LiveAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LiveDialog implements LiveAdapter.OnClickListener {

    private final VodDialogLiveBinding binding;
    private final LiveCallback callback;
    private final LiveAdapter adapter;
    private final AlertDialog dialog;

    public static LiveDialog create(Activity activity) {
        return new LiveDialog(activity);
    }

    private LiveDialog(Activity activity) {
        this.adapter = new LiveAdapter(this);
        this.callback = (LiveCallback) activity;
        this.binding = VodDialogLiveBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
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
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(LiveConfig.getHomeIndex()));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.4f);
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
