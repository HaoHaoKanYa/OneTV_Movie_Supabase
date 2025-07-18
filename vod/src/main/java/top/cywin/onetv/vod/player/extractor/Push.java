package top.cywin.onetv.vod.player.extractor;

import android.os.SystemClock;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.player.Source;
import top.cywin.onetv.vod.ui.activity.VideoActivity;

public class Push implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return "push".equals(scheme);
    }

    @Override
    public String fetch(String url) throws Exception {
        if (App.activity() != null) VideoActivity.start(App.activity(), url.substring(7));
        SystemClock.sleep(500);
        return "";
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
