package top.cywin.onetv.vod.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.DialogSpeedBinding;
import top.cywin.onetv.vod.impl.SpeedCallback;
import top.cywin.onetv.vod.utils.KeyUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SpeedDialog {

    private final DialogSpeedBinding binding;
    private final SpeedCallback callback;
    private final AlertDialog dialog;

    public static SpeedDialog create(FragmentActivity activity) {
        return new SpeedDialog(activity);
    }

    public SpeedDialog(FragmentActivity activity) {
        this.callback = (SpeedCallback) activity;
        this.binding = DialogSpeedBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(Setting.getSpeed());
    }

    private void initEvent() {
        binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setSpeed(value));
        binding.slider.setOnKeyListener((view, keyCode, event) -> {
            boolean enter = KeyUtil.isEnterKey(event);
            if (enter) dialog.dismiss();
            return enter;
        });
    }
}
