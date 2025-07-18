package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.databinding.VodDialogSiteBinding;
import top.cywin.onetv.vod.impl.SiteCallback;
import top.cywin.onetv.vod.ui.adapter.SiteAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private RecyclerView.ItemDecoration decoration;
    private final VodDialogSiteBinding binding;
    private final SiteCallback callback;
    private final SiteAdapter adapter;
    private final AlertDialog dialog;
    private final int GRID_COUNT = 10;
    private int type;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public SiteDialog(Activity activity) {
        this.adapter = new SiteAdapter(this);
        this.callback = (SiteCallback) activity;
        this.binding = VodDialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public SiteDialog search() {
        type = 1;
        return this;
    }

    public SiteDialog action() {
        binding.action.setVisibility(View.VISIBLE);
        return this;
    }

    public void show() {
        setType(type);
        initView();
        initEvent();
    }

    private boolean list() {
        return Setting.getSiteMode() == 0 || adapter.getItemCount() < GRID_COUNT;
    }

    private int getCount() {
        return list() ? 1 : Math.max(2, Math.min((int) Math.ceil((double) adapter.getItemCount() / GRID_COUNT), 3));
    }

    private int getIcon() {
        return list() ? R.drawable.vod_ic_site_grid : R.drawable.vod_ic_site_list;
    }

    private float getWidth() {
        return 0.4f + (getCount() - 1) * 0.2f;
    }

    private void initView() {
        setRecyclerView();
        setDialog();
        setMode();
    }

    private void initEvent() {
        binding.mode.setOnClickListener(this::setMode);
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        binding.search.setOnClickListener(v -> setType(v.isActivated() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isActivated() ? 0 : 2));
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        if (decoration != null) binding.recycler.removeItemDecoration(decoration);
        binding.recycler.addItemDecoration(decoration = new SpaceItemDecoration(getCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), getCount()));
        if (!binding.mode.hasFocus()) binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * getWidth());
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void setMode() {
        if (adapter.getItemCount() < GRID_COUNT) Setting.putSiteMode(0);
        binding.mode.setEnabled(adapter.getItemCount() >= GRID_COUNT);
        binding.mode.setImageResource(getIcon());
    }

    private void setType(int type) {
        binding.search.setActivated(type == 1);
        binding.change.setActivated(type == 2);
        binding.select.setClickable(type > 0);
        binding.cancel.setClickable(type > 0);
        adapter.setType(this.type = type);
    }

    private void setMode(View view) {
        Setting.putSiteMode(Math.abs(Setting.getSiteMode() - 1));
        initView();
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }
}
