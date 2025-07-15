package top.cywin.onetv.vod.player.exo;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import top.cywin.onetv.vod.App;
import top.github.catvod.utils.Path;

public class CacheManager {

    private SimpleCache cache;

    private static class Loader {
        static volatile CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager get() {
        return Loader.INSTANCE;
    }

    public Cache getCache() {
        if (cache == null) create();
        return cache;
    }

    private void create() {
        cache = new SimpleCache(Path.exo(), new NoOpCacheEvictor(), new StandaloneDatabaseProvider(App.get()));
    }
}

