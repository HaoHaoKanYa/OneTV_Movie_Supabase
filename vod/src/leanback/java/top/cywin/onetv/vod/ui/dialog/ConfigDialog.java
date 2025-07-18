package top.cywin.onetv.vod.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.api.config.LiveConfig;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.api.config.WallConfig;
import top.cywin.onetv.vod.bean.Config;
import top.cywin.onetv.vod.databinding.VodDialogConfigBinding;
import top.cywin.onetv.vod.event.ServerEvent;
import top.cywin.onetv.vod.impl.ConfigCallback;
import top.cywin.onetv.vod.server.Server;
import top.cywin.onetv.vod.ui.custom.CustomTextListener;
import top.cywin.onetv.vod.utils.FileChooser;
import top.cywin.onetv.vod.utils.QRCode;
import top.cywin.onetv.vod.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ConfigDialog implements DialogInterface.OnDismissListener {

    private final VodDialogConfigBinding binding;
    private final FragmentActivity activity;
    private final ConfigCallback callback;
    private final AlertDialog dialog;
    private boolean append;
    private boolean edit;
    private String url;
    private int type;

    public static ConfigDialog create(FragmentActivity activity) {
        return new ConfigDialog(activity);
    }

    public ConfigDialog type(int type) {
        this.type = type;
        return this;
    }

    public ConfigDialog edit() {
        this.edit = true;
        return this;
    }

    public ConfigDialog(FragmentActivity activity) {
        this.activity = activity;
        this.callback = (ConfigCallback) activity;
        this.binding = VodDialogConfigBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.append = true;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.55f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void initView() {
        binding.text.setText(url = getUrl());
        binding.text.setSelection(TextUtils.isEmpty(url) ? 0 : url.length());
        binding.positive.setText(edit ? R.string.vod_dialog_edit : R.string.vod_dialog_positive);
        binding.code.setImageBitmap(QRCode.getBitmap(Server.get().getAddress(3), 200, 0));
        binding.info.setText(ResUtil.getString(R.string.vod_push_info, Server.get().getAddress()).replace("，", "\n"));
    }

    private void initEvent() {
        EventBus.getDefault().register(this);
        binding.choose.setOnClickListener(this::onChoose);
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private String getUrl() {
        switch (type) {
            case 0:
                return VodConfig.getUrl();
            case 1:
                return LiveConfig.getUrl();
            case 2:
                return WallConfig.getUrl();
            default:
                return "";
        }
    }

    private void onChoose(View view) {
        FileChooser.from(activity).show();
        dialog.dismiss();
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.text.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            binding.text.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            binding.text.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(View view) {
        String name = binding.name.getText().toString().trim();
        String text = binding.text.getText().toString().trim();
        if (edit) Config.find(url, type).url(text).update();
        if (text.isEmpty()) Config.delete(url, type);
        if (name.isEmpty()) callback.setConfig(Config.find(text, type));
        else callback.setConfig(Config.find(text, name, type));
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.SETTING) return;
        binding.name.setText(event.getName());
        binding.text.setText(event.getText());
        binding.text.setSelection(binding.text.getText().length());
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        EventBus.getDefault().unregister(this);
    }
}
