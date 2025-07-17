package top.cywin.onetv.movie.spider.engine;

import android.content.Context;
import android.text.TextUtils;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.spider.SpiderConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Spider引擎管理器
 * 基于FongMi_TV架构实现统一的Spider引擎管理
 */
public class SpiderEngineManager {

    private static SpiderEngineManager instance;
    private final ConcurrentHashMap<String, Spider> spiders;
    private final SpiderRegistry registry;
    private final SpiderLifecycleManager lifecycleManager;
    private Context context;

    private SpiderEngineManager() {
        this.spiders = new ConcurrentHashMap<>();
        this.registry = new SpiderRegistry();
        this.lifecycleManager = new SpiderLifecycleManager();
    }

    public static SpiderEngineManager getInstance() {
        if (instance == null) {
            synchronized (SpiderEngineManager.class) {
                if (instance == null) {
                    instance = new SpiderEngineManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        registry.init(context);
        lifecycleManager.init(context);
    }

    /**
     * 获取Spider实例
     */
    public Spider getSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getKey())) {
            return new SpiderNull();
        }

        String key = site.getKey();
        
        // 检查缓存
        if (spiders.containsKey(key)) {
            Spider spider = spiders.get(key);
            if (lifecycleManager.isAlive(spider)) {
                return spider;
            } else {
                // 移除失效的Spider
                removeSpider(key);
            }
        }

        // 创建新的Spider
        return createSpider(site);
    }

    /**
     * 创建Spider实例
     */
    private Spider createSpider(Site site) {
        try {
            SpiderConfig.SpiderType type = SpiderConfig.getInstance().getSpiderType(site);
            Spider spider = null;

            switch (type) {
                case JAR:
                    spider = createJarSpider(site);
                    break;
                case JAVASCRIPT:
                    spider = createJsSpider(site);
                    break;
                case PYTHON:
                    spider = createPySpider(site);
                    break;
                case XPATH:
                    spider = createXPathSpider(site);
                    break;
                case JSON:
                    spider = createJsonSpider(site);
                    break;
                default:
                    spider = new SpiderNull();
                    break;
            }

            if (spider != null && !(spider instanceof SpiderNull)) {
                // 注册Spider
                registry.register(site.getKey(), spider, type);
                
                // 管理生命周期
                lifecycleManager.onCreate(spider);
                
                // 缓存Spider
                spiders.put(site.getKey(), spider);
                
                return spider;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new SpiderNull();
    }

    /**
     * 创建JAR Spider
     */
    private Spider createJarSpider(Site site) {
        try {
            // 使用JAR加载器创建Spider
            return top.cywin.onetv.movie.api.loader.BaseLoader.get()
                .getSpider(site.getKey(), site.getApi(), site.getExt(), 
                    !TextUtils.isEmpty(site.getJar()) ? site.getJar() : "assets://jar/spider.jar");
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 创建JavaScript Spider
     */
    private Spider createJsSpider(Site site) {
        try {
            return JsSpiderManager.getInstance().getJsSpider(site);
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 创建Python Spider
     */
    private Spider createPySpider(Site site) {
        try {
            return top.cywin.onetv.movie.api.loader.BaseLoader.get()
                .getSpider(site.getKey(), site.getApi(), site.getExt(), "");
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 创建XPath Spider
     */
    private Spider createXPathSpider(Site site) {
        try {
            return XPathSpiderManager.getInstance().getXPathSpider(site);
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 创建JSON Spider
     */
    private Spider createJsonSpider(Site site) {
        // JSON Spider暂未实现，返回空Spider
        return new SpiderNull();
    }

    /**
     * 移除Spider
     */
    public void removeSpider(String key) {
        if (TextUtils.isEmpty(key)) return;

        Spider spider = spiders.remove(key);
        if (spider != null) {
            // 注销Spider
            registry.unregister(key);
            
            // 管理生命周期
            lifecycleManager.onDestroy(spider);
        }
    }

    /**
     * 清理所有Spider
     */
    public void clear() {
        for (String key : spiders.keySet()) {
            removeSpider(key);
        }
        spiders.clear();
        registry.clear();
        lifecycleManager.clear();
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
     * 获取引擎状态
     */
    public String getEngineStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Spider引擎管理器状态:\n");
        status.append("- 总Spider数量: ").append(spiders.size()).append("\n");
        status.append("- 注册表状态: ").append(registry.getStatus()).append("\n");
        status.append("- 生命周期管理: ").append(lifecycleManager.getStatus()).append("\n");
        
        if (!spiders.isEmpty()) {
            status.append("- 已加载的Spider:\n");
            for (String key : spiders.keySet()) {
                Spider spider = spiders.get(key);
                SpiderConfig.SpiderInfo info = SpiderConfig.getInstance().getSpiderInfo(
                    registry.getSpiderApi(key));
                status.append("  * ").append(key)
                    .append(" (").append(info != null ? info.getDescription() : "Unknown").append(")\n");
            }
        }
        
        return status.toString();
    }

    /**
     * 预加载Spider
     */
    public void preloadSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getKey())) return;

        // 在后台线程预加载
        new Thread(() -> {
            try {
                getSpider(site);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 获取Spider注册表
     */
    public SpiderRegistry getRegistry() {
        return registry;
    }

    /**
     * 获取生命周期管理器
     */
    public SpiderLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    /**
     * 健康检查
     */
    public void healthCheck() {
        // 检查所有Spider的健康状态
        for (String key : spiders.keySet()) {
            Spider spider = spiders.get(key);
            if (!lifecycleManager.isAlive(spider)) {
                removeSpider(key);
            }
        }
    }

    /**
     * 获取Spider统计信息
     */
    public SpiderStats getStats() {
        SpiderStats stats = new SpiderStats();
        stats.totalSpiders = spiders.size();
        stats.jarSpiders = 0;
        stats.jsSpiders = 0;
        stats.pySpiders = 0;
        stats.xpathSpiders = 0;
        stats.jsonSpiders = 0;

        for (String key : spiders.keySet()) {
            String api = registry.getSpiderApi(key);
            SpiderConfig.SpiderType type = SpiderConfig.getInstance().getSpiderType(
                createSiteFromApi(key, api));
            
            switch (type) {
                case JAR:
                    stats.jarSpiders++;
                    break;
                case JAVASCRIPT:
                    stats.jsSpiders++;
                    break;
                case PYTHON:
                    stats.pySpiders++;
                    break;
                case XPATH:
                    stats.xpathSpiders++;
                    break;
                case JSON:
                    stats.jsonSpiders++;
                    break;
            }
        }

        return stats;
    }

    /**
     * 从API创建Site对象
     */
    private Site createSiteFromApi(String key, String api) {
        Site site = new Site();
        site.setKey(key);
        site.setApi(api);
        return site;
    }

    /**
     * Spider统计信息类
     */
    public static class SpiderStats {
        public int totalSpiders;
        public int jarSpiders;
        public int jsSpiders;
        public int pySpiders;
        public int xpathSpiders;
        public int jsonSpiders;

        @Override
        public String toString() {
            return String.format("SpiderStats{total=%d, jar=%d, js=%d, py=%d, xpath=%d, json=%d}",
                totalSpiders, jarSpiders, jsSpiders, pySpiders, xpathSpiders, jsonSpiders);
        }
    }
}
