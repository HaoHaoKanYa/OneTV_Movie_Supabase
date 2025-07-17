package com.fongmi.onetv.tv.player.extractor;

import com.fongmi.onetv.tv.player.Source;

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
