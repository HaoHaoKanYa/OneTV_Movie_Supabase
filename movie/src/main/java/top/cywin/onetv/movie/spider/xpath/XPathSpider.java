package top.cywin.onetv.movie.spider.xpath;

import android.content.Context;
import android.text.TextUtils;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.utils.Json;
import top.cywin.onetv.movie.bean.Result;
import top.cywin.onetv.movie.bean.Vod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * XPath基础解析器 - csp_XPath
 * 基于FongMi_TV架构实现
 */
public class XPathSpider extends Spider {

    protected String siteUrl;
    protected String siteName;
    protected HashMap<String, String> headers;

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        if (!TextUtils.isEmpty(extend)) {
            parseExtend(extend);
        }
        initHeaders();
    }

    protected void parseExtend(String extend) {
        try {
            // 解析扩展配置
            // 这里可以根据具体需求解析JSON配置
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initHeaders() {
        headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Accept-Encoding", "gzip, deflate");
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        try {
            Document doc = fetchDocument(siteUrl);
            List<Vod> vodList = parseHomeVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        try {
            String categoryUrl = buildCategoryUrl(tid, pg, extend);
            Document doc = fetchDocument(categoryUrl);
            List<Vod> vodList = parseCategoryVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            result.setPage(Integer.parseInt(pg));
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        try {
            String vodId = ids.get(0);
            String detailUrl = buildDetailUrl(vodId);
            Document doc = fetchDocument(detailUrl);
            Vod vod = parseDetailVod(doc, vodId);
            
            Result result = new Result();
            List<Vod> vodList = new ArrayList<>();
            vodList.add(vod);
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        try {
            String searchUrl = buildSearchUrl(key);
            Document doc = fetchDocument(searchUrl);
            List<Vod> vodList = parseSearchVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        try {
            String playUrl = parsePlayUrl(flag, id);
            
            HashMap<String, Object> result = new HashMap<>();
            result.put("parse", 0);
            result.put("playUrl", "");
            result.put("url", playUrl);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 抽象方法，由具体实现类重写
    protected Document fetchDocument(String url) throws Exception {
        // 使用OkHttp获取网页内容并解析为Document
        return Jsoup.connect(url).headers(headers).get();
    }

    protected List<Vod> parseHomeVods(Document doc) {
        // 解析首页视频列表，由子类实现
        return new ArrayList<>();
    }

    protected String buildCategoryUrl(String tid, String pg, HashMap<String, String> extend) {
        // 构建分类URL，由子类实现
        return siteUrl;
    }

    protected List<Vod> parseCategoryVods(Document doc) {
        // 解析分类视频列表，由子类实现
        return new ArrayList<>();
    }

    protected String buildDetailUrl(String vodId) {
        // 构建详情URL，由子类实现
        return siteUrl + vodId;
    }

    protected Vod parseDetailVod(Document doc, String vodId) {
        // 解析视频详情，由子类实现
        Vod vod = new Vod();
        vod.setVodId(vodId);
        return vod;
    }

    protected String buildSearchUrl(String key) {
        // 构建搜索URL，由子类实现
        return siteUrl + "/search?q=" + key;
    }

    protected List<Vod> parseSearchVods(Document doc) {
        // 解析搜索结果，由子类实现
        return new ArrayList<>();
    }

    protected String parsePlayUrl(String flag, String id) {
        // 解析播放地址，由子类实现
        return id;
    }

    // 工具方法
    protected String getTextBySelector(Element element, String selector) {
        try {
            Element target = element.selectFirst(selector);
            return target != null ? target.text().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    protected String getAttrBySelector(Element element, String selector, String attr) {
        try {
            Element target = element.selectFirst(selector);
            return target != null ? target.attr(attr).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    protected Elements getElementsBySelector(Element element, String selector) {
        try {
            return element.select(selector);
        } catch (Exception e) {
            return new Elements();
        }
    }
}
