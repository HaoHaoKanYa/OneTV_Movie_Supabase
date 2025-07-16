package top.cywin.onetv.vod.ui.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.databinding.VodActivityCrashBinding;
import top.cywin.onetv.vod.ui.base.BaseActivity;

import java.util.Objects;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class CrashActivity extends BaseActivity {

    private VodActivityCrashBinding mBinding;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityCrashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEvent() {
        mBinding.details.setOnClickListener(v -> showError());
        mBinding.restart.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(this, Objects.requireNonNull(CustomActivityOnCrash.getConfigFromIntent(getIntent()))));
    }

    private void showError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.vod_crash_details_title)
                .setMessage(CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, getIntent()))
                .setPositiveButton(R.string.vod_crash_details_close, null)
                .show();
    }
}
