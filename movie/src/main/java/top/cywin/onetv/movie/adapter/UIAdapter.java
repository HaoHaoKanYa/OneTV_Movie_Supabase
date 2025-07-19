package top.cywin.onetv.movie.adapter;

import android.util.Log;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.event.*;

/**
 * UI适配器 - 纯粹的EventBus事件适配器
 * 只负责FongMi_TV事件与Compose UI的事件转换
 */
public class UIAdapter {

    private static final String TAG = "UIAdapter";

    public UIAdapter() {
        Log.d(TAG, "🏗️ UIAdapter 初始化完成");
    }

    /**
     * 初始化EventBus监听 - 只做事件转换
     */
    public void initializeEventBus() {
        Log.d(TAG, "🔄 初始化EventBus监听");
        try {
            // ✅ 注册EventBus监听器
            EventBus.getDefault().register(this);
            Log.d(TAG, "✅ EventBus监听初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ EventBus监听初始化失败", e);
            throw new RuntimeException("EventBus监听初始化失败", e);
        }
    }

    /**
     * 清理EventBus监听
     */
    public void cleanup() {
        Log.d(TAG, "🧹 清理EventBus监听");
        try {
            EventBus.getDefault().unregister(this);
            Log.d(TAG, "✅ EventBus监听清理完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ EventBus监听清理失败", e);
        }
    }

    // ✅ 监听FongMi_TV的SiteViewModel事件，转换为Compose UI事件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteViewModelResult(Object result) {
        Log.d(TAG, "📡 收到SiteViewModel结果事件");

        // ✅ 转换为Compose UI事件
        if (result instanceof top.cywin.onetv.movie.bean.Result) {
            top.cywin.onetv.movie.bean.Result vodResult = (top.cywin.onetv.movie.bean.Result) result;

            // 判断结果类型并发送相应的Compose事件
            if (vodResult.getList() != null && !vodResult.getList().isEmpty()) {
                // 搜索结果或分类结果
                EventBus.getDefault().post(new SearchResultEvent(
                    vodResult.getList(),
                    "",
                    vodResult.getList().size() >= 20
                ));
            }
        }
    }

    // ✅ 监听FongMi_TV的配置更新事件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigUpdate(Object configEvent) {
        Log.d(TAG, "⚙️ 收到配置更新事件");

        // ✅ 转换为Compose UI事件
        EventBus.getDefault().post(new ConfigUpdateEvent(
            VodConfig.get(),
            true,
            null
        ));
    }

    // ✅ 监听FongMi_TV的播放解析事件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayUrlParsed(Object parseEvent) {
        Log.d(TAG, "🎬 收到播放解析事件");

        // ✅ 转换为Compose UI事件
        // 这里需要根据FongMi_TV的实际解析事件结构进行适配
        EventBus.getDefault().post(new PlayUrlParseEvent(
            null, // 解析后的播放地址
            null,  // 请求头
            null,
            0,
            null
        ));
    }

    // ✅ 监听FongMi_TV的错误事件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onError(Object errorEvent) {
        Log.d(TAG, "❌ 收到错误事件");

        // ✅ 转换为Compose UI事件
        EventBus.getDefault().post(new ErrorEvent(
            "FongMi_TV系统错误",
            null,
            null
        ));
    }

}
