package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import top.cywin.onetv.vod.databinding.VodDialogDohBinding;
import top.cywin.onetv.vod.impl.DohCallback;
import top.cywin.onetv.vod.ui.adapter.DohAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.utils.ResUtil;
import top.github.catvod.bean.Doh;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DohDialog implements DohAdapter.OnClickListener {

    private final VodDialogDohBinding binding;
    private final DohCallback callback;
    private final AlertDialog dialog;
    private final DohAdapter adapter;

    public static DohDialog create(Activity activity) {
        return new DohDialog(activity);
    }

    public DohDialog index(int index) {
        adapter.setSelect(index);
        return this;
    }

    public DohDialog(Activity activity) {
        this.callback = (DohCallback) activity;
        this.binding = VodDialogDohBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new DohAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(adapter.getSelect()));
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
    public void onItemClick(Doh item) {
        callback.setDoh(item);
        dialog.dismiss();
    }
}
