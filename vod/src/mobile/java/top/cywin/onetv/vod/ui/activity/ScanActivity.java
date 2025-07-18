package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.databinding.VodActivityScanBinding;
import top.cywin.onetv.vod.event.ScanEvent;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.utils.Util;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.List;

public class ScanActivity extends BaseActivity implements BarcodeCallback {

    private VodActivityScanBinding mBinding;
    private CaptureManager mCapture;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, ScanActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityScanBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.hideSystemUI(this);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mCapture = new CaptureManager(this, mBinding.scanner);
        mBinding.scanner.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(List.of(BarcodeFormat.QR_CODE)));
    }

    @Override
    public void barcodeResult(BarcodeResult result) {
        if (!result.getText().startsWith("http")) return;
        ScanEvent.post(result.getText());
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.hideSystemUI(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Util.hideSystemUI(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCapture.onResume();
        mBinding.scanner.decodeSingle(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCapture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCapture.onDestroy();
    }
}
