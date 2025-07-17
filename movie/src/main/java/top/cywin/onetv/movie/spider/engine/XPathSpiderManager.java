package top.cywin.onetv.movie.spider.engine;

import android.content.Context;
import android.text.TextUtils;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.bean.Site;

import java.util.concurrent.ConcurrentHashMap;

/**
 * XPath Spider管理器
 * 基于FongMi_TV架构实现XPath解析器管理
 */
public class XPathSpiderManager {

    private static XPathSpiderManager instance;
    private final ConcurrentHashMap<String, Spider> spiders;
    private Context context;

    private XPathSpiderManager() {
        this.spiders = new ConcurrentHashMap<>();
    }

    public static XPathSpiderManager getInstance() {
        if (instance == null) {
            synchronized (XPathSpiderManager.class) {
                if (instance == null) {
                    instance = new XPathSpiderManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
    }

    /**
     * 获取XPath Spider
     */
    public Spider getXPathSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getApi())) {
            return new SpiderNull();
        }

        String key = site.getKey();
        if (spiders.containsKey(key)) {
            return spiders.get(key);
        }

        try {
            Spider spider = createXPathSpider(site);
            if (spider != null && !(spider instanceof SpiderNull)) {
                spiders.put(key, spider);
                return spider;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new SpiderNull();
    }

    /**
     * 创建XPath Spider
     */
    private Spider createXPathSpider(Site site) {
        try {
            XPathEngine engine = new XPathEngine();
            engine.init(context, site.getExt());
            return engine;
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 移除Spider缓存
     */
    public void removeSpider(String key) {
        if (!TextUtils.isEmpty(key)) {
            Spider spider = spiders.remove(key);
            if (spider != null) {
                try {
                    spider.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 清理所有Spider缓存
     */
    public void clear() {
        for (Spider spider : spiders.values()) {
            try {
                spider.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        spiders.clear();
    }

    /**
     * 获取Spider数量
     */
    public int getSpiderCount() {
        return spiders.size();
    }

    /**
     * 检查Spider是否存在
     */
    public boolean containsSpider(String key) {
        return !TextUtils.isEmpty(key) && spiders.containsKey(key);
    }

    /**
     * 预加载XPath Spider
     */
    public void preloadSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getApi()) || !isXPathSpider(site.getApi())) {
            return;
        }

        // 在后台线程预加载
        new Thread(() -> {
            try {
                getXPathSpider(site);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 检查是否为XPath Spider
     */
    public boolean isXPathSpider(String api) {
        if (TextUtils.isEmpty(api)) {
            return false;
        }
        
        return api.startsWith("csp_XPath");
    }

    /**
     * 获取XPath引擎状态
     */
    public String getEngineStatus() {
        StringBuilder status = new StringBuilder();
        status.append("XPath引擎状态:\n");
        status.append("- 已加载Spider数量: ").append(spiders.size()).append("\n");
        status.append("- 引擎类型: XPath\n");
        status.append("- 支持解析器: csp_XPath系列\n");
        
        if (!spiders.isEmpty()) {
            status.append("- 已加载的Spider:\n");
            for (String key : spiders.keySet()) {
                status.append("  * ").append(key).append("\n");
            }
        }
        
        return status.toString();
    }

    /**
     * 验证XPath配置
     */
    public boolean validateXPathConfig(String config) {
        if (TextUtils.isEmpty(config)) {
            return false;
        }

        // 基本的XPath配置检查
        return config.contains("selector") || 
               config.contains("xpath") || 
               config.contains("homeUrl");
    }

    /**
     * 获取XPath Spider信息
     */
    public SpiderInfo getSpiderInfo(String key) {
        Spider spider = spiders.get(key);
        if (spider == null) {
            return null;
        }

        SpiderInfo info = new SpiderInfo();
        info.key = key;
        info.type = "XPath";
        info.engine = "XPath";
        info.loaded = true;
        info.className = spider.getClass().getSimpleName();
        
        return info;
    }

    /**
     * 创建XPath配置模板
     */
    public String createXPathConfigTemplate() {
        return "{\n" +
               "  \"homeUrl\": \"https://example.com\",\n" +
               "  \"homeListSelector\": \".movie-list .item\",\n" +
               "  \"titleSelector\": \".title\",\n" +
               "  \"linkSelector\": \"a\",\n" +
               "  \"picSelector\": \"img\",\n" +
               "  \"categoryUrlTemplate\": \"https://example.com/category/{tid}/page/{pg}\",\n" +
               "  \"detailUrlTemplate\": \"https://example.com/detail/{id}\",\n" +
               "  \"searchUrlTemplate\": \"https://example.com/search?q={key}\",\n" +
               "  \"detailNameSelector\": \".detail-title\"\n" +
               "}";
    }

    /**
     * Spider信息类
     */
    public static class SpiderInfo {
        public String key;
        public String type;
        public String engine;
        public boolean loaded;
        public String className;
        
        @Override
        public String toString() {
            return String.format("SpiderInfo{key='%s', type='%s', engine='%s', loaded=%s, className='%s'}", 
                key, type, engine, loaded, className);
        }
    }
}
