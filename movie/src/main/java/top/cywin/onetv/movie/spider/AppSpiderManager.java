package top.cywin.onetv.movie.spider;

import android.content.Context;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.proxy.ProxyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * App系列Spider管理器
 * 专门管理App系列Spider的加载和使用
 */
public class AppSpiderManager {
    private static final String TAG = "AppSpiderManager";
    
    private static AppSpiderManager instance;
    private Context context;
    private SpiderJarManager jarManager;
    private ProxyManager proxyManager;
    
    // App系列Spider配置
    private final Map<String, AppSpiderConfig> appSpiders;
    
    private AppSpiderManager() {
        this.appSpiders = new HashMap<>();
        initAppSpiders();
    }
    
    public static synchronized AppSpiderManager getInstance() {
        if (instance == null) {
            instance = new AppSpiderManager();
        }
        return instance;
    }
    
    public void init(Context context) {
        this.context = context;
        this.jarManager = SpiderJarManager.getInstance();
        this.proxyManager = ProxyManager.get();
        
        jarManager.init(context);
    }
    
    /**
     * 初始化App系列Spider配置
     */
    private void initAppSpiders() {
        // csp_AppYS - 影视App解析器
        appSpiders.put("AppYS", new AppSpiderConfig(
            "csp_AppYS",
            "AppYS影视",
            "通用App影视解析器",
            "app"
        ));
        
        // csp_AppTT - TT影视App解析器
        appSpiders.put("AppTT", new AppSpiderConfig(
            "csp_AppTT", 
            "TT影视",
            "TT影视App解析器",
            "app"
        ));
        
        // csp_AppYsV2 - 影视V2App解析器
        appSpiders.put("AppYsV2", new AppSpiderConfig(
            "csp_AppYsV2",
            "影视V2",
            "影视V2 App解析器",
            "app"
        ));
        
        // csp_AppMr - Mr影视App解析器
        appSpiders.put("AppMr", new AppSpiderConfig(
            "csp_AppMr",
            "Mr影视",
            "Mr影视App解析器", 
            "app"
        ));
        
        // csp_AppYuan - 圆圈影视App解析器
        appSpiders.put("AppYuan", new AppSpiderConfig(
            "csp_AppYuan",
            "圆圈影视",
            "圆圈影视App解析器",
            "app"
        ));
    }
    
    /**
     * 获取App Spider
     * @param spiderName Spider名称（如：AppYS）
     * @param ext 扩展参数
     * @return Spider实例
     */
    public Spider getAppSpider(String spiderName, String ext) {
        AppSpiderConfig config = appSpiders.get(spiderName);
        if (config == null) {
            return null;
        }
        
        String jarPath = jarManager.getJarPath(config.jarType);
        return proxyManager.getSpider(
            spiderName.toLowerCase(),
            config.className,
            ext,
            jarPath
        );
    }
    
    /**
     * 获取所有App系列Spider配置
     * @return App Spider配置Map
     */
    public Map<String, AppSpiderConfig> getAllAppSpiders() {
        return new HashMap<>(appSpiders);
    }
    
    /**
     * 检查App Spider是否可用
     * @param spiderName Spider名称
     * @return 是否可用
     */
    public boolean isAppSpiderAvailable(String spiderName) {
        try {
            Spider spider = getAppSpider(spiderName, "");
            return spider != null && !(spider instanceof top.cywin.onetv.movie.catvod.crawler.SpiderNull);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 测试所有App系列Spider
     * @return 测试结果
     */
    public Map<String, Boolean> testAllAppSpiders() {
        Map<String, Boolean> results = new HashMap<>();
        
        for (String spiderName : appSpiders.keySet()) {
            boolean available = isAppSpiderAvailable(spiderName);
            results.put(spiderName, available);
        }
        
        return results;
    }
    
    /**
     * 获取App Spider的首页内容
     * @param spiderName Spider名称
     * @param ext 扩展参数
     * @return 首页内容JSON
     */
    public String getAppSpiderHomeContent(String spiderName, String ext) {
        try {
            Spider spider = getAppSpider(spiderName, ext);
            if (spider != null) {
                return spider.homeContent(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }
    
    /**
     * 搜索App Spider内容
     * @param spiderName Spider名称
     * @param keyword 搜索关键词
     * @param ext 扩展参数
     * @return 搜索结果JSON
     */
    public String searchAppSpiderContent(String spiderName, String keyword, String ext) {
        try {
            Spider spider = getAppSpider(spiderName, ext);
            if (spider != null) {
                return spider.searchContent(keyword, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }
    
    /**
     * 获取App Spider详情
     * @param spiderName Spider名称
     * @param ids 内容ID
     * @param ext 扩展参数
     * @return 详情JSON
     */
    public String getAppSpiderDetail(String spiderName, String ids, String ext) {
        try {
            Spider spider = getAppSpider(spiderName, ext);
            if (spider != null) {
                return spider.detailContent(ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }
    
    /**
     * App Spider配置类
     */
    public static class AppSpiderConfig {
        public final String className;
        public final String displayName;
        public final String description;
        public final String jarType;
        
        public AppSpiderConfig(String className, String displayName, String description, String jarType) {
            this.className = className;
            this.displayName = displayName;
            this.description = description;
            this.jarType = jarType;
        }
        
        @Override
        public String toString() {
            return "AppSpiderConfig{" +
                    "className='" + className + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", description='" + description + '\'' +
                    ", jarType='" + jarType + '\'' +
                    '}';
        }
    }
}
