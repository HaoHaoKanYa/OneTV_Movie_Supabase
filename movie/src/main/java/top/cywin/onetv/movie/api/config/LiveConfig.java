package top.cywin.onetv.movie.api.config;

import android.net.Uri;
import android.text.TextUtils;

import top.cywin.onetv.movie.App;
import top.cywin.onetv.movie.R;
import top.cywin.onetv.movie.Setting;
import top.cywin.onetv.movie.api.Decoder;
import top.cywin.onetv.movie.api.LiveParser;
import top.cywin.onetv.movie.api.loader.BaseLoader;
import top.cywin.onetv.movie.bean.Channel;
import top.cywin.onetv.movie.bean.Config;
import top.cywin.onetv.movie.bean.Depot;
import top.cywin.onetv.movie.bean.Group;
import top.cywin.onetv.movie.bean.Keep;
import top.cywin.onetv.movie.bean.Live;
import top.cywin.onetv.movie.bean.Rule;
import top.cywin.onetv.movie.database.AppDatabase;
import top.cywin.onetv.movie.impl.Callback;
// ❌ 移除Activity引用
// import top.cywin.onetv.movie.ui.activity.LiveActivity;
import org.greenrobot.eventbus.EventBus;
import top.cywin.onetv.movie.event.NavigationEvent;
import top.cywin.onetv.movie.utils.Notify;
import top.cywin.onetv.movie.utils.UrlUtil;
import top.cywin.onetv.movie.catvod.net.OkHttp;
import top.cywin.onetv.movie.catvod.utils.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LiveConfig {

    private List<Live> lives;
    private List<Rule> rules;
    private List<String> ads;
    private Config config;
    private boolean sync;
    private Live home;

    private static class Loader {
        static volatile LiveConfig INSTANCE = new LiveConfig();
    }

    public static LiveConfig get() {
        return Loader.INSTANCE;
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static String getResp() {
        return get().getHome().getCore().getResp();
    }

    public static int getHomeIndex() {
        return get().getLives().indexOf(get().getHome());
    }

    public static boolean isOnly() {
        return get().getLives().size() == 1;
    }

    public static boolean isEmpty() {
        return get().getHome().isEmpty();
    }

    public static boolean hasUrl() {
        return getUrl() != null && !getUrl().isEmpty();
    }

    public static void load(Config config, Callback callback) {
        get().clear().config(config).load(callback);
    }

    public LiveConfig init() {
        this.home = null;
        this.ads = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.lives = new ArrayList<>();
        return config(Config.live());
    }

    public LiveConfig config(Config config) {
        this.config = config;
        if (config.getUrl() == null) return this;
        this.sync = config.getUrl().equals(VodConfig.getUrl());
        return this;
    }

    public LiveConfig clear() {
        this.home = null;
        this.ads.clear();
        this.rules.clear();
        this.lives.clear();
        return this;
    }

    public void load() {
        if (isEmpty()) load(new Callback());
    }

    public void load(Callback callback) {
        App.execute(() -> loadConfig(callback));
    }

    private void loadConfig(Callback callback) {
        try {
            OkHttp.cancel("live");
            parseConfig(Decoder.getJson(UrlUtil.convert(config.getUrl()), "live"), callback);
        } catch (Throwable e) {
            if (TextUtils.isEmpty(config.getUrl())) App.post(() -> callback.error(""));
            else App.post(() -> callback.error(Notify.getError(R.string.error_config_get, e)));
            e.printStackTrace();
        }
    }

    private void parseConfig(String text, Callback callback) {
        if (!Json.isObj(text)) {
            parseText(text, callback);
        } else {
            checkJson(Json.parse(text).getAsJsonObject(), callback);
        }
    }

    private void parseText(String text, Callback callback) {
        Live live = new Live(parseName(config.getUrl()), config.getUrl()).sync();
        LiveParser.text(live, text);
        lives.add(live);
        setHome(live, true);
        App.post(callback::success);
    }

    private String parseName(String url) {
        Uri uri = Uri.parse(url);
        if ("file".equals(uri.getScheme())) return new File(url).getName();
        if (uri.getLastPathSegment() != null) return uri.getLastPathSegment();
        if (uri.getQuery() != null) return uri.getQuery();
        if (uri.getHost() != null) return uri.getHost();
        return url;
    }

    private void checkJson(JsonObject object, Callback callback) {
        if (object.has("msg")) {
            App.post(() -> callback.error(object.get("msg").getAsString()));
        } else if (object.has("urls")) {
            parseDepot(object, callback);
        } else {
            parseConfig(object, callback);
        }
    }

    private void parseDepot(JsonObject object, Callback callback) {
        List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls").toString());
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) configs.add(Config.find(item, 1));
        Config.delete(config.getUrl());
        config = configs.get(0);
        loadConfig(callback);
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            initLive(object);
            initOther(object);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (callback != null) App.post(callback::success);
        }
    }

    private void initLive(JsonObject object) {
        String spider = Json.safeString(object, "spider");
        BaseLoader.get().parseJar(spider, false);
        for (JsonElement element : Json.safeListElement(object, "lives")) {
            Live live = Live.objectFrom(element);
            if (lives.contains(live)) continue;
            live.setApi(UrlUtil.convert(live.getApi()));
            live.setExt(UrlUtil.convert(live.getExt()));
            live.setJar(parseJar(live, spider));
            lives.add(live.sync());
        }
        for (Live live : lives) {
            if (live.getName().equals(config.getHome())) {
                setHome(live, true);
            }
        }
    }

    private void initOther(JsonObject object) {
        if (home == null) setHome(lives.isEmpty() ? new Live() : lives.get(0), true);
        setRules(Rule.arrayFrom(object.getAsJsonArray("rules")));
        setHeaders(Json.safeListElement(object, "headers"));
        setHosts(Json.safeListString(object, "hosts"));
        setProxy(Json.safeListString(object, "proxy"));
        setAds(Json.safeListString(object, "ads"));
    }

    private String parseJar(Live live, String spider) {
        return live.getJar().isEmpty() ? spider : live.getJar();
    }

    private void bootLive() {
        Setting.putBootLive(false);
        // ❌ LiveActivity.start(App.get());

        // ✅ 通过EventBus通知Compose UI
        EventBus.getDefault().post(new NavigationEvent("live_boot"));
    }

    public void parse(JsonObject object) {
        parseConfig(object, null);
    }

    public void setKeep(Channel channel) {
        if (home != null && !channel.getGroup().isHidden()) home.keep(channel).save();
    }

    public void setKeep(List<Group> items) {
        List<String> key = new ArrayList<>();
        for (Keep keep : Keep.getLive()) key.add(keep.getKey());
        for (Group group : items) {
            if (group.isKeep()) continue;
            for (Channel channel : group.getChannel()) {
                if (key.contains(channel.getName())) {
                    items.get(0).add(channel);
                }
            }
        }
    }

    public int[] find(List<Group> items) {
        String[] splits = getHome().getKeep().split(AppDatabase.SYMBOL);
        if (splits.length < 2) return new int[]{1, 0};
        for (int i = 0; i < items.size(); i++) {
            Group group = items.get(i);
            if (group.getName().equals(splits[0])) {
                int j = group.find(splits[1]);
                if (j != -1 && splits.length > 2) group.getChannel().get(j).setLine(splits[2]);
                if (j != -1) return new int[]{i, j};
            }
        }
        return new int[]{1, 0};
    }

    public int[] find(String number, List<Group> items) {
        for (int i = 0; i < items.size(); i++) {
            int j = items.get(i).find(Integer.parseInt(number));
            if (j != -1) return new int[]{i, j};
        }
        return new int[]{-1, -1};
    }

    public boolean needSync(String url) {
        return sync || TextUtils.isEmpty(config.getUrl()) || url.equals(config.getUrl());
    }

    public List<Rule> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public void setHeaders(List<JsonElement> items) {
        OkHttp.responseInterceptor().setHeaders(items);
    }

    public void setHosts(List<String> hosts) {
        OkHttp.dns().addAll(hosts);
    }

    public void setProxy(List<String> hosts) {
        OkHttp.selector().addAll(hosts);
    }

    public List<String> getAds() {
        return ads == null ? Collections.emptyList() : ads;
    }

    private void setAds(List<String> ads) {
        this.ads = ads;
    }

    public List<Live> getLives() {
        return lives == null ? lives = new ArrayList<>() : lives;
    }

    public Config getConfig() {
        return config == null ? Config.live() : config;
    }

    public Live getHome() {
        return home == null ? new Live() : home;
    }

    public Live getLive(String key) {
        int index = getLives().indexOf(Live.get(key));
        return index == -1 ? new Live() : getLives().get(index);
    }

    public void setHome(Live home) {
        setHome(home, false);
    }

    private void setHome(Live home, boolean check) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getName()).update();
        for (Live item : getLives()) item.setActivated(home);

        // ❌ 移除Activity检查
        // if (App.activity() != null && App.activity() instanceof LiveActivity) return;

        if (check) if (home.isBoot() || Setting.isBootLive()) App.post(this::bootLive);
    }
}
