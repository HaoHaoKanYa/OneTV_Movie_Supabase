package top.cywin.onetv.vod.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.DialogBufferBinding;
import top.cywin.onetv.vod.impl.BufferCallback;
import top.cywin.onetv.vod.utils.KeyUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BufferDialog {

    private final DialogBufferBinding binding;
    private final BufferCallback callback;
    private final AlertDialog dialog;

    public static BufferDialog create(FragmentActivity activity) {
        return new BufferDialog(activity);
    }

    public BufferDialog(FragmentActivity activity) {
        this.callback = (BufferCallback) activity;
        this.binding = DialogBufferBinding.inflate(LayoutInflater.from(activity));
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
        binding.slider.setValue(Setting.getBuffer());
    }

    private void initEvent() {
        binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setBuffer((int) value));
        binding.slider.setOnKeyListener((view, keyCode, event) -> {
            boolean enter = KeyUtil.isEnterKey(event);
            if (enter) dialog.dismiss();
            return enter;
        });
    }
}
