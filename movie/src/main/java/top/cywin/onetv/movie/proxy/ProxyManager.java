package top.cywin.onetv.movie.proxy;

import android.text.TextUtils;

import top.cywin.onetv.movie.api.config.LiveConfig;
import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.bean.Live;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.catvod.utils.Util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * 多语言代理管理器
 * 基于FongMi_TV的BaseLoader完整移植，支持Java、Python、JavaScript代理
 */
public class ProxyManager {
    private static final String TAG = "ProxyManager";

    private final JavaProxyLoader javaLoader;
    private final PythonProxyLoader pythonLoader;
    private final JavaScriptProxyLoader jsLoader;

    private static class Loader {
        static volatile ProxyManager INSTANCE = new ProxyManager();
    }

    public static ProxyManager get() {
        return Loader.INSTANCE;
    }

    private ProxyManager() {
        this.javaLoader = new JavaProxyLoader();
        this.pythonLoader = new PythonProxyLoader();
        this.jsLoader = new JavaScriptProxyLoader();
    }

    /**
     * 清理所有代理加载器
     */
    public void clear() {
        this.javaLoader.clear();
        this.pythonLoader.clear();
        this.jsLoader.clear();
    }

    /**
     * 获取Spider实例
     * @param key 站点key
     * @param api API地址
     * @param ext 扩展参数
     * @param jar JAR包地址
     * @return Spider实例
     */
    public Spider getSpider(String key, String api, String ext, String jar) {
        boolean js = api.contains(".js");
        boolean py = api.contains(".py");
        boolean csp = api.startsWith("csp_");
        
        if (py) {
            return pythonLoader.getSpider(key, api, ext);
        } else if (js) {
            return jsLoader.getSpider(key, api, ext, jar);
        } else if (csp) {
            return javaLoader.getSpider(key, api, ext, jar);
        } else {
            return new SpiderNull();
        }
    }

    /**
     * 通过参数获取Spider实例
     * @param params 参数Map
     * @return Spider实例
     */
    public Spider getSpider(Map<String, String> params) {
        if (!params.containsKey("siteKey")) {
            return new SpiderNull();
        }
        
        String siteKey = params.get("siteKey");
        Live live = LiveConfig.get().getLive(siteKey);
        Site site = VodConfig.get().getSite(siteKey);
        
        if (!site.isEmpty()) {
            return site.spider();
        }
        if (!live.isEmpty()) {
            return live.spider();
        }
        
        return new SpiderNull();
    }

    /**
     * 设置最近使用的代理
     * @param key 站点key
     * @param api API地址
     * @param jar JAR包地址
     */
    public void setRecent(String key, String api, String jar) {
        boolean js = api.contains(".js");
        boolean py = api.contains(".py");
        boolean csp = api.startsWith("csp_");
        
        if (js) {
            jsLoader.setRecent(key);
        } else if (py) {
            pythonLoader.setRecent(key);
        } else if (csp) {
            javaLoader.setRecent(Util.md5(jar));
        }
    }

    /**
     * 本地代理调用
     * @param params 参数Map
     * @return 代理结果
     */
    public Object[] proxyLocal(Map<String, String> params) {
        String doType = params.get("do");
        
        if ("js".equals(doType)) {
            return jsLoader.proxyInvoke(params);
        } else if ("py".equals(doType)) {
            return pythonLoader.proxyInvoke(params);
        } else {
            return javaLoader.proxyInvoke(params);
        }
    }

    /**
     * 解析JAR包
     * @param jar JAR包地址
     * @param recent 是否设为最近使用
     */
    public void parseJar(String jar, boolean recent) {
        if (TextUtils.isEmpty(jar)) {
            return;
        }
        
        String jarMd5 = Util.md5(jar);
        javaLoader.parseJar(jarMd5, jar);
        
        if (recent) {
            javaLoader.setRecent(jarMd5);
        }
    }

    /**
     * 获取Dex类加载器
     * @param jar JAR包地址
     * @return DexClassLoader
     */
    public DexClassLoader dex(String jar) {
        return javaLoader.dex(jar);
    }

    /**
     * JSON扩展调用
     * @param key 站点key
     * @param jxs 扩展参数
     * @param url 目标URL
     * @return JSON结果
     * @throws Throwable 异常
     */
    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        return javaLoader.jsonExt(key, jxs, url);
    }

    /**
     * JSON扩展混合调用
     * @param flag 标志
     * @param key 站点key
     * @param name 名称
     * @param jxs 扩展参数
     * @param url 目标URL
     * @return JSON结果
     * @throws Throwable 异常
     */
    public JSONObject jsonExtMix(String flag, String key, String name, 
                                LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        return javaLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    /**
     * 获取Java代理加载器
     */
    public JavaProxyLoader getJavaLoader() {
        return javaLoader;
    }

    /**
     * 获取Python代理加载器
     */
    public PythonProxyLoader getPythonLoader() {
        return pythonLoader;
    }

    /**
     * 获取JavaScript代理加载器
     */
    public JavaScriptProxyLoader getJsLoader() {
        return jsLoader;
    }

    /**
     * 检查代理类型
     * @param api API地址
     * @return 代理类型
     */
    public ProxyType getProxyType(String api) {
        if (api.contains(".js")) {
            return ProxyType.JAVASCRIPT;
        } else if (api.contains(".py")) {
            return ProxyType.PYTHON;
        } else if (api.startsWith("csp_")) {
            return ProxyType.JAVA;
        } else {
            return ProxyType.UNKNOWN;
        }
    }

    /**
     * 代理类型枚举
     */
    public enum ProxyType {
        JAVA, PYTHON, JAVASCRIPT, UNKNOWN
    }
}
