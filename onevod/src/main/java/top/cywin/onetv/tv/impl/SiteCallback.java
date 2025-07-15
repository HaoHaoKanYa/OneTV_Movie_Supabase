package top.cywin.onetv.tv.impl;

import top.cywin.onetv.tv.bean.Site;

public interface SiteCallback {

    void setSite(Site item);

    void onChanged();
}
