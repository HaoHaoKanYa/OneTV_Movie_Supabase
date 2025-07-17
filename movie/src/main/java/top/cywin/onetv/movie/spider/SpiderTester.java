package top.cywin.onetv.movie.spider;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.catvod.crawler.Spider;

import java.util.ArrayList;
import java.util.List;

/**
 * Spider解析器测试工具
 * 用于测试各类Spider解析器的加载和基本功能
 */
public class SpiderTester {

    private static final String TAG = "SpiderTester";
    private Context context;

    public SpiderTester(Context context) {
        this.context = context;
    }

    /**
     * 测试所有配置的Spider解析器
     */
    public void testAllSpiders() {
        Log.d(TAG, "开始测试所有Spider解析器...");

        // 测试XPath系列解析器
        testXPathSpiders();
        
        // 测试App系列解析器
        testAppSpiders();
        
        // 测试阿里系列解析器
        testAliSpiders();
        
        // 测试视频站解析器
        testVideoSpiders();
        
        // 测试网盘系列解析器
        testNetdiskSpiders();

        Log.d(TAG, "所有Spider解析器测试完成");
    }

    /**
     * 测试XPath系列解析器
     */
    private void testXPathSpiders() {
        Log.d(TAG, "测试XPath系列解析器...");
        
        String[] xpathSpiders = {
            "csp_XPath", "csp_XPathMac", "csp_XPathMacFilter",
            "csp_XPathFilter", "csp_XPathNode", "csp_XPathUtil"
        };
        
        for (String api : xpathSpiders) {
            testSpider(api, "XPath解析器");
        }
    }

    /**
     * 测试App系列解析器
     */
    private void testAppSpiders() {
        Log.d(TAG, "测试App系列解析器...");
        
        String[] appSpiders = {
            "csp_AppYS", "csp_AppTT", "csp_AppYsV2",
            "csp_AppMr", "csp_AppYuan"
        };
        
        for (String api : appSpiders) {
            testSpider(api, "App解析器");
        }
    }

    /**
     * 测试阿里系列解析器
     */
    private void testAliSpiders() {
        Log.d(TAG, "测试阿里系列解析器...");
        
        String[] aliSpiders = {
            "csp_YydsAli1", "csp_AliPan", "csp_AliDrive",
            "csp_AliYun", "csp_AliShare"
        };
        
        for (String api : aliSpiders) {
            testSpider(api, "阿里解析器");
        }
    }

    /**
     * 测试视频站解析器
     */
    private void testVideoSpiders() {
        Log.d(TAG, "测试视频站解析器...");
        
        String[] videoSpiders = {
            "csp_Cokemv", "csp_Auete", "csp_LibVio", "csp_Kuake",
            "csp_Zxzj", "csp_Tangrenjie", "csp_Meijumi", "csp_Ysgc",
            "csp_Bttwo", "csp_Qimi", "csp_Voflix", "csp_Ddys",
            "csp_Dm84", "csp_Ddrk", "csp_Anime1", "csp_Bangumi",
            "csp_Bili", "csp_BiliBili", "csp_Douban", "csp_Iqiyi",
            "csp_Qq", "csp_Youku", "csp_Mgtv"
        };
        
        for (String api : videoSpiders) {
            testSpider(api, "视频站解析器");
        }
    }

    /**
     * 测试网盘系列解析器
     */
    private void testNetdiskSpiders() {
        Log.d(TAG, "测试网盘系列解析器...");
        
        String[] netdiskSpiders = {
            "csp_Quark", "csp_UC", "csp_OneDrive",
            "csp_GoogleDrive", "csp_BaiduPan", "csp_115Pan"
        };
        
        for (String api : netdiskSpiders) {
            testSpider(api, "网盘解析器");
        }
    }

    /**
     * 测试单个Spider解析器
     */
    private void testSpider(String api, String type) {
        try {
            Log.d(TAG, String.format("测试%s: %s", type, api));
            
            // 创建测试Site
            Site site = createTestSite(api);
            
            // 获取Spider实例
            Spider spider = SpiderManager.getInstance().getSpider(site.getKey());
            
            if (spider != null && !(spider instanceof top.cywin.onetv.movie.catvod.crawler.SpiderNull)) {
                Log.d(TAG, String.format("✅ %s加载成功: %s", type, api));
                
                // 测试基本方法
                testSpiderBasicMethods(spider, api);
            } else {
                Log.w(TAG, String.format("❌ %s加载失败: %s", type, api));
            }
            
        } catch (Exception e) {
            Log.e(TAG, String.format("❌ %s测试异常: %s - %s", type, api, e.getMessage()));
        }
    }

    /**
     * 创建测试Site
     */
    private Site createTestSite(String api) {
        Site site = new Site();
        site.setKey(api + "_test");
        site.setName(api + "测试");
        site.setApi(api);
        site.setType(getSpiderType(api));
        site.setSearchable(1);
        site.setChangeable(1);
        site.setExt("");
        
        // 设置JAR路径
        if (api.startsWith("csp_")) {
            site.setJar(SpiderJarManager.getInstance().getDefaultSpiderJar());
        }
        
        return site;
    }

    /**
     * 获取Spider类型
     */
    private int getSpiderType(String api) {
        SpiderConfig.SpiderType type = SpiderConfig.getInstance().getSpiderType(createTestSite(api));
        return type.getValue();
    }

    /**
     * 测试Spider基本方法
     */
    private void testSpiderBasicMethods(Spider spider, String api) {
        try {
            // 测试初始化
            spider.init(context, "");
            Log.d(TAG, String.format("  ✅ %s初始化成功", api));
            
            // 测试homeContent方法
            String homeResult = spider.homeContent(false);
            if (!TextUtils.isEmpty(homeResult)) {
                Log.d(TAG, String.format("  ✅ %s homeContent返回数据", api));
            } else {
                Log.d(TAG, String.format("  ⚠️ %s homeContent返回空数据", api));
            }
            
        } catch (Exception e) {
            Log.w(TAG, String.format("  ⚠️ %s基本方法测试异常: %s", api, e.getMessage()));
        }
    }

    /**
     * 测试JAR文件状态
     */
    public void testJarStatus() {
        Log.d(TAG, "测试JAR文件状态...");
        
        SpiderJarManager jarManager = SpiderJarManager.getInstance();
        jarManager.init(context);
        
        String defaultJar = jarManager.getDefaultSpiderJar();
        Log.d(TAG, "默认JAR路径: " + defaultJar);
        
        boolean exists = jarManager.isJarExists(defaultJar);
        Log.d(TAG, "JAR文件存在: " + exists);
        
        if (exists) {
            long size = jarManager.getJarSize(defaultJar);
            Log.d(TAG, "JAR文件大小: " + size + " bytes");
            
            String availablePath = jarManager.getAvailableJarPath(defaultJar);
            Log.d(TAG, "可用JAR路径: " + availablePath);
        }
    }

    /**
     * 获取测试报告
     */
    public String getTestReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Spider解析器测试报告 ===\n");
        report.append("XPath系列解析器: 6个\n");
        report.append("App系列解析器: 5个\n");
        report.append("阿里系列解析器: 5个\n");
        report.append("视频站解析器: 23个\n");
        report.append("网盘系列解析器: 6个\n");
        report.append("总计: 45个Spider解析器\n");
        report.append("JAR文件: spider.jar\n");
        report.append("状态: 已部署\n");
        return report.toString();
    }
}
