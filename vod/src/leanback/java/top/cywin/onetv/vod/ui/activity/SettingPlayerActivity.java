package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.VodActivitySettingPlayerBinding;
import top.cywin.onetv.vod.impl.BufferCallback;
import top.cywin.onetv.vod.impl.SpeedCallback;
import top.cywin.onetv.vod.impl.UaCallback;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.dialog.BufferDialog;
import top.cywin.onetv.vod.ui.dialog.SpeedDialog;
import top.cywin.onetv.vod.ui.dialog.UaDialog;
import top.cywin.onetv.vod.utils.ResUtil;

import java.text.DecimalFormat;

public class SettingPlayerActivity extends BaseActivity implements UaCallback, BufferCallback, SpeedCallback {

    private VodActivitySettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] caption;
    private String[] render;
    private String[] scale;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.vod_setting_on : R.string.vod_setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setVisible();
        format = new DecimalFormat("0.#");
        mBinding.render.requestFocus();
        mBinding.uaText.setText(Setting.getUa());
        mBinding.aacText.setText(getSwitch(Setting.isPreferAAC()));
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        mBinding.speedText.setText(format.format(Setting.getSpeed()));
        mBinding.bufferText.setText(String.valueOf(Setting.getBuffer()));
        mBinding.backgroundText.setText(getSwitch(Setting.isBackgroundOn()));
        mBinding.audioDecodeText.setText(getSwitch(Setting.isAudioPrefer()));
        mBinding.danmakuLoadText.setText(getSwitch(Setting.isDanmakuLoad()));
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.vod_select_scale))[Setting.getScale()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.vod_select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.vod_select_caption))[Setting.isCaption() ? 1 : 0]);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.aac.setOnClickListener(this::setAAC);
        mBinding.scale.setOnClickListener(this::setScale);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.audioDecode.setOnClickListener(this::setAudioDecode);
        mBinding.danmakuLoad.setOnClickListener(this::setDanmakuLoad);
    }

    private void setVisible() {
        if (Setting.getBackground() == 2) Setting.putBackground(1);
        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }

    private void setAAC(View view) {
        Setting.putPreferAAC(!Setting.isPreferAAC());
        mBinding.aacText.setText(getSwitch(Setting.isPreferAAC()));
    }

    private void setScale(View view) {
        int index = Setting.getScale();
        Setting.putScale(index = index == scale.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(scale[index]);
    }

    private void onSpeed(View view) {
        SpeedDialog.create(this).show();
    }

    @Override
    public void setSpeed(float speed) {
        mBinding.speedText.setText(format.format(speed));
        Setting.putSpeed(speed);
    }

    private void onBuffer(View view) {
        BufferDialog.create(this).show();
    }

    @Override
    public void setBuffer(int times) {
        mBinding.bufferText.setText(String.valueOf(times));
        Setting.putBuffer(times);
    }

    private void setRender(View view) {
        int index = Setting.getRender();
        Setting.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
        if (Setting.isTunnel() && Setting.getRender() == 1) setTunnel(view);
    }

    private void setTunnel(View view) {
        Setting.putTunnel(!Setting.isTunnel());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        if (Setting.isTunnel() && Setting.getRender() == 1) setRender(view);
    }

    private void setCaption(View view) {
        Setting.putCaption(!Setting.isCaption());
        mBinding.captionText.setText(caption[Setting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (Setting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return Setting.isCaption();
    }

    private void setAudioDecode(View view) {
        Setting.putAudioPrefer(!Setting.isAudioPrefer());
        mBinding.audioDecodeText.setText(getSwitch(Setting.isAudioPrefer()));
    }

    private void setDanmakuLoad(View view) {
        Setting.putDanmakuLoad(!Setting.isDanmakuLoad());
        mBinding.danmakuLoadText.setText(getSwitch(Setting.isDanmakuLoad()));
    }

    private void onBackground(View view) {
        Setting.putBackground(Setting.isBackgroundOn() ? 0 : 1);
        mBinding.backgroundText.setText(getSwitch(Setting.isBackgroundOn()));
    }
}
