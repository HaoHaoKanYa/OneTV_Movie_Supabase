package top.cywin.onetv.movie.player.extractor;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;

import top.cywin.onetv.movie.App;
import top.cywin.onetv.movie.player.Source;
import top.cywin.onetv.movie.forcetech.Util;
import top.cywin.onetv.movie.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.util.HashSet;
import java.util.regex.Pattern;

import okhttp3.Headers;

public class Force implements Source.Extractor, ServiceConnection {

    private static final Pattern PATTERN = Pattern.compile("(?i)(p[2-9]p|mitv)");
    private final HashSet<String> set = new HashSet<>();

    @Override
    public boolean match(String scheme, String host) {
        return PATTERN.matcher(scheme).find();
    }

    private void init(String scheme) {
        App.get().bindService(Util.intent(App.get(), scheme), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public String fetch(String url) throws Exception {
        String scheme = Util.scheme(url);
        if (!set.contains(scheme)) init(scheme);
        while (!set.contains(scheme)) SystemClock.sleep(10);
        Uri uri = Uri.parse(url);
        int port = Util.port(scheme);
        String id = uri.getLastPathSegment();
        String cmd = "http://127.0.0.1:" + port + "/cmd.xml?cmd=switch_chan&server=" + uri.getHost() + ":" + uri.getPort() + "&id=" + id;
        String result = "http://127.0.0.1:" + port + "/" + id;
        OkHttp.newCall(cmd, Headers.of(HttpHeaders.USER_AGENT, "MTV")).execute().body().string();
        return result;
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
        try {
            if (!set.isEmpty()) App.get().unbindService(this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            set.clear();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        set.add(Util.trans(name));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        set.remove(Util.trans(name));
    }
}
