package top.cywin.onetv.movie.model;

import android.text.TextUtils;

import androidx.collection.ArrayMap;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import top.cywin.onetv.movie.App;
import top.cywin.onetv.movie.Constants;
import top.cywin.onetv.movie.R;
import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.bean.Episode;
import top.cywin.onetv.movie.bean.Flag;
import top.cywin.onetv.movie.bean.Result;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.bean.Url;
import top.cywin.onetv.movie.bean.Vod;
import top.cywin.onetv.movie.exception.ExtractException;
import top.cywin.onetv.movie.player.Source;
import top.cywin.onetv.movie.utils.ResUtil;
import top.cywin.onetv.movie.utils.Sniffer;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.crawler.SpiderDebug;
import top.cywin.onetv.movie.catvod.net.OkHttp;
import top.cywin.onetv.movie.catvod.utils.Trans;
import top.cywin.onetv.movie.catvod.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Response;

// ✅ 添加EventBus支持
import org.greenrobot.eventbus.EventBus;
import top.cywin.onetv.movie.event.*;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Episode> episode;
    public MutableLiveData<Result> result;
    public MutableLiveData<Result> player;
    public MutableLiveData<Result> search;
    public MutableLiveData<Result> action;
    private ExecutorService executor;

    // ✅ 添加搜索状态记录
    private String lastSearchKeyword = "";
    private String lastTypeId = "";
    private int lastPage = 1;

    public SiteViewModel() {
        this.episode = new MutableLiveData<>();
        this.result = new MutableLiveData<>();
        this.player = new MutableLiveData<>();
        this.search = new MutableLiveData<>();
        this.action = new MutableLiveData<>();
    }

    public void setEpisode(Episode value) {
        episode.setValue(value);
    }

    public void homeContent() {
        execute(result, () -> {
            Site site = VodConfig.get().getHome();
            if (site.getType() == 3) {
                Spider spider = site.recent().spider();
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                Result result = Result.fromJson(homeContent);
                if (!result.getList().isEmpty()) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.log(homeVideoContent);
                result.setList(Result.fromJson(homeVideoContent).getList());
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("filter", "true");
                String homeContent = call(site.fetchExt(), params);
                SpiderDebug.log(homeContent);
                return Result.fromJson(homeContent);
            } else {
                Response response = OkHttp.newCall(site.getApi(), site.getHeaders()).execute();
                String homeContent = response.body().string();
                SpiderDebug.log(homeContent);
                response.close();
                return fetchPic(site, Result.fromType(site.getType(), homeContent));
            }
        });
    }

    public void categoryContent(String key, String tid, String page, boolean filter, HashMap<String, String> extend) {
        // ✅ 记录分类状态
        this.lastTypeId = tid;
        this.lastPage = Integer.parseInt(page);

        execute(result, () -> {
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = site.recent().spider();
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                return Result.fromJson(categoryContent);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                if (site.getType() == 1 && !extend.isEmpty()) params.put("f", App.gson().toJson(extend));
                if (site.getType() == 4) params.put("ext", Util.base64(App.gson().toJson(extend), Util.URL_SAFE));
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String categoryContent = call(site, params);
                SpiderDebug.log(categoryContent);
                return Result.fromType(site.getType(), categoryContent);
            }
        });
    }

    public void detailContent(String key, String id) {
        execute(result, () -> {
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = site.recent().spider();
                String detailContent = spider.detailContent(Arrays.asList(id));
                SpiderDebug.log(detailContent);
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) Source.get().parse(result.getList().get(0).getVodFlags());
                return result;
            } else if (site.isEmpty() && "push_agent".equals(key)) {
                Vod vod = new Vod();
                vod.setVodId(id);
                vod.setVodName(id);
                vod.setVodPic(ResUtil.getString(R.string.vod_push_image));
                vod.setVodFlags(Flag.create(ResUtil.getString(R.string.vod_push), id));
                Source.get().parse(vod.getVodFlags());
                return Result.vod(vod);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", id);
                String detailContent = call(site, params);
                SpiderDebug.log(detailContent);
                Result result = Result.fromType(site.getType(), detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) Source.get().parse(result.getList().get(0).getVodFlags());
                return result;
            }
        });
    }

    public void playerContent(String key, String flag, String id) {
        execute(player, () -> {
            Source.get().stop();
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = site.recent().spider();
                String playerContent = spider.playerContent(flag, id, VodConfig.get().getFlags());
                SpiderDebug.log(playerContent);
                Result result = Result.fromJson(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setUrl(Source.get().fetch(result));
                result.setHeader(site.getHeader());
                result.setKey(key);
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("play", id);
                params.put("flag", flag);
                String playerContent = call(site, params);
                SpiderDebug.log(playerContent);
                Result result = Result.fromJson(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setUrl(Source.get().fetch(result));
                result.setHeader(site.getHeader());
                return result;
            } else if (site.isEmpty() && "push_agent".equals(key)) {
                Result result = new Result();
                result.setParse(0);
                result.setFlag(flag);
                result.setUrl(Url.create().add(id));
                result.setUrl(Source.get().fetch(result));
                return result;
            } else {
                Url url = Url.create().add(id);
                Result result = new Result();
                result.setUrl(url);
                result.setFlag(flag);
                result.setHeader(site.getHeader());
                result.setPlayUrl(site.getPlayUrl());
                result.setParse(Sniffer.isVideoFormat(url.v()) && result.getPlayUrl().isEmpty() ? 0 : 1);
                result.setUrl(Source.get().fetch(result));
                SpiderDebug.log(result.toString());
                return result;
            }
        });
    }

    public void action(String key, String action) {
        execute(this.action, () -> {
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) return Result.fromJson(site.recent().spider().action(action));
            if (site.getType() == 4) return Result.fromJson(OkHttp.string(action));
            return Result.empty();
        });
    }

    public void searchContent(Site site, String keyword, boolean quick) throws Throwable {
        if (site.getType() == 3) {
            if (quick && !site.isQuickSearch()) return;
            String searchContent = site.spider().searchContent(Trans.t2s(keyword), quick);
            SpiderDebug.log(site.getName() + "," + searchContent);
            post(site, Result.fromJson(searchContent));
        } else {
            if (quick && !site.isQuickSearch()) return;
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("wd", Trans.t2s(keyword));
            params.put("quick", String.valueOf(quick));
            String searchContent = call(site, params);
            SpiderDebug.log(site.getName() + "," + searchContent);
            post(site, fetchPic(site, Result.fromType(site.getType(), searchContent)));
        }
    }

    public void searchContent(Site site, String keyword, String page) {
        // ✅ 记录搜索状态
        this.lastSearchKeyword = keyword;

        execute(search, () -> {
            if (site.getType() == 3) {
                String searchContent = site.spider().searchContent(Trans.t2s(keyword), false, page);
                SpiderDebug.log(site.getName() + "," + searchContent);
                Result result = Result.fromJson(searchContent);
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("wd", Trans.t2s(keyword));
                params.put("pg", page);
                String searchContent = call(site, params);
                SpiderDebug.log(site.getName() + "," + searchContent);
                Result result = fetchPic(site, Result.fromType(site.getType(), searchContent));
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            }
        });
    }

    private String call(Site site, ArrayMap<String, String> params) throws IOException {
        if (!site.getExt().isEmpty()) params.put("extend", site.getExt());
        Call get = OkHttp.newCall(site.getApi(), site.getHeaders(), params);
        Call post = OkHttp.newCall(site.getApi(), site.getHeaders(), OkHttp.toBody(params));
        Response response = (site.getExt().length() <= 1000 ? get : post).execute();
        String result = response.body().string();
        response.close();
        return result;
    }

    private Result fetchPic(Site site, Result result) throws Exception {
        if (site.getType() > 2 || result.getList().isEmpty() || !result.getList().get(0).getVodPic().isEmpty()) return result;
        ArrayList<String> ids = new ArrayList<>();
        if (site.getCategories().isEmpty()) for (Vod item : result.getList()) ids.add(item.getVodId());
        else for (Vod item : result.getList()) if (site.getCategories().contains(item.getTypeName())) ids.add(item.getVodId());
        if (ids.isEmpty()) return result.clear();
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("ac", site.getType() == 0 ? "videolist" : "detail");
        params.put("ids", TextUtils.join(",", ids));
        Response response = OkHttp.newCall(site.getApi(), site.getHeaders(), params).execute();
        result.setList(Result.fromType(site.getType(), response.body().string()).getList());
        response.close();
        return result;
    }

    private void post(Site site, Result result) {
        if (result.getList().isEmpty()) return;
        for (Vod vod : result.getList()) vod.setSite(site);
        this.search.postValue(result);
    }

    private void execute(MutableLiveData<Result> result, Callable<Result> callable) {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            try {
                if (Thread.interrupted()) return;
                Result resultData = executor.submit(callable).get(Constants.TIMEOUT_VOD, TimeUnit.MILLISECONDS);
                result.postValue(resultData);

                // ✅ 添加EventBus通知机制
                notifyComposeUI(result, resultData);

            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;

                Result errorResult;
                if (e.getCause() instanceof ExtractException) {
                    errorResult = Result.error(e.getCause().getMessage());
                    // ✅ 通知Compose UI错误
                    EventBus.getDefault().post(new ErrorEvent(e.getCause().getMessage(), e.getCause()));
                } else {
                    errorResult = Result.empty();
                    // ✅ 通知Compose UI错误
                    EventBus.getDefault().post(new ErrorEvent("请求失败", e));
                }
                result.postValue(errorResult);
                e.printStackTrace();
            }
        });
    }

    /**
     * 通知Compose UI - 根据结果类型发送相应事件
     */
    private void notifyComposeUI(MutableLiveData<Result> resultLiveData, Result resultData) {
        try {
            // ✅ 根据LiveData类型判断操作类型
            if (resultLiveData == search) {
                // 搜索结果
                EventBus.getDefault().post(new SearchResultEvent(
                    resultData.getList(),
                    lastSearchKeyword,
                    resultData.getList().size() >= 20
                ));
            } else if (resultLiveData == result) {
                // 判断是首页内容还是分类内容
                if (!TextUtils.isEmpty(lastTypeId)) {
                    // 分类内容
                    EventBus.getDefault().post(new CategoryContentEvent(
                        resultData.getList(),
                        lastTypeId,
                        lastPage,
                        resultData.getList().size() >= 20
                    ));
                } else {
                    // 首页内容
                    EventBus.getDefault().post(new HomeContentEvent(
                        resultData.getTypes(),
                        resultData.getList(),
                        true
                    ));
                }
            } else if (resultLiveData == action) {
                // 详情内容
                Vod vod = resultData.getList().isEmpty() ? null : resultData.getList().get(0);
                EventBus.getDefault().post(new ContentDetailEvent(vod, true, null));
            } else if (resultLiveData == player) {
                // 播放地址解析
                if (!resultData.getList().isEmpty()) {
                    Vod vod = resultData.getList().get(0);
                    if (!vod.getVodFlags().isEmpty()) {
                        // 获取第一个播放源的第一个剧集URL
                        String playUrl = vod.getVodUrls().split("\\$\\$\\$")[0].split("#")[0].split("\\$")[1];
                        EventBus.getDefault().post(new PlayUrlParseEvent(playUrl, null, vod.getVodId(), 0, null));
                    }
                }
            }
        } catch (Exception e) {
            // 通知失败不影响主流程
            e.printStackTrace();
        }
    }

    // ✅ 添加便捷方法供Compose UI调用

    /**
     * 搜索内容 - Compose UI调用入口
     */
    public void searchContent(String keyword, boolean quick) {
        this.lastSearchKeyword = keyword;
        Site site = VodConfig.get().getHome();
        if (site != null) {
            searchContent(site, keyword, "1");
        }
    }

    /**
     * 分类内容 - Compose UI调用入口
     */
    public void categoryContent(String typeId, int page, boolean more, java.util.Map<String, String> filters) {
        this.lastTypeId = typeId;
        this.lastPage = page;

        Site site = VodConfig.get().getHome();
        if (site != null) {
            HashMap<String, String> extend = new HashMap<>();
            if (filters != null) extend.putAll(filters);
            categoryContent(site.getKey(), typeId, String.valueOf(page), true, extend);
        }
    }

    /**
     * 获取内容详情 - Compose UI调用入口
     */
    public void detailContent(String vodId) {
        Site site = VodConfig.get().getHome();
        if (site != null) {
            detailContent(site.getKey(), vodId);
        }
    }

    /**
     * 解析播放地址 - Compose UI调用入口
     */
    public void playerContent(String url, String flag) {
        Site site = VodConfig.get().getHome();
        if (site != null) {
            playerContent(site.getKey(), flag, url);
        }
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
