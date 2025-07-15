package top.cywin.onetv.tv.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import top.cywin.onetv.tv.App;
import top.cywin.onetv.tv.bean.Backup;
import top.cywin.onetv.tv.bean.Config;
import top.cywin.onetv.tv.bean.Device;
import top.cywin.onetv.tv.bean.History;
import top.cywin.onetv.tv.bean.Keep;
import top.cywin.onetv.tv.bean.Live;
import top.cywin.onetv.tv.bean.Site;
import top.cywin.onetv.tv.bean.Track;
import top.cywin.onetv.tv.db.dao.ConfigDao;
import top.cywin.onetv.tv.db.dao.DeviceDao;
import top.cywin.onetv.tv.db.dao.HistoryDao;
import top.cywin.onetv.tv.db.dao.KeepDao;
import top.cywin.onetv.tv.db.dao.LiveDao;
import top.cywin.onetv.tv.db.dao.SiteDao;
import top.cywin.onetv.tv.db.dao.TrackDao;
import top.cywin.onetv.tv.utils.FileUtil;
import top.github.catvod.utils.Path;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Database(entities = {Keep.class, Site.class, Live.class, Track.class, Config.class, Device.class, History.class}, version = AppDatabase.VERSION)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 33;
    public static final String NAME = "tv";
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    public static void backup() {
        backup(new top.cywin.onetv.tv.impl.Callback());
    }

    public static void backup(top.cywin.onetv.tv.impl.Callback callback) {
        App.execute(() -> {
            File file = new File(Path.tv(), "tv-" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + ".bk");
            Backup backup = Backup.create();
            if (backup.getConfig().isEmpty()) {
                App.post(callback::error);
            } else {
                Path.write(file, backup.toString().getBytes());
                FileUtil.gzipCompress(file);
                App.post(callback::success);
                cleanOld();
            }
        });
    }

    public static void restore(File file, top.cywin.onetv.tv.impl.Callback callback) {
        App.execute(() -> {
            File restore = Path.cache("restore");
            FileUtil.gzipDecompress(file, restore);
            Backup backup = Backup.objectFrom(Path.read(restore));
            if (backup.getConfig().isEmpty()) {
                App.post(callback::error);
            } else {
                backup.restore();
                Path.clear(restore);
                App.post(callback::success);
            }
        });
    }

    private static void cleanOld() {
        List<File> items = new ArrayList<>();
        File[] files = Path.tv().listFiles();
        if (files == null) files = new File[0];
        for (File file : files) if (file.getName().startsWith("tv") && file.getName().endsWith(".bk.gz")) items.add(file);
        if (!items.isEmpty()) Collections.sort(items, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        if (items.size() > 7) for (int i = 7; i < items.size(); i++) Path.clear(items.get(i));
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(Migrations.MIGRATION_30_31)
                .addMigrations(Migrations.MIGRATION_31_32)
                .addMigrations(Migrations.MIGRATION_32_33)
                .allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract LiveDao getLiveDao();

    public abstract TrackDao getTrackDao();

    public abstract ConfigDao getConfigDao();

    public abstract DeviceDao getDeviceDao();

    public abstract HistoryDao getHistoryDao();
}
