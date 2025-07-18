package top.cywin.onetv.movie.player.extractor;

import top.cywin.onetv.movie.player.Source;

public class Video implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return "video".equals(scheme);
    }

    @Override
    public String fetch(String url) throws Exception {
        return url.substring(8);
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
