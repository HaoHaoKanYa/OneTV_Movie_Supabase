package top.cywin.onetv.movie.spider.engine;

import android.content.Context;
import android.text.TextUtils;

import top.cywin.onetv.movie.quickjs.crawler.Spider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaScript引擎池
 * 基于FongMi_TV的QuickJS架构实现引擎复用
 */
public class JsEnginePool {

    private static JsEnginePool instance;
    private final BlockingQueue<JsEngine> availableEngines;
    private final AtomicInteger totalEngines;
    private final AtomicInteger activeEngines;
    private final int maxPoolSize;
    private final int corePoolSize;
    private Context context;

    private JsEnginePool() {
        this.maxPoolSize = 5; // 最大引擎数量
        this.corePoolSize = 2; // 核心引擎数量
        this.availableEngines = new LinkedBlockingQueue<>();
        this.totalEngines = new AtomicInteger(0);
        this.activeEngines = new AtomicInteger(0);
    }

    public static JsEnginePool getInstance() {
        if (instance == null) {
            synchronized (JsEnginePool.class) {
                if (instance == null) {
                    instance = new JsEnginePool();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        // 预创建核心引擎
        for (int i = 0; i < corePoolSize; i++) {
            createEngine();
        }
    }

    /**
     * 获取JavaScript引擎
     */
    public JsEngine acquireEngine() {
        try {
            // 尝试从池中获取可用引擎
            JsEngine engine = availableEngines.poll(100, TimeUnit.MILLISECONDS);
            
            if (engine == null) {
                // 如果没有可用引擎且未达到最大数量，创建新引擎
                if (totalEngines.get() < maxPoolSize) {
                    engine = createEngine();
                } else {
                    // 等待可用引擎
                    engine = availableEngines.take();
                }
            }
            
            if (engine != null) {
                activeEngines.incrementAndGet();
            }
            
            return engine;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 释放JavaScript引擎
     */
    public void releaseEngine(JsEngine engine) {
        if (engine == null) return;
        
        try {
            // 重置引擎状态
            engine.reset();
            
            // 将引擎放回池中
            availableEngines.offer(engine);
            activeEngines.decrementAndGet();
        } catch (Exception e) {
            // 如果重置失败，销毁引擎
            destroyEngine(engine);
        }
    }

    /**
     * 创建新的JavaScript引擎
     */
    private JsEngine createEngine() {
        try {
            JsEngine engine = new JsEngine(context);
            totalEngines.incrementAndGet();
            return engine;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 销毁JavaScript引擎
     */
    private void destroyEngine(JsEngine engine) {
        if (engine == null) return;
        
        try {
            engine.destroy();
            totalEngines.decrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清理引擎池
     */
    public void clear() {
        // 销毁所有可用引擎
        JsEngine engine;
        while ((engine = availableEngines.poll()) != null) {
            destroyEngine(engine);
        }
        
        totalEngines.set(0);
        activeEngines.set(0);
    }

    /**
     * 获取引擎池状态
     */
    public PoolStatus getStatus() {
        PoolStatus status = new PoolStatus();
        status.totalEngines = totalEngines.get();
        status.activeEngines = activeEngines.get();
        status.availableEngines = availableEngines.size();
        status.maxPoolSize = maxPoolSize;
        status.corePoolSize = corePoolSize;
        return status;
    }

    /**
     * JavaScript引擎包装类
     */
    public static class JsEngine {
        private final Context context;
        private Spider spider;
        private boolean initialized;

        public JsEngine(Context context) {
            this.context = context;
            this.initialized = false;
        }

        /**
         * 初始化引擎
         */
        public void init(String key, String api, String jar) throws Exception {
            if (!initialized) {
                this.spider = new Spider(key, api, jar);
                this.spider.init(context, "");
                this.initialized = true;
            }
        }

        /**
         * 执行JavaScript代码
         */
        public Object execute(String method, Object... args) throws Exception {
            if (!initialized || spider == null) {
                throw new IllegalStateException("Engine not initialized");
            }
            
            // 根据方法名调用相应的Spider方法
            switch (method) {
                case "homeContent":
                    return spider.homeContent((Boolean) args[0]);
                case "categoryContent":
                    return spider.categoryContent((String) args[0], (String) args[1], 
                        (Boolean) args[2], (java.util.HashMap<String, String>) args[3]);
                case "detailContent":
                    return spider.detailContent((java.util.List<String>) args[0]);
                case "searchContent":
                    return spider.searchContent((String) args[0], (Boolean) args[1]);
                case "playerContent":
                    return spider.playerContent((String) args[0], (String) args[1], 
                        (java.util.List<String>) args[2]);
                default:
                    throw new UnsupportedOperationException("Unsupported method: " + method);
            }
        }

        /**
         * 重置引擎状态
         */
        public void reset() {
            // 清理临时状态，但保持初始化状态
        }

        /**
         * 销毁引擎
         */
        public void destroy() {
            if (spider != null) {
                try {
                    spider.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                spider = null;
            }
            initialized = false;
        }

        /**
         * 检查引擎是否可用
         */
        public boolean isAvailable() {
            return initialized && spider != null;
        }
    }

    /**
     * 引擎池状态类
     */
    public static class PoolStatus {
        public int totalEngines;
        public int activeEngines;
        public int availableEngines;
        public int maxPoolSize;
        public int corePoolSize;

        @Override
        public String toString() {
            return String.format("PoolStatus{total=%d, active=%d, available=%d, max=%d, core=%d}",
                totalEngines, activeEngines, availableEngines, maxPoolSize, corePoolSize);
        }
    }
}
