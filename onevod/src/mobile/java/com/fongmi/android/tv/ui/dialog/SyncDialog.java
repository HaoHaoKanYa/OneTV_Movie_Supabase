package com.fongmi.onetv.tv.ui.dialog;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.onetv.tv.App;
import com.fongmi.onetv.tv.Constant;
import com.fongmi.onetv.tv.R;
import com.fongmi.onetv.tv.Setting;
import com.fongmi.onetv.tv.api.config.VodConfig;
import com.fongmi.onetv.tv.bean.Config;
import com.fongmi.onetv.tv.bean.Device;
import com.fongmi.onetv.tv.bean.History;
import com.fongmi.onetv.tv.bean.Keep;
import com.fongmi.onetv.tv.databinding.DialogDeviceBinding;
import com.fongmi.onetv.tv.event.ScanEvent;
import com.fongmi.onetv.tv.impl.Callback;
import com.fongmi.onetv.tv.ui.activity.ScanActivity;
import com.fongmi.onetv.tv.ui.adapter.DeviceAdapter;
import com.fongmi.onetv.tv.utils.Notify;
import com.fongmi.onetv.tv.utils.ResUtil;
import com.fongmi.onetv.tv.utils.ScanTask;
import com.github.catvod.net.OkHttp;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SyncDialog extends BaseDialog implements DeviceAdapter.OnClickListener, ScanTask.Listener {

    private final FormBody.Builder body;
    private final OkHttpClient client;
    private final ScanTask scanTask;
    private final TypedArray mode;
    private DialogDeviceBinding binding;
    private DeviceAdapter adapter;
    private String type;

    public static SyncDialog create() {
        return new SyncDialog();
    }

    public SyncDialog() {
        body = new FormBody.Builder();
        scanTask = new ScanTask(this);
        client = OkHttp.client(Constant.TIMEOUT_SYNC);
        mode = ResUtil.getTypedArray(R.array.cast_mode);
    }

    public SyncDialog history() {
        body.add("device", Device.get().toString());
        body.add("config", Config.vod().toString());
        body.add("targets", App.gson().toJson(History.get()));
        return type("history");
    }

    public SyncDialog keep() {
        body.add("device", Device.get().toString());
        body.add("targets", App.gson().toJson(Keep.getVod()));
        body.add("configs", App.gson().toJson(Config.findUrls()));
        return type("keep");
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    private SyncDialog type(String type) {
        this.type = type;
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDeviceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.mode.setVisibility(View.VISIBLE);
        EventBus.getDefault().register(this);
        setRecyclerView();
        getDevice();
        setMode();
    }

    @Override
    protected void initEvent() {
        binding.mode.setOnClickListener(v -> onMode());
        binding.scan.setOnClickListener(v -> onScan());
        binding.refresh.setOnClickListener(v -> onRefresh());
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter = new DeviceAdapter(this));
    }

    private void getDevice() {
        adapter.addAll(Device.getAll());
        if (adapter.getItemCount() == 0) App.post(this::onRefresh, 1000);
    }

    private void setMode() {
        int index = Setting.getSyncMode();
        binding.mode.setImageResource(mode.getResourceId(index, 0));
        binding.mode.setTag(String.valueOf(index));
    }

    private void onMode() {
        int index = Setting.getSyncMode();
        Setting.putSyncMode(index = index == mode.length() - 1 ? 0 : ++index);
        binding.mode.setImageResource(mode.getResourceId(index, 0));
        binding.mode.setTag(String.valueOf(index));
    }

    private void onScan() {
        ScanActivity.start(getActivity());
    }

    private void onRefresh() {
        scanTask.start(adapter.getIps());
        adapter.clear();
    }

    private void onSuccess() {
        dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanEvent(ScanEvent event) {
        scanTask.start(event.getAddress());
    }

    @Override
    public void onFind(List<Device> devices) {
        if (!devices.isEmpty()) adapter.addAll(devices);
    }

    @Override
    public void onItemClick(Device item) {
        OkHttp.newCall(client, String.format(Locale.getDefault(), "%s/action?do=sync&mode=%s&type=%s", item.getIp(), binding.mode.getTag().toString(), type), body.build()).enqueue(getCallback());
    }

    @Override
    public boolean onLongClick(Device item) {
        String mode = binding.mode.getTag().toString();
        if (mode.equals("0")) return false;
        if (mode.equals("2") && type.equals("keep")) Keep.deleteAll();
        if (mode.equals("2") && type.equals("history")) History.delete(VodConfig.getCid());
        OkHttp.newCall(client, String.format(Locale.getDefault(), "%s/action?do=sync&mode=%s&type=%s&force=true", item.getIp(), binding.mode.getTag().toString(), type), body.build()).enqueue(getCallback());
        return true;
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                App.post(() -> onSuccess());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                App.post(() -> Notify.show(e.getMessage()));
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanTask.stop();
    }
}
