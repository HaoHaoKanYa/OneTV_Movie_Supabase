package top.cywin.onetv.vod.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import top.cywin.onetv.vod.bean.Config;
import top.cywin.onetv.vod.databinding.VodDialogHistoryBinding;
import top.cywin.onetv.vod.impl.ConfigCallback;
import top.cywin.onetv.vod.ui.adapter.ConfigAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HistoryDialog implements ConfigAdapter.OnClickListener {

    private final VodDialogHistoryBinding binding;
    private final ConfigCallback callback;
    private final ConfigAdapter adapter;
    private final AlertDialog dialog;
    private int type;

    public static HistoryDialog create(Fragment fragment) {
        return new HistoryDialog(fragment);
    }

    public HistoryDialog type(int type) {
        this.type = type;
        return this;
    }

    public HistoryDialog(Fragment fragment) {
        this.callback = (ConfigCallback) fragment;
        this.binding = VodDialogHistoryBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
        this.adapter = new ConfigAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(type));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(0));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
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
