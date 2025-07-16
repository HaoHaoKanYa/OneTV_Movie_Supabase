package top.cywin.onetv.vod.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.VodDialogBufferBinding;
import top.cywin.onetv.vod.impl.BufferCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BufferDialog {

    private final VodDialogBufferBinding binding;
    private final BufferCallback callback;
    private int value;

    public static BufferDialog create(Fragment fragment) {
        return new BufferDialog(fragment);
    }

    public BufferDialog(Fragment fragment) {
        this.callback = (BufferCallback) fragment;
        this.binding = VodDialogBufferBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.vod_player_buffer).setView(binding.getRoot()).setPositiveButton(R.string.vod_dialog_positive, this::onPositive).setNegativeButton(R.string.vod_dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getBuffer());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setBuffer((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setBuffer(value);
        dialog.dismiss();
    }
}
