package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import top.cywin.onetv.vod.databinding.VodDialogDescBinding;
import top.cywin.onetv.vod.ui.custom.CustomMovement;
import com.github.bassaer.library.MDColor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DescDialog {

    public static void show(Activity activity, CharSequence desc) {
        new DescDialog().create(activity, desc);
    }

    public void create(Activity activity, CharSequence desc) {
        VodDialogDescBinding binding = VodDialogDescBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        initView(binding.text, desc);
        dialog.show();
    }

    private void initView(TextView view, CharSequence desc) {
        view.setText(desc, TextView.BufferType.SPANNABLE);
        view.setLinkTextColor(MDColor.BLUE_500);
        CustomMovement.bind(view);
    }
}
