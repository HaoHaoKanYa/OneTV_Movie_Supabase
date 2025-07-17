package top.cywin.onetv.movie.spider;

import android.text.TextUtils;

import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.api.loader.JarLoader;
import top.cywin.onetv.movie.api.loader.JsLoader;
import top.cywin.onetv.movie.api.loader.PyLoader;
import top.cywin.onetv.movie.api.loader.BaseLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Spider解析器管理类
 * 基于FongMi_TV架构实现
 */
public class SpiderManager {

    private static SpiderManager instance;
    private final Map<String, Spider> spiders;
    private final SpiderNull nullSpider;

    private SpiderManager() {
        this.spiders = new HashMap<>();
        this.nullSpider = new SpiderNull();
    }

    public static SpiderManager getInstance() {
        if (instance == null) {
            synchronized (SpiderManager.class) {
                if (instance == null) {
                    instance = new SpiderManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取Spider解析器
     */
    public Spider getSpider(String key) {
        if (TextUtils.isEmpty(key)) return nullSpider;
        
        Spider spider = spiders.get(key);
        if (spider != null) return spider;

        Site site = VodConfig.get().getSite(key);
        if (site == null) return nullSpider;

        return createSpider(site);
    }

    /**
     * 创建Spider解析器
     * 基于FongMi_TV的BaseLoader逻辑
     */
    private Spider createSpider(Site site) {
        try {
            Spider spider = null;
            SpiderConfig.SpiderType spiderType = SpiderConfig.getInstance().getSpiderType(site);

            switch (spiderType) {
                case JAR:
                    // 使用JarLoader加载JAR类型的Spider
                    spider = createJarSpider(site);
                    break;
                case JAVASCRIPT:
                    // 使用JsLoader加载JavaScript类型的Spider
                    spider = createJsSpider(site);
                    break;
                case PYTHON:
                    // 使用PyLoader加载Python类型的Spider
                    spider = createPySpider(site);
                    break;
                case XPATH:
                    // 创建XPath类型的Spider
                    spider = createXPathSpider(site);
                    break;
                case JSON:
                    // 创建JSON类型的Spider
                    spider = createJsonSpider(site);
                    break;
                default:
                    spider = nullSpider;
                    break;
            }

            if (spider != null && spider != nullSpider) {
                spiders.put(site.getKey(), spider);
            }

            return spider != null ? spider : nullSpider;

        } catch (Exception e) {
            e.printStackTrace();
            return nullSpider;
        }
    }

    /**
     * 创建JAR Spider（基于FongMi_TV的JarLoader）
     */
    private Spider createJarSpider(Site site) {
        try {
            // 使用默认的spider.jar文件路径
            String jarPath = "assets://jar/spider.jar";

            // 如果Site指定了特定的JAR文件，使用指定的JAR
            if (!TextUtils.isEmpty(site.getJar())) {
                jarPath = site.getJar();
            }

            // 使用BaseLoader获取Spider实例
            return BaseLoader.get().getSpider(site.getKey(), site.getApi(), site.getExt(), jarPath);
        } catch (Exception e) {
            e.printStackTrace();
            return nullSpider;
        }
    }

    /**
     * 创建JavaScript Spider（基于FongMi_TV的JsLoader）
     */
    private Spider createJsSpider(Site site) {
        try {
            // 使用默认的spider.jar文件路径（JavaScript文件通常也在JAR中）
            String jarPath = "assets://jar/spider.jar";

            // 如果Site指定了特定的JAR文件，使用指定的JAR
            if (!TextUtils.isEmpty(site.getJar())) {
                jarPath = site.getJar();
            }

            // 使用BaseLoader获取JavaScript Spider实例
            return BaseLoader.get().getSpider(site.getKey(), site.getApi(), site.getExt(), jarPath);
        } catch (Exception e) {
            e.printStackTrace();
            return nullSpider;
        }
    }

    /**
     * 创建Python Spider（基于FongMi_TV的PyLoader）
     */
    private Spider createPySpider(Site site) {
        try {
            // Python Spider通过PyLoader直接加载，不需要JAR文件
            return BaseLoader.get().getSpider(site.getKey(), site.getApi(), site.getExt(), "");
        } catch (Exception e) {
            e.printStackTrace();
            return nullSpider;
        }
    }

    /**
     * 创建XPath解析器
     */
    private Spider createXPathSpider(Site site) {
        // XPath解析器实现
        // 这里可以根据需要实现具体的XPath解析逻辑
        return nullSpider;
    }

    /**
     * 创建JSON解析器
     */
    private Spider createJsonSpider(Site site) {
        // JSON解析器实现
        // 这里可以根据需要实现具体的JSON解析逻辑
        return nullSpider;
    }

    /**
     * 清理Spider缓存
     */
    public void clear() {
        spiders.clear();
    }

    /**
     * 移除指定Spider
     */
    public void remove(String key) {
        spiders.remove(key);
    }

    /**
     * 检查Spider是否存在
     */
    public boolean contains(String key) {
        return spiders.containsKey(key);
    }

    /**
     * 获取Spider数量
     */
    public int size() {
        return spiders.size();
    }
}
