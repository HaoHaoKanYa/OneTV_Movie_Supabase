package top.cywin.onetv.movie.proxy;

import android.text.TextUtils;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderNull;
import top.cywin.onetv.movie.catvod.utils.Path;
import top.cywin.onetv.movie.catvod.utils.Util;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * Java代理加载器
 * 基于FongMi_TV的JarLoader完整移植
 */
public class JavaProxyLoader {
    private static final String TAG = "JavaProxyLoader";

    private final Map<String, DexClassLoader> loaders;
    private final Map<String, Spider> spiders;
    private String recent;

    public JavaProxyLoader() {
        this.loaders = new ConcurrentHashMap<>();
        this.spiders = new ConcurrentHashMap<>();
        this.recent = "";
    }

    /**
     * 清理所有加载器和Spider
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
        loaders.clear();
        recent = "";
    }

    /**
     * 获取Spider实例
     * @param key 站点key
     * @param api API类名
     * @param ext 扩展参数
     * @param jar JAR包地址
     * @return Spider实例
     */
    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            String jarMd5 = Util.md5(jar);
            parseJar(jarMd5, jar);
            
            if (spiders.containsKey(key)) {
                return spiders.get(key);
            }

            DexClassLoader loader = loaders.get(jarMd5);
            if (loader == null) {
                return new SpiderNull();
            }

            Class<?> clazz = loader.loadClass(api);
            Spider spider = (Spider) clazz.newInstance();
            spider.init(ext);
            spiders.put(key, spider);
            
            return spider;
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /**
     * 解析JAR包
     * @param jarMd5 JAR包MD5
     * @param jar JAR包地址
     */
    public void parseJar(String jarMd5, String jar) {
        try {
            if (loaders.containsKey(jarMd5)) {
                return;
            }

            File file = Path.jar(jarMd5);
            if (!file.exists()) {
                // 下载JAR包到本地
                downloadJar(jar, file);
            }

            if (!file.exists()) {
                return;
            }

            DexClassLoader loader = new DexClassLoader(
                file.getAbsolutePath(),
                Path.dex().getAbsolutePath(),
                null,
                ClassLoader.getSystemClassLoader()
            );

            loaders.put(jarMd5, loader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载JAR包
     * @param jar JAR包URL
     * @param file 目标文件
     */
    private void downloadJar(String jar, File file) {
        try {
            if (jar.startsWith("assets://")) {
                // 从assets复制JAR包
                copyJarFromAssets(jar, file);
            } else if (jar.startsWith("http")) {
                // 从网络下载JAR包
                downloadJarFromNetwork(jar, file);
            } else {
                // 从本地文件复制
                copyJarFromLocal(jar, file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从assets复制JAR包
     */
    private void copyJarFromAssets(String assetPath, File targetFile) {
        try {
            // 使用SpiderJarManager来处理assets复制
            SpiderJarManager jarManager = SpiderJarManager.getInstance();
            String internalPath = jarManager.copyJarToInternalStorage(assetPath);

            if (internalPath != null) {
                // 复制到目标位置
                java.nio.file.Files.copy(
                    new java.io.File(internalPath).toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从网络下载JAR包
     */
    private void downloadJarFromNetwork(String url, File targetFile) {
        try {
            // 使用OkHttp下载
            // 这里可以集成网络下载逻辑
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从本地文件复制JAR包
     */
    private void copyJarFromLocal(String localPath, File targetFile) {
        try {
            java.io.File sourceFile = new java.io.File(localPath);
            if (sourceFile.exists()) {
                java.nio.file.Files.copy(
                    sourceFile.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Dex类加载器
     * @param jar JAR包地址
     * @return DexClassLoader
     */
    public DexClassLoader dex(String jar) {
        String jarMd5 = Util.md5(jar);
        return loaders.get(jarMd5);
    }

    /**
     * 设置最近使用的JAR
     * @param jarMd5 JAR包MD5
     */
    public void setRecent(String jarMd5) {
        this.recent = jarMd5;
    }

    /**
     * 获取最近使用的JAR
     * @return JAR包MD5
     */
    public String getRecent() {
        return recent;
    }

    /**
     * 代理调用
     * @param params 参数Map
     * @return 调用结果
     */
    public Object[] proxyInvoke(Map<String, String> params) {
        try {
            if (TextUtils.isEmpty(recent)) {
                return new Object[]{500, "text/plain", ""};
            }

            DexClassLoader loader = loaders.get(recent);
            if (loader == null) {
                return new Object[]{500, "text/plain", ""};
            }

            // 查找代理类
            Class<?> proxyClass = findProxyClass(loader);
            if (proxyClass == null) {
                return new Object[]{500, "text/plain", ""};
            }

            // 调用代理方法
            Method method = proxyClass.getMethod("proxyLocal", Map.class);
            Object result = method.invoke(null, params);

            if (result instanceof Object[]) {
                return (Object[]) result;
            } else {
                return new Object[]{200, "text/plain", result.toString()};
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[]{500, "text/plain", e.getMessage()};
        }
    }

    /**
     * 查找代理类
     * @param loader 类加载器
     * @return 代理类
     */
    private Class<?> findProxyClass(DexClassLoader loader) {
        try {
            // 尝试常见的代理类名
            String[] proxyClassNames = {
                "com.github.catvod.spider.Proxy",
                "Proxy",
                "ProxyLocal"
            };

            for (String className : proxyClassNames) {
                try {
                    return loader.loadClass(className);
                } catch (ClassNotFoundException ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        Spider spider = spiders.get(key);
        if (spider == null) {
            return new JSONObject();
        }
        return spider.jsonExt(jxs, url);
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
        Spider spider = spiders.get(key);
        if (spider == null) {
            return new JSONObject();
        }
        return spider.jsonExtMix(flag, key, name, jxs, url);
    }

    /**
     * 获取已加载的Spider数量
     * @return Spider数量
     */
    public int getSpiderCount() {
        return spiders.size();
    }

    /**
     * 获取已加载的JAR数量
     * @return JAR数量
     */
    public int getJarCount() {
        return loaders.size();
    }
}
