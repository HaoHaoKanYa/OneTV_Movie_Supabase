package top.cywin.onetv.movie;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import top.cywin.onetv.movie.event.EventIndex;
// ❌ 移除CrashActivity引用
// import top.cywin.onetv.movie.ui.activity.CrashActivity;
import org.greenrobot.eventbus.EventBus;
import top.cywin.onetv.movie.event.ErrorEvent;
import top.cywin.onetv.movie.utils.Notify;
import top.cywin.onetv.movie.hook.Hook;
import top.cywin.onetv.movie.catvod.Init;
import top.cywin.onetv.movie.catvod.bean.Doh;
import top.cywin.onetv.movie.catvod.net.OkHttp;
import top.cywin.onetv.movie.config.VodConfigDeployer;
import top.cywin.onetv.movie.impl.Callback;
import com.google.gson.Gson;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class App extends Application {

    private final ExecutorService executor;
    private final Handler handler;
    private static App instance;
    private Activity activity;
    private final Gson gson;
    private final long time;
    private Hook hook;

    public App() {
        instance = this;
        executor = Executors.newFixedThreadPool(Constants.THREAD_POOL);
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
        time = System.currentTimeMillis();
        gson = new Gson();
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static long time() {
        return get().time;
    }

    public static Activity activity() {
        return get().activity;
    }

    public static void execute(Runnable runnable) {
        get().executor.execute(runnable);
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    public static void removeCallbacks(Runnable... runnable) {
        for (Runnable r : runnable) get().handler.removeCallbacks(r);
    }

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    private void setActivity(Activity activity) {
        this.activity = activity;
    }

    private LogAdapter getLogAdapter() {
        return new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().methodCount(0).showThreadInfo(false).tag("").build()) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return true;
            }
        };
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Init.set(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notify.createChannel();
        Logger.addLogAdapter(getLogAdapter());
        OkHttp.get().setProxy(Setting.getProxy());
        OkHttp.get().setDoh(Doh.objectFrom(Setting.getDoh()));
        EventBus.builder().addIndex(new EventIndex()).installDefaultEventBus();
        // ❌ 不再使用CrashActivity
        // CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();

        // ✅ 使用Compose错误处理
        setupExceptionHandler();

        // 自动部署OneTV官方影视接口
        initVodConfig();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }
        });
    }

    /**
     * 初始化VOD配置
     * 自动部署OneTV官方影视接口
     */
    private void initVodConfig() {
        execute(() -> {
            try {
                Logger.d("开始初始化VOD配置");

                // 部署OneTV官方影视接口
                VodConfigDeployer.deployOnetvApiConfig(this, new Callback() {
                    @Override
                    public void success() {
                        Logger.d("OneTV官方影视接口部署成功！");
                        Logger.d(VodConfigDeployer.getConfigStatus());
                    }

                    @Override
                    public void error(String msg) {
                        Logger.e("OneTV官方影视接口部署失败: " + msg);
                        // 部署失败时可以尝试其他配置或显示错误信息
                    }
                });

            } catch (Exception e) {
                Logger.e("初始化VOD配置时发生异常: " + e.getMessage());
            }
        });
    }

    /**
     * 设置异常处理器 - 使用Compose错误处理
     */
    private void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger.e("未捕获的异常", throwable);

            // ✅ 使用Compose错误处理
            EventBus.getDefault().post(new ErrorEvent(throwable.getMessage(), throwable, null));

            // 重启应用
            restartApplication();
        });
    }

    /**
     * 重启应用
     */
    private void restartApplication() {
        android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        System.exit(0);
    }

    @Override
    public PackageManager getPackageManager() {
        return hook != null ? hook : getBaseContext().getPackageManager();
    }

    @Override
    public String getPackageName() {
        return hook != null ? hook.getPackageName() : getBaseContext().getPackageName();
    }
}