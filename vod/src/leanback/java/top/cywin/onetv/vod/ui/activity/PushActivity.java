package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.databinding.VodActivityPushBinding;
import top.cywin.onetv.vod.server.Server;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.utils.QRCode;
import top.cywin.onetv.vod.utils.ResUtil;
import top.cywin.onetv.vod.utils.Sniffer;
import top.cywin.onetv.vod.utils.Util;

public class PushActivity extends BaseActivity {

    private VodActivityPushBinding mBinding;

    public static void start(Activity activity) {
        start(activity, 2);
    }

    public static void start(Activity activity, int tab) {
        Intent intent = new Intent(activity, PushActivity.class);
        intent.putExtra("tab", tab);
        activity.startActivity(intent);
    }

    private int getTab() {
        return getIntent().getIntExtra("tab", 2);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityPushBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.code.setImageBitmap(QRCode.getBitmap(Server.get().getAddress(getTab()), 250, 1));
        mBinding.info.setText(ResUtil.getString(R.string.vod_push_info, Server.get().getAddress()));
    }

    @Override
    protected void initEvent() {
        mBinding.code.setOnClickListener(this::onCode);
        mBinding.clip.setOnClickListener(this::onClip);
    }

    private void onClip(View view) {
        CharSequence text = Util.getClipText();
        if (!TextUtils.isEmpty(text)) VideoActivity.start(this, Sniffer.getUrl(text.toString()));
    }

    private void onCode(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Server.get().getAddress(getTab())));
        startActivity(intent);
    }
}
