package top.cywin.onetv.movie.spider;

import android.text.TextUtils;

import top.cywin.onetv.movie.bean.Site;

import java.util.HashMap;
import java.util.Map;

/**
 * Spider解析器配置管理
 * 基于FongMi_TV架构实现
 */
public class SpiderConfig {

    private static SpiderConfig instance;
    private final Map<String, SpiderInfo> spiderInfoMap;

    private SpiderConfig() {
        this.spiderInfoMap = new HashMap<>();
        initDefaultSpiders();
    }

    public static SpiderConfig getInstance() {
        if (instance == null) {
            synchronized (SpiderConfig.class) {
                if (instance == null) {
                    instance = new SpiderConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化默认Spider解析器配置
     * 基于FongMi_TV的Spider类型定义
     */
    private void initDefaultSpiders() {
        // XPath系列解析器
        addSpiderInfo("csp_XPath", SpiderType.XPATH, "XPath基础解析器");
        addSpiderInfo("csp_XPathMac", SpiderType.XPATH, "Mac版XPath解析器");
        addSpiderInfo("csp_XPathMacFilter", SpiderType.XPATH, "Mac版XPath过滤解析器");
        addSpiderInfo("csp_XPathFilter", SpiderType.XPATH, "XPath过滤解析器");
        addSpiderInfo("csp_XPathNode", SpiderType.XPATH, "XPath节点解析器");
        addSpiderInfo("csp_XPathUtil", SpiderType.XPATH, "XPath工具解析器");

        // App系列解析器
        addSpiderInfo("csp_AppYS", SpiderType.JAR, "影视App解析器");
        addSpiderInfo("csp_AppTT", SpiderType.JAR, "天堂App解析器");
        addSpiderInfo("csp_AppYsV2", SpiderType.JAR, "影视V2解析器");
        addSpiderInfo("csp_AppMr", SpiderType.JAR, "美人App解析器");
        addSpiderInfo("csp_AppYuan", SpiderType.JAR, "苹果App解析器");

        // 阿里系列解析器
        addSpiderInfo("csp_YydsAli1", SpiderType.JAR, "阿里云盘解析器1");
        addSpiderInfo("csp_AliPan", SpiderType.JAR, "阿里网盘解析器");
        addSpiderInfo("csp_AliDrive", SpiderType.JAR, "阿里云盘解析器");
        addSpiderInfo("csp_AliYun", SpiderType.JAR, "阿里云解析器");
        addSpiderInfo("csp_AliShare", SpiderType.JAR, "阿里分享解析器");

        // 视频站解析器
        addSpiderInfo("csp_Cokemv", SpiderType.JAR, "Coke影视解析器");
        addSpiderInfo("csp_Auete", SpiderType.JAR, "Auete解析器");
        addSpiderInfo("csp_LibVio", SpiderType.JAR, "LibVio解析器");
        addSpiderInfo("csp_Kuake", SpiderType.JAR, "夸克解析器");
        addSpiderInfo("csp_Zxzj", SpiderType.JAR, "在线之家解析器");
        addSpiderInfo("csp_Tangrenjie", SpiderType.JAR, "唐人街解析器");
        addSpiderInfo("csp_Meijumi", SpiderType.JAR, "美剧迷解析器");
        addSpiderInfo("csp_Ysgc", SpiderType.JAR, "影视工厂解析器");
        addSpiderInfo("csp_Bttwo", SpiderType.JAR, "BT之家解析器");
        addSpiderInfo("csp_Qimi", SpiderType.JAR, "奇米解析器");
        addSpiderInfo("csp_Voflix", SpiderType.JAR, "Voflix解析器");
        addSpiderInfo("csp_Ddys", SpiderType.JAR, "低端影视解析器");
        addSpiderInfo("csp_Dm84", SpiderType.JAR, "动漫巴士解析器");
        addSpiderInfo("csp_Ddrk", SpiderType.JAR, "低端影视解析器");
        addSpiderInfo("csp_Anime1", SpiderType.JAR, "动漫解析器1");
        addSpiderInfo("csp_Bangumi", SpiderType.JAR, "番组计划解析器");
        addSpiderInfo("csp_Bili", SpiderType.JAR, "哔哩哔哩解析器");
        addSpiderInfo("csp_BiliBili", SpiderType.JAR, "哔哩哔哩完整解析器");
        addSpiderInfo("csp_Douban", SpiderType.JAR, "豆瓣解析器");
        addSpiderInfo("csp_Iqiyi", SpiderType.JAR, "爱奇艺解析器");
        addSpiderInfo("csp_Qq", SpiderType.JAR, "腾讯视频解析器");
        addSpiderInfo("csp_Youku", SpiderType.JAR, "优酷解析器");
        addSpiderInfo("csp_Mgtv", SpiderType.JAR, "芒果TV解析器");

        // 网盘系列解析器
        addSpiderInfo("csp_Quark", SpiderType.JAR, "夸克网盘解析器");
        addSpiderInfo("csp_UC", SpiderType.JAR, "UC网盘解析器");
        addSpiderInfo("csp_OneDrive", SpiderType.JAR, "OneDrive解析器");
        addSpiderInfo("csp_GoogleDrive", SpiderType.JAR, "Google Drive解析器");
        addSpiderInfo("csp_BaiduPan", SpiderType.JAR, "百度网盘解析器");
        addSpiderInfo("csp_115Pan", SpiderType.JAR, "115网盘解析器");
    }

    private void addSpiderInfo(String api, SpiderType type, String description) {
        spiderInfoMap.put(api, new SpiderInfo(api, type, description));
    }

    /**
     * 获取Spider信息
     */
    public SpiderInfo getSpiderInfo(String api) {
        return spiderInfoMap.get(api);
    }

    /**
     * 判断是否为支持的Spider
     */
    public boolean isSupported(String api) {
        return !TextUtils.isEmpty(api) && spiderInfoMap.containsKey(api);
    }

    /**
     * 根据Site获取Spider类型
     */
    public SpiderType getSpiderType(Site site) {
        if (site == null || TextUtils.isEmpty(site.getApi())) {
            return SpiderType.UNKNOWN;
        }

        // 根据FongMi_TV的逻辑判断Spider类型
        String api = site.getApi();
        
        if (api.contains(".js")) {
            return SpiderType.JAVASCRIPT;
        } else if (api.contains(".py")) {
            return SpiderType.PYTHON;
        } else if (api.startsWith("csp_")) {
            SpiderInfo info = getSpiderInfo(api);
            return info != null ? info.getType() : SpiderType.JAR;
        }
        
        return SpiderType.UNKNOWN;
    }

    /**
     * Spider类型枚举
     */
    public enum SpiderType {
        JAR(0),
        JAVASCRIPT(1),
        PYTHON(2),
        XPATH(3),
        JSON(4),
        UNKNOWN(-1);

        private final int value;

        SpiderType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Spider信息类
     */
    public static class SpiderInfo {
        private final String api;
        private final SpiderType type;
        private final String description;

        public SpiderInfo(String api, SpiderType type, String description) {
            this.api = api;
            this.type = type;
            this.description = description;
        }

        public String getApi() {
            return api;
        }

        public SpiderType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }
}
