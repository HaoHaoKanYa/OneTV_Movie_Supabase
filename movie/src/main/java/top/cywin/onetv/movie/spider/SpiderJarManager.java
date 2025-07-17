package top.cywin.onetv.movie.spider;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Spider JAR文件管理器
 * 基于FongMi_TV架构实现JAR文件的管理和加载
 */
public class SpiderJarManager {

    private static SpiderJarManager instance;
    private final Map<String, String> jarPaths;
    private Context context;

    private SpiderJarManager() {
        this.jarPaths = new HashMap<>();
        initDefaultJarPaths();
    }

    public static SpiderJarManager getInstance() {
        if (instance == null) {
            synchronized (SpiderJarManager.class) {
                if (instance == null) {
                    instance = new SpiderJarManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
    }

    /**
     * 初始化默认JAR路径
     */
    private void initDefaultJarPaths() {
        // 默认Spider JAR文件路径
        jarPaths.put("default", "assets://jar/spider.jar");
        
        // 可以添加更多特定的JAR文件路径
        jarPaths.put("xpath", "assets://jar/spider.jar");
        jarPaths.put("app", "assets://jar/spider.jar");
        jarPaths.put("ali", "assets://jar/spider.jar");
        jarPaths.put("video", "assets://jar/spider.jar");
        jarPaths.put("netdisk", "assets://jar/spider.jar");
    }

    /**
     * 获取JAR文件路径
     */
    public String getJarPath(String type) {
        if (TextUtils.isEmpty(type)) {
            return jarPaths.get("default");
        }
        
        String path = jarPaths.get(type.toLowerCase());
        return path != null ? path : jarPaths.get("default");
    }

    /**
     * 获取默认Spider JAR路径
     */
    public String getDefaultSpiderJar() {
        return jarPaths.get("default");
    }

    /**
     * 检查JAR文件是否存在
     */
    public boolean isJarExists(String jarPath) {
        if (TextUtils.isEmpty(jarPath)) {
            return false;
        }

        try {
            if (jarPath.startsWith("assets://")) {
                // 检查assets中的文件
                String assetPath = jarPath.substring(9); // 移除 "assets://" 前缀
                InputStream is = context.getAssets().open(assetPath);
                is.close();
                return true;
            } else if (jarPath.startsWith("file://")) {
                // 检查本地文件
                String filePath = jarPath.substring(7); // 移除 "file://" 前缀
                File file = new File(filePath);
                return file.exists() && file.isFile();
            } else {
                // 检查网络URL或其他路径
                return !TextUtils.isEmpty(jarPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复制assets中的JAR文件到内部存储
     */
    public String copyJarToInternalStorage(String assetPath) {
        if (context == null || TextUtils.isEmpty(assetPath)) {
            return null;
        }

        try {
            // 移除 "assets://" 前缀
            if (assetPath.startsWith("assets://")) {
                assetPath = assetPath.substring(9);
            }

            // 创建内部存储目录
            File jarDir = new File(context.getFilesDir(), "jar");
            if (!jarDir.exists()) {
                jarDir.mkdirs();
            }

            // 目标文件路径
            File targetFile = new File(jarDir, "spider.jar");
            
            // 如果文件已存在且不为空，直接返回路径
            if (targetFile.exists() && targetFile.length() > 0) {
                return targetFile.getAbsolutePath();
            }

            // 复制文件
            InputStream is = context.getAssets().open(assetPath);
            FileOutputStream fos = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            is.close();

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取可用的JAR文件路径
     * 优先使用内部存储的文件，如果不存在则从assets复制
     */
    public String getAvailableJarPath(String jarPath) {
        if (TextUtils.isEmpty(jarPath)) {
            jarPath = getDefaultSpiderJar();
        }

        // 如果是assets路径，尝试复制到内部存储
        if (jarPath.startsWith("assets://")) {
            String internalPath = copyJarToInternalStorage(jarPath);
            if (!TextUtils.isEmpty(internalPath)) {
                return internalPath;
            }
        }

        return jarPath;
    }

    /**
     * 添加自定义JAR路径
     */
    public void addJarPath(String type, String path) {
        if (!TextUtils.isEmpty(type) && !TextUtils.isEmpty(path)) {
            jarPaths.put(type.toLowerCase(), path);
        }
    }

    /**
     * 移除JAR路径
     */
    public void removeJarPath(String type) {
        if (!TextUtils.isEmpty(type)) {
            jarPaths.remove(type.toLowerCase());
        }
    }

    /**
     * 清理临时JAR文件
     */
    public void cleanupTempJars() {
        if (context == null) return;

        try {
            File jarDir = new File(context.getFilesDir(), "jar");
            if (jarDir.exists() && jarDir.isDirectory()) {
                File[] files = jarDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".jar")) {
                            // 可以根据需要决定是否删除临时文件
                            // file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取JAR文件大小
     */
    public long getJarSize(String jarPath) {
        try {
            if (jarPath.startsWith("assets://")) {
                String assetPath = jarPath.substring(9);
                InputStream is = context.getAssets().open(assetPath);
                long size = is.available();
                is.close();
                return size;
            } else if (jarPath.startsWith("file://")) {
                String filePath = jarPath.substring(7);
                File file = new File(filePath);
                return file.length();
            } else {
                File file = new File(jarPath);
                return file.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
