package top.cywin.onetv.movie.impl;

import top.cywin.onetv.movie.bean.Site;

public interface SiteCallback {

    void setSite(Site item);

    void onChanged();
}
