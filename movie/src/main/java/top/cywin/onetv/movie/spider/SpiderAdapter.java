package top.cywin.onetv.movie.spider;

import android.text.TextUtils;

import top.cywin.onetv.movie.bean.Result;
import top.cywin.onetv.movie.bean.Vod;
import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.utils.Json;

import java.util.HashMap;
import java.util.List;

/**
 * Spider解析器适配器
 * 提供统一的Spider调用接口
 */
public class SpiderAdapter {

    private final Spider spider;

    public SpiderAdapter(Spider spider) {
        this.spider = spider;
    }

    /**
     * 初始化Spider
     */
    public void init(String extend) {
        try {
            if (spider != null) {
                spider.init(null, extend);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取首页数据
     */
    public String homeContent(boolean filter) {
        try {
            if (spider != null) {
                return spider.homeContent(filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取首页推荐数据
     */
    public String homeVideoContent() {
        try {
            if (spider != null) {
                return spider.homeVideoContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取分类数据
     */
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            if (spider != null) {
                return spider.categoryContent(tid, pg, filter, extend);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取详情数据
     */
    public String detailContent(List<String> ids) {
        try {
            if (spider != null) {
                return spider.detailContent(ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取播放地址
     */
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            if (spider != null) {
                return spider.playerContent(flag, id, vipFlags);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 搜索内容
     */
    public String searchContent(String key, boolean quick) {
        try {
            if (spider != null) {
                return spider.searchContent(key, quick);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 解析Result对象
     */
    public Result parseResult(String json) {
        try {
            if (!TextUtils.isEmpty(json)) {
                return Json.parse(json, Result.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result();
    }

    /**
     * 解析Vod列表
     */
    public List<Vod> parseVodList(String json) {
        try {
            Result result = parseResult(json);
            return result.getList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检查Spider是否有效
     */
    public boolean isValid() {
        return spider != null && !(spider instanceof top.cywin.onetv.movie.catvod.crawler.SpiderNull);
    }

    /**
     * 获取Spider实例
     */
    public Spider getSpider() {
        return spider;
    }
}
