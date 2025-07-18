package top.cywin.onetv.vod.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.BuildConfig;
import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.Updater;
import top.cywin.onetv.vod.api.config.LiveConfig;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.api.config.WallConfig;
import top.cywin.onetv.vod.bean.Config;
import top.cywin.onetv.vod.bean.Live;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.databinding.VodActivitySettingBinding;
import top.cywin.onetv.vod.db.AppDatabase;
import top.cywin.onetv.vod.event.RefreshEvent;
import top.cywin.onetv.vod.impl.Callback;
import top.cywin.onetv.vod.impl.ConfigCallback;
import top.cywin.onetv.vod.impl.DohCallback;
import top.cywin.onetv.vod.impl.LiveCallback;
import top.cywin.onetv.vod.impl.ProxyCallback;
import top.cywin.onetv.vod.impl.SiteCallback;
import top.cywin.onetv.vod.player.Source;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.dialog.ConfigDialog;
import top.cywin.onetv.vod.ui.dialog.DohDialog;
import top.cywin.onetv.vod.ui.dialog.HistoryDialog;
import top.cywin.onetv.vod.ui.dialog.LiveDialog;
import top.cywin.onetv.vod.ui.dialog.ProxyDialog;
import top.cywin.onetv.vod.ui.dialog.RestoreDialog;
import top.cywin.onetv.vod.ui.dialog.SiteDialog;
import top.cywin.onetv.vod.utils.FileChooser;
import top.cywin.onetv.vod.utils.FileUtil;
import top.cywin.onetv.vod.utils.Notify;
import top.cywin.onetv.vod.utils.ResUtil;
import top.cywin.onetv.vod.utils.UrlUtil;
import top.github.catvod.bean.Doh;
import top.github.catvod.net.OkHttp;
import top.github.catvod.utils.Path;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SettingActivity extends BaseActivity implements ConfigCallback, SiteCallback, LiveCallback, DohCallback, ProxyCallback {

    private VodActivitySettingBinding mBinding;
    private String[] quality;
    private String[] size;
    private int type;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.vod_setting_on : R.string.vod_setting_off);
    }

    private String getProxy(String proxy) {
        return proxy.isEmpty() ? getString(R.string.vod_none) : UrlUtil.scheme(proxy);
    }

    private int getDohIndex() {
        return Math.max(0, VodConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : VodConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.vod.requestFocus();
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        setCacheText();
        setOtherText();
    }

    private void setOtherText() {
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.proxyText.setText(getProxy(Setting.getProxy()));
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.vod_select_size))[Setting.getSize()]);
        mBinding.qualityText.setText((quality = ResUtil.getStringArray(R.array.vod_select_quality))[Setting.getQuality()]);
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.proxy.setOnClickListener(this::onProxy);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.backup.setOnClickListener(this::onBackup);
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.restore.setOnClickListener(this::onRestore);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.vod.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.live.setOnLongClickListener(this::onLiveEdit);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
        mBinding.wall.setOnLongClickListener(this::onWallEdit);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.incognito.setOnClickListener(this::setIncognito);
        mBinding.quality.setOnClickListener(this::setQuality);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.doh.setOnClickListener(this::setDoh);
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(this);
                VodConfig.load(config, getCallback(0));
                mBinding.vodUrl.setText(config.getDesc());
                break;
            case 1:
                Notify.progress(this);
                LiveConfig.load(config, getCallback(1));
                mBinding.liveUrl.setText(config.getDesc());
                break;
            case 2:
                Notify.progress(this);
                WallConfig.load(config, getCallback(2));
                mBinding.wallUrl.setText(config.getDesc());
                break;
        }
    }

    private Callback getCallback(int type) {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                setConfig(type);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                setConfig(type);
            }
        };
    }

    private void setConfig(int type) {
        switch (type) {
            case 0:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                RefreshEvent.history();
                mBinding.vodUrl.setText(VodConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
            case 1:
                setCacheText();
                Notify.dismiss();
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                break;
            case 2:
                setCacheText();
                Notify.dismiss();
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void onVod(View view) {
        ConfigDialog.create(this).type(type = 0).show();
    }

    private void onLive(View view) {
        ConfigDialog.create(this).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private boolean onVodEdit(View view) {
        ConfigDialog.create(this).type(type = 0).edit().show();
        return true;
    }

    private boolean onLiveEdit(View view) {
        ConfigDialog.create(this).type(type = 1).edit().show();
        return true;
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create(this).type(type = 2).edit().show();
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).action().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).action().show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }

    private void onPlayer(View view) {
        SettingPlayerActivity.start(this);
    }

    private void onVersion(View view) {
        Updater.create().force().release().start(this);
    }

    private boolean onVersionDev(View view) {
        Updater.create().force().dev().start(this);
        return true;
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 4 ? 1 : Setting.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(this);
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setIncognito(View view) {
        Setting.putIncognito(!Setting.isIncognito());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
    }

    private void setQuality(View view) {
        int index = Setting.getQuality();
        Setting.putQuality(index = index == quality.length - 1 ? 0 : ++index);
        mBinding.qualityText.setText(quality[index]);
        RefreshEvent.image();
    }

    private void setSize(View view) {
        int index = Setting.getSize();
        Setting.putSize(index = index == size.length - 1 ? 0 : ++index);
        mBinding.sizeText.setText(size[index]);
        RefreshEvent.size();
    }

    private void setDoh(View view) {
        DohDialog.create(this).index(getDohIndex()).show();
    }

    @Override
    public void setDoh(Doh doh) {
        Source.get().stop();
        OkHttp.get().setDoh(doh);
        Notify.progress(getActivity());
        Setting.putDoh(doh.toString());
        mBinding.dohText.setText(doh.getName());
        VodConfig.load(Config.vod(), getCallback(0));
    }

    private void onProxy(View view) {
        ProxyDialog.create(this).show();
    }

    @Override
    public void setProxy(String proxy) {
        Source.get().stop();
        Setting.putProxy(proxy);
        OkHttp.get().setProxy(proxy);
        Notify.progress(getActivity());
        mBinding.proxyText.setText(getProxy(proxy));
        VodConfig.load(Config.vod(), getCallback(0));
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }

    private void onBackup(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.backup(new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.vod_backup_success);
            }

            @Override
            public void error() {
                Notify.show(R.string.vod_backup_fail);
            }
        }));
    }

    private void onRestore(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> RestoreDialog.create(this).show(new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.vod_restore_success);
                Notify.progress(getActivity());
                setOtherText();
                initConfig();
            }

            @Override
            public void error() {
                Notify.show(R.string.vod_restore_fail);
            }
        }));
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback(0));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        setConfig(Config.find("file:/" + FileChooser.getPathFromUri(this, data.getData()).replace(Path.rootPath(), ""), type));
    }
}
