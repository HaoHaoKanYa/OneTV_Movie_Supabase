package top.cywin.onetv.movie;

import java.util.concurrent.TimeUnit;

public class Constants {

    public static final long INTERVAL_SEEK = TimeUnit.SECONDS.toMillis(10);
    public static final long INTERVAL_HIDE = TimeUnit.SECONDS.toMillis(5);
    public static final long INTERVAL_TRAFFIC = TimeUnit.SECONDS.toMillis(1);
    public static final long TIMEOUT_VOD = TimeUnit.SECONDS.toMillis(30);
    public static final long TIMEOUT_LIVE = TimeUnit.SECONDS.toMillis(30);
    public static final long TIMEOUT_EPG = TimeUnit.SECONDS.toMillis(5);
    public static final long TIMEOUT_XML = TimeUnit.SECONDS.toMillis(15);
    public static final long TIMEOUT_PLAY = TimeUnit.SECONDS.toMillis(15);
    public static final long TIMEOUT_SYNC = TimeUnit.SECONDS.toMillis(2);
    public static final long TIMEOUT_DANMAKU = TimeUnit.SECONDS.toMillis(30);
    public static final long TIMEOUT_PARSE_DEF = TimeUnit.SECONDS.toMillis(15);
    public static final long TIMEOUT_PARSE_WEB = TimeUnit.SECONDS.toMillis(15);
    public static final long TIMEOUT_PARSE_LIVE = TimeUnit.SECONDS.toMillis(10);
    public static final long HISTORY_TIME = TimeUnit.DAYS.toMillis(60);
    public static final long OPED_LIMIT = TimeUnit.MINUTES.toMillis(5);
    public static final int THREAD_POOL = 10;

    // 配置相关常量
    public static final String CONFIG_APP_ID = "onetv";
    public static final String ERROR_CONFIG_NOT_INITIALIZED = "配置未初始化，请先调用initializeConfig()";
}
