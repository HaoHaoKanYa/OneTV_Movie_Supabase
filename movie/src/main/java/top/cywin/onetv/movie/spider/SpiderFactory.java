package top.cywin.onetv.movie.spider;

import android.text.TextUtils;

import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;

/**
 * Spider工厂类
 * 基于FongMi_TV架构实现
 */
public class SpiderFactory {

    /**
     * 创建Spider实例
     */
    public static Spider create(Site site) {
        if (site == null || TextUtils.isEmpty(site.getKey())) {
            return new SpiderNull();
        }

        return SpiderManager.getInstance().getSpider(site.getKey());
    }

    /**
     * 根据类型创建Spider
     */
    public static Spider create(int type, String key, String api, String ext) {
        Site site = new Site();
        site.setType(type);
        site.setKey(key);
        site.setApi(api);
        site.setExt(ext);
        
        return create(site);
    }

    /**
     * 创建JAR Spider
     */
    public static Spider createJar(String key, String api, String ext) {
        return create(0, key, api, ext);
    }

    /**
     * 创建JAR Spider（指定JAR路径）
     */
    public static Spider createJar(String key, String api, String ext, String jarPath) {
        Site site = new Site();
        site.setType(0);
        site.setKey(key);
        site.setApi(api);
        site.setExt(ext);
        site.setJar(jarPath);

        return create(site);
    }

    /**
     * 创建JavaScript Spider
     */
    public static Spider createJs(String key, String api, String ext) {
        return create(1, key, api, ext);
    }

    /**
     * 创建Python Spider
     */
    public static Spider createPy(String key, String api, String ext) {
        return create(2, key, api, ext);
    }

    /**
     * 创建XPath Spider
     */
    public static Spider createXPath(String key, String api, String ext) {
        return create(3, key, api, ext);
    }

    /**
     * 创建JSON Spider
     */
    public static Spider createJson(String key, String api, String ext) {
        return create(4, key, api, ext);
    }

    /**
     * 获取空Spider
     */
    public static Spider createNull() {
        return new SpiderNull();
    }
}
