package top.cywin.onetv.vod;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import top.cywin.onetv.vod.databinding.VodDialogUpdateBinding;
import top.cywin.onetv.vod.utils.Download;
import top.cywin.onetv.vod.utils.FileUtil;
import top.cywin.onetv.vod.utils.Notify;
import top.cywin.onetv.vod.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private VodDialogUpdateBinding binding;
    private final Download download;
    private AlertDialog dialog;
    private boolean dev;

    private File getFile() {
        return Path.cache("update.apk");
    }

    private String getJson() {
        return Github.getJson(dev, "leanback");
    }

    private String getApk() {
        return Github.getApk(dev, "leanback" + "-" + "arm64_v8a");
    }

    public static Updater create() {
        return new Updater();
    }

    public Updater() {
        this.download = Download.create(getApk(), getFile(), this);
    }

    public Updater force() {
        Notify.show(R.string.vod_update_check);
        Setting.putUpdate(true);
        return this;
    }

    public Updater release() {
        this.dev = false;
        return this;
    }

    public Updater dev() {
        this.dev = true;
        return this;
    }

    private Updater check() {
        dismiss();
        return this;
    }

    public void start(Activity activity) {
        App.execute(() -> doInBackground(activity));
    }

    private boolean need(int code, String name) {
        return Setting.getUpdate() && (dev ? !name.equals(BuildConfig.VERSION_NAME) && code >= BuildConfig.VERSION_CODE : code > BuildConfig.VERSION_CODE);
    }

    private void doInBackground(Activity activity) {
        try {
            JSONObject object = new JSONObject(OkHttp.string(getJson()));
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            if (need(code, name)) App.post(() -> show(activity, name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(Activity activity, String version, String desc) {
        binding = VodDialogUpdateBinding.inflate(LayoutInflater.from(activity));
        check().create(activity, ResUtil.getString(R.string.vod_update_version, version)).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        binding.desc.setText(desc);
    }

    private AlertDialog create(Activity activity, String title) {
        return dialog = new MaterialAlertDialogBuilder(activity).setTitle(title).setView(binding.getRoot()).setPositiveButton(R.string.vod_update_confirm, null).setNegativeButton(R.string.vod_dialog_negative, null).setCancelable(false).create();
    }

    private void cancel(View view) {
        Setting.putUpdate(false);
        download.cancel();
        dialog.dismiss();
    }

    private void confirm(View view) {
        view.setEnabled(false);
        download.start();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String msg) {
        Notify.show(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        dismiss();
    }
}
