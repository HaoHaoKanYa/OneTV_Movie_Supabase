package top.cywin.onetv.vod.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.VodDialogUaBinding;
import top.cywin.onetv.vod.impl.UaCallback;
import top.cywin.onetv.vod.ui.custom.CustomTextListener;
import com.github.catvod.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UaDialog {

    private final VodDialogUaBinding binding;
    private final UaCallback callback;
    private AlertDialog dialog;
    private boolean append;

    public static UaDialog create(Fragment fragment) {
        return new UaDialog(fragment);
    }

    public UaDialog(Fragment fragment) {
        this.callback = (UaCallback) fragment;
        this.binding = VodDialogUaBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.append = true;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.vod_player_ua).setView(binding.getRoot()).setPositiveButton(R.string.vod_dialog_positive, this::onPositive).setNegativeButton(R.string.vod_dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        String text = Setting.getUa();
        binding.text.setText(text);
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    private void initEvent() {
        binding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void detect(String s) {
        if (append && "c".equalsIgnoreCase(s)) {
            append = false;
            binding.text.setText(Util.CHROME);
        } else if (append && "o".equalsIgnoreCase(s)) {
            append = false;
            binding.text.setText(Util.OKHTTP);
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setUa(binding.text.getText().toString().trim());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
