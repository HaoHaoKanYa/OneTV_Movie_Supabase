package top.cywin.onetv.movie.spider.engine;

import android.content.Context;
import android.text.TextUtils;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.api.loader.JsLoader;
import top.cywin.onetv.movie.bean.Site;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaScript Spider管理器
 * 基于FongMi_TV的JsLoader架构实现
 */
public class JsSpiderManager {

    private static JsSpiderManager instance;
    private final ConcurrentHashMap<String, Spider> spiders;
    private final JsLoader jsLoader;
    private Context context;

    private JsSpiderManager() {
        this.spiders = new ConcurrentHashMap<>();
        this.jsLoader = new JsLoader();
    }

    public static JsSpiderManager getInstance() {
        if (instance == null) {
            synchronized (JsSpiderManager.class) {
                if (instance == null) {
                    instance = new JsSpiderManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
    }

    /**
     * 获取JavaScript Spider
     * 基于FongMi_TV的JsLoader.getSpider方法
     */
    public Spider getJsSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getApi())) {
            return new SpiderNull();
        }

        String key = site.getKey();
        if (spiders.containsKey(key)) {
            return spiders.get(key);
        }

        try {
            // 使用JsLoader创建JavaScript Spider
            Spider spider = jsLoader.getSpider(
                site.getKey(),
                site.getApi(),
                site.getExt(),
                getJarPath(site)
            );

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
     * 获取JAR路径
     */
    private String getJarPath(Site site) {
        if (!TextUtils.isEmpty(site.getJar())) {
            return site.getJar();
        }
        
        // 默认使用spider.jar
        return "assets://jar/spider.jar";
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
        jsLoader.clear();
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
     * 设置最近使用的JavaScript文件
     */
    public void setRecentJs(String recent) {
        jsLoader.setRecent(recent);
    }

    /**
     * 预加载JavaScript Spider
     */
    public void preloadSpider(Site site) {
        if (site == null || TextUtils.isEmpty(site.getApi()) || !site.getApi().contains(".js")) {
            return;
        }

        // 在后台线程预加载
        new Thread(() -> {
            try {
                getJsSpider(site);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 获取JavaScript引擎状态
     */
    public String getEngineStatus() {
        StringBuilder status = new StringBuilder();
        status.append("JavaScript引擎状态:\n");
        status.append("- 已加载Spider数量: ").append(spiders.size()).append("\n");
        status.append("- 引擎类型: QuickJS\n");
        status.append("- 支持格式: .js文件\n");
        
        if (!spiders.isEmpty()) {
            status.append("- 已加载的Spider:\n");
            for (String key : spiders.keySet()) {
                status.append("  * ").append(key).append("\n");
            }
        }
        
        return status.toString();
    }

    /**
     * 验证JavaScript文件
     */
    public boolean validateJsFile(String jsContent) {
        if (TextUtils.isEmpty(jsContent)) {
            return false;
        }

        // 基本的JavaScript语法检查
        return jsContent.contains("function") || 
               jsContent.contains("var") || 
               jsContent.contains("let") || 
               jsContent.contains("const");
    }

    /**
     * 获取JavaScript Spider信息
     */
    public SpiderInfo getSpiderInfo(String key) {
        Spider spider = spiders.get(key);
        if (spider == null) {
            return null;
        }

        SpiderInfo info = new SpiderInfo();
        info.key = key;
        info.type = "JavaScript";
        info.engine = "QuickJS";
        info.loaded = true;
        info.className = spider.getClass().getSimpleName();
        
        return info;
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
