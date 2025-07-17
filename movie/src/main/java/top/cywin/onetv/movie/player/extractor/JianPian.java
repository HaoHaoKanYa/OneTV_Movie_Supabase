package com.fongmi.onetv.tv.player.extractor;

import android.net.Uri;

import com.fongmi.onetv.tv.App;
import com.fongmi.onetv.tv.player.Source;
import com.fongmi.onetv.tv.utils.Clock;
import com.fongmi.onetv.tv.utils.FileUtil;
import top.cywin.onetv.movie.catvod.utils.Path;
import top.cywin.onetv.movie.jianpian.P2PClass;

import java.net.URLDecoder;
import java.net.URLEncoder;

public class JianPian implements Source.Extractor, Clock.Callback {

    private P2PClass p2p;
    private String path;
    private Clock clock;

    @Override
    public boolean match(String scheme, String host) {
        return "tvbox-xg".equals(scheme) || "jianpian".equals(scheme) || "ftp".equals(scheme);
    }

    private void init() {
        if (p2p == null) p2p = new P2PClass();
        if (clock == null) clock = Clock.create();
    }

    @Override
    public String fetch(String url) throws Exception {
        init();
        stop();
        check(10);
        start(url);
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(path).getLastPathSegment(), "GBK");
    }

    private void check(int limit) {
        double cache = FileUtil.getDirectorySize(Path.jpa());
        double total = cache + FileUtil.getAvailableStorageSpace(Path.jpa());
        int percent = (int) (cache / total * 100);
        if (percent > limit) Path.clear(Path.jpa());
    }

    private void start(String url) {
        try {
            path = URLDecoder.decode(url).split("\\|")[0];
            path = path.replace("jianpian://pathtype=url&path=", "");
            path = path.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
            path = path.replace("xg://", "ftp://").replace("xgplay://", "ftp://");
            p2p.P2Pdoxstart(path.getBytes("GBK"));
            clock.setCallback(this);
            clock.stop().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (clock != null) clock.stop();
            if (p2p == null || path == null) return;
            p2p.P2Pdoxpause(path.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            path = null;
        }
    }

    @Override
    public void exit() {
        App.execute(() -> check(10));
        if (clock != null) clock.release();
    }

    @Override
    public void onTimeChanged() {
        long seconds = System.currentTimeMillis() / 1000 % 60;
        if (seconds % 30 == 0) App.execute(() -> check(60));
    }
}
