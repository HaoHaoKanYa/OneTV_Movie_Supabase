package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import top.cywin.onetv.vod.bean.Config;
import top.cywin.onetv.vod.databinding.VodDialogHistoryBinding;
import top.cywin.onetv.vod.impl.ConfigCallback;
import top.cywin.onetv.vod.ui.adapter.ConfigAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HistoryDialog implements ConfigAdapter.OnClickListener {

    private final VodDialogHistoryBinding binding;
    private final ConfigCallback callback;
    private final ConfigAdapter adapter;
    private final AlertDialog dialog;
    private int type;

    public static HistoryDialog create(Activity activity) {
        return new HistoryDialog(activity);
    }

    public HistoryDialog type(int type) {
        this.type = type;
        return this;
    }

    public HistoryDialog(Activity activity) {
        this.callback = (ConfigCallback) activity;
        this.binding = VodDialogHistoryBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new ConfigAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(false);
        binding.recycler.setAdapter(adapter.addAll(type));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
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
    public void onTextClick(Config item) {
        callback.setConfig(item);
        dialog.dismiss();
    }

    @Override
    public void onDeleteClick(Config item) {
        if (adapter.remove(item) == 0) dialog.dismiss();
    }
}
