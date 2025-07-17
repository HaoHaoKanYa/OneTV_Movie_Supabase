package top.cywin.onetv.movie.test;

import android.content.Context;
import android.util.Log;

import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.bean.Parse;
import top.cywin.onetv.movie.config.VodConfigDeployer;
import top.cywin.onetv.movie.impl.Callback;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.proxy.ProxyManager;

/**
 * VOD配置测试器
 * 用于测试OneTV官方影视接口的部署和功能
 */
public class VodConfigTester {
    private static final String TAG = "VodConfigTester";
    
    /**
     * 完整测试VOD配置
     * @param context 上下文
     */
    public static void runFullTest(Context context) {
        Log.d(TAG, "=== 开始完整VOD配置测试 ===");
        
        // 1. 测试配置部署
        testConfigDeployment(context);
        
        // 2. 测试配置状态
        testConfigStatus();
        
        // 3. 测试站点功能
        testSiteFunctionality();
        
        // 4. 测试解析器功能
        testParserFunctionality();
        
        Log.d(TAG, "=== VOD配置测试完成 ===");
    }
    
    /**
     * 测试配置部署
     */
    private static void testConfigDeployment(Context context) {
        Log.d(TAG, "--- 测试配置部署 ---");
        
        VodConfigDeployer.deployOnetvApiConfig(context, new Callback() {
            @Override
            public void success() {
                Log.d(TAG, "✅ 配置部署成功");
                testConfigContent();
            }
            
            @Override
            public void error(String msg) {
                Log.e(TAG, "❌ 配置部署失败: " + msg);
            }
        });
    }
    
    /**
     * 测试配置内容
     */
    private static void testConfigContent() {
        Log.d(TAG, "--- 测试配置内容 ---");
        
        try {
            // 检查站点数量
            int siteCount = VodConfig.get().getSites().size();
            Log.d(TAG, "站点数量: " + siteCount);
            
            if (siteCount > 0) {
                Log.d(TAG, "✅ 站点配置正常");
                
                // 列出前5个站点
                for (int i = 0; i < Math.min(5, siteCount); i++) {
                    Site site = VodConfig.get().getSites().get(i);
                    Log.d(TAG, String.format("站点%d: %s (%s)", i+1, site.getName(), site.getKey()));
                }
            } else {
                Log.e(TAG, "❌ 没有找到站点配置");
            }
            
            // 检查解析器数量
            int parseCount = VodConfig.get().getParses().size();
            Log.d(TAG, "解析器数量: " + parseCount);
            
            if (parseCount > 0) {
                Log.d(TAG, "✅ 解析器配置正常");
                
                // 列出前3个解析器
                for (int i = 0; i < Math.min(3, parseCount); i++) {
                    Parse parse = VodConfig.get().getParses().get(i);
                    Log.d(TAG, String.format("解析器%d: %s (%s)", i+1, parse.getName(), parse.getUrl()));
                }
            } else {
                Log.w(TAG, "⚠️ 没有找到解析器配置");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试配置内容时发生异常", e);
        }
    }
    
    /**
     * 测试配置状态
     */
    private static void testConfigStatus() {
        Log.d(TAG, "--- 测试配置状态 ---");
        
        try {
            String status = VodConfigDeployer.getConfigStatus();
            Log.d(TAG, status);
            
            boolean isLoaded = VodConfigDeployer.isConfigLoaded();
            Log.d(TAG, "配置加载状态: " + (isLoaded ? "✅ 已加载" : "❌ 未加载"));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试配置状态时发生异常", e);
        }
    }
    
    /**
     * 测试站点功能
     */
    private static void testSiteFunctionality() {
        Log.d(TAG, "--- 测试站点功能 ---");
        
        try {
            Site homeSite = VodConfig.get().getHome();
            if (homeSite != null) {
                Log.d(TAG, "默认站点: " + homeSite.getName());
                
                // 测试Spider加载
                testSpiderLoading(homeSite);
                
            } else {
                Log.w(TAG, "⚠️ 没有设置默认站点");
                
                // 尝试使用第一个站点
                if (!VodConfig.get().getSites().isEmpty()) {
                    Site firstSite = VodConfig.get().getSites().get(0);
                    Log.d(TAG, "使用第一个站点进行测试: " + firstSite.getName());
                    testSpiderLoading(firstSite);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试站点功能时发生异常", e);
        }
    }
    
    /**
     * 测试Spider加载
     */
    private static void testSpiderLoading(Site site) {
        Log.d(TAG, "--- 测试Spider加载 ---");
        
        try {
            Log.d(TAG, "测试站点: " + site.getName());
            Log.d(TAG, "站点API: " + site.getApi());
            Log.d(TAG, "站点类型: " + site.getType());
            
            // 尝试获取Spider
            Spider spider = site.spider();
            if (spider != null) {
                Log.d(TAG, "✅ Spider加载成功: " + spider.getClass().getSimpleName());
                
                // 测试基本功能
                testSpiderBasicFunction(spider, site);
                
            } else {
                Log.e(TAG, "❌ Spider加载失败");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试Spider加载时发生异常", e);
        }
    }
    
    /**
     * 测试Spider基本功能
     */
    private static void testSpiderBasicFunction(Spider spider, Site site) {
        Log.d(TAG, "--- 测试Spider基本功能 ---");
        
        try {
            // 测试首页内容
            String homeContent = spider.homeContent(false);
            if (homeContent != null && !homeContent.isEmpty() && !homeContent.equals("{}")) {
                Log.d(TAG, "✅ 首页内容获取成功，长度: " + homeContent.length());
                Log.d(TAG, "首页内容预览: " + homeContent.substring(0, Math.min(200, homeContent.length())) + "...");
            } else {
                Log.w(TAG, "⚠️ 首页内容为空或无效");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试Spider基本功能时发生异常", e);
        }
    }
    
    /**
     * 测试解析器功能
     */
    private static void testParserFunctionality() {
        Log.d(TAG, "--- 测试解析器功能 ---");
        
        try {
            if (!VodConfig.get().getParses().isEmpty()) {
                Parse firstParse = VodConfig.get().getParses().get(0);
                Log.d(TAG, "测试解析器: " + firstParse.getName());
                Log.d(TAG, "解析器URL: " + firstParse.getUrl());
                Log.d(TAG, "✅ 解析器配置正常");
            } else {
                Log.w(TAG, "⚠️ 没有可用的解析器");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试解析器功能时发生异常", e);
        }
    }
    
    /**
     * 快速测试配置是否可用
     * @return 是否可用
     */
    public static boolean quickTest() {
        try {
            return VodConfigDeployer.isConfigLoaded() && 
                   !VodConfig.get().getSites().isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "快速测试失败", e);
            return false;
        }
    }
}
