package top.cywin.onetv.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import tip.cywin.onetv.tv.R;
import top.cywin.onetv.tv.databinding.DialogLinkBinding;
import top.cywin.onetv.tv.ui.activity.VideoActivity;
import top.cywin.onetv.tv.utils.FileChooser;
import top.cywin.onetv.tv.utils.Sniffer;
import top.cywin.onetv.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LinkDialog {

    private final DialogLinkBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;

    public static LinkDialog create(Fragment fragment) {
        return new LinkDialog(fragment);
    }

    public LinkDialog(Fragment fragment) {
        this.fragment = fragment;
        this.binding = DialogLinkBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.play).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        CharSequence text = Util.getClipText();
        binding.text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.MAX_VALUE)});
        if (!TextUtils.isEmpty(text)) binding.text.setText(Sniffer.getUrl(text.toString()));
    }

    private void initEvent() {
        binding.input.setEndIconOnClickListener(this::onChoose);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void onChoose(View view) {
        FileChooser.from(fragment).show();
        dialog.dismiss();
    }

    private void onPositive(DialogInterface dialog, int which) {
        String text = binding.text.getText().toString().trim();
        if (!text.isEmpty()) VideoActivity.start(fragment.getActivity(), text);
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
