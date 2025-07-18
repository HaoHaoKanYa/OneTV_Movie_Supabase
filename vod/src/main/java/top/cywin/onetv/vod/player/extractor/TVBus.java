package top.cywin.onetv.vod.player.extractor;

import android.net.Uri;
import android.text.TextUtils;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.api.config.LiveConfig;
import top.cywin.onetv.vod.bean.Core;
import top.cywin.onetv.vod.exception.ExtractException;
import top.cywin.onetv.vod.player.Source;
import top.cywin.onetv.vod.utils.ResUtil;
import top.github.catvod.net.OkHttp;
import top.github.catvod.utils.Path;
import com.google.gson.JsonObject;
import top.tvbus.engine.Listener;
import top.tvbus.engine.TVCore;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class TVBus implements Source.Extractor, Listener {

    private CountDownLatch latch;
    private volatile String hls;
    private TVCore tvcore;
    private Core core;

    @Override
    public boolean match(String scheme, String host) {
        return "tvbus".equals(scheme);
    }

    private void init(Core core) {
        try {
            App.get().setHook(core.getHook());
            tvcore = new TVCore(getPath(core.getSo())).listener(this).auth(core.getAuth()).name(core.getName()).pass(core.getPass()).domain(core.getDomain()).broker(core.getBroker()).serv(0).play(8902).mode(1).init();
        } catch (Exception ignored) {
        } finally {
            App.get().setHook(null);
        }
    }

    private String getPath(String url) {
        String name = Uri.parse(url).getLastPathSegment();
        if (TextUtils.isEmpty(name)) name = "tvcore.so";
        File file = new File(Path.so(), name);
        if (file.length() < 10240) Path.write(file, OkHttp.bytes(url));
        return file.getAbsolutePath();
    }

    @Override
    public String fetch(String url) throws Exception {
        Core c = LiveConfig.get().getHome().getCore();
        if (core != null && !core.equals(c)) change();
        if (tvcore == null) init(core = c);
        latch = new CountDownLatch(1);
        tvcore.start(url);
        latch.await();
        return check();
    }

    private void change() throws Exception {
        Setting.putBootLive(true);
        App.post(() -> System.exit(0), 100);
        throw new ExtractException(ResUtil.getString(R.string.vod_error_play_url));
    }

    private String check() throws Exception {
        if (hls == null) return "";
        if (!hls.startsWith("-")) return hls;
        throw new ExtractException(ResUtil.getString(R.string.vod_error_play_code, hls));
    }

    @Override
    public void stop() {
        if (tvcore != null) tvcore.stop();
        hls = null;
    }

    @Override
    public void exit() {
        if (tvcore != null) tvcore.quit();
        tvcore = null;
    }

    @Override
    public void onPrepared(String result) {
        JsonObject json = App.gson().fromJson(result, JsonObject.class);
        if (json.get("hls") == null) return;
        hls = json.get("hls").getAsString();
        latch.countDown();
    }

    @Override
    public void onStop(String result) {
        JsonObject json = App.gson().fromJson(result, JsonObject.class);
        hls = json.get("errno").getAsString();
        if (hls.startsWith("-")) latch.countDown();
    }

    @Override
    public void onInited(String result) {
    }

    @Override
    public void onStart(String result) {
    }

    @Override
    public void onInfo(String result) {
    }

    @Override
    public void onQuit(String result) {
    }
}
