package top.cywin.onetv.vod.impl;

import top.cywin.onetv.vod.bean.Site;

public interface SiteCallback {

    void setSite(Site item);

    void onChanged();
}
