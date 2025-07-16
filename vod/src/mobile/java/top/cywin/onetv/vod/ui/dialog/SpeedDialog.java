package top.cywin.onetv.vod.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.VodDialogSpeedBinding;
import top.cywin.onetv.vod.impl.SpeedCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SpeedDialog {

    private final VodDialogSpeedBinding binding;
    private final SpeedCallback callback;
    private float value;

    public static SpeedDialog create(Fragment fragment) {
        return new SpeedDialog(fragment);
    }

    public SpeedDialog(Fragment fragment) {
        this.callback = (SpeedCallback) fragment;
        this.binding = VodDialogSpeedBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.vod_player_speed).setView(binding.getRoot()).setPositiveButton(R.string.vod_dialog_positive, this::onPositive).setNegativeButton(R.string.vod_dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getSpeed());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setSpeed(binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setSpeed(value);
        dialog.dismiss();
    }
}
