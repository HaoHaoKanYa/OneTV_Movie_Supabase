package top.cywin.onetv.vod.ui.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod
import top.cywin.onetv.vod.Updater;
import top.cywin.onetv.vod.api.config.LiveConfig;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.api.config.WallConfig;
import top.cywin.onetv.vod.bean.Config;
import top.cywin.onetv.vod.databinding.VodActivityHomeBinding;
import top.cywin.onetv.vod.db.AppDatabase;
import top.cywin.onetv.vod.event.RefreshEvent;
import top.cywin.onetv.vod.event.ServerEvent;
import top.cywin.onetv.vod.event.StateEvent;
import top.cywin.onetv.vod.impl.Callback;
import top.cywin.onetv.vod.player.Source;
import top.cywin.onetv.vod.receiver.ShortcutReceiver;
import top.cywin.onetv.vod.server.Server;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.custom.FragmentStateManager;
import top.cywin.onetv.vod.ui.fragment.SettingFragment;
import top.cywin.onetv.vod.ui.fragment.SettingPlayerFragment;
import com.fongmi.onetv.tv.ui.fragment.VodFragment;
import com.fongmi.onetv.tv.utils.FileChooser;
import com.fongmi.onetv.tv.utils.Notify;
import com.fongmi.onetv.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.android.material.navigation.NavigationBarView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private VodActivityHomeBinding mBinding;
    private int orientation;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        orientation = getResources().getConfiguration().orientation;
        Updater.create().release().start(this);
        initFragment(savedInstanceState);
        Server.get().start();
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
        mBinding.navigation.findViewById(R.id.live).setOnLongClickListener(this::addShortcut);
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(this, intent.getData()));
            } else {
                VideoActivity.push(this, intent.getData().toString());
            }
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) return VodFragment.newInstance();
                if (position == 1) return SettingFragment.newInstance();
                if (position == 2) return SettingPlayerFragment.newInstance();
                return null;
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                RefreshEvent.config();
                StateEvent.empty();
                Notify.show(msg);
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                openLive();
            }
        });
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl());
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private boolean addShortcut(View view) {
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, getString(R.string.vod_nav_live)).setIcon(IconCompat.createWithResource(this, R.mipmap.vod_ic_launcher)).setIntent(new Intent(Intent.ACTION_VIEW, null, this, LiveActivity.class)).setShortLabel(getString(R.string.vod_nav_live)).build();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, ShortcutReceiver.class).setAction(ShortcutReceiver.ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ShortcutManagerCompat.requestPinShortcut(this, info, pendingIntent.getIntentSender());
        return true;
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType().equals(RefreshEvent.Type.CONFIG)) setNavigation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.PUSH) return;
        VideoActivity.push(this, event.getText());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.live) return openLive();
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.post(() -> checkOrientation(newConfig), 100);
    }

    private void checkOrientation(Configuration newConfig) {
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation;
            RefreshEvent.video();
        }
    }

    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (mManager.isVisible(2)) {
            change(1);
        } else if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        WallConfig.get().clear();
        LiveConfig.get().clear();
        VodConfig.get().clear();
        OkHttp.get().clear();
        AppDatabase.backup();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
