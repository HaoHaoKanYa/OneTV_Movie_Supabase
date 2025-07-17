package top.cywin.onetv.movie.spider.engine;

import android.content.Context;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import top.cywin.onetv.movie.catvod.crawler.Spider;
import top.cywin.onetv.movie.catvod.utils.Json;
import top.cywin.onetv.movie.bean.Result;
import top.cywin.onetv.movie.bean.Vod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XPath解析引擎
 * 基于FongMi_TV架构实现XPath解析功能
 */
public class XPathEngine extends Spider {

    private Context context;
    private Map<String, String> config;
    private Map<String, String> headers;

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        this.context = context;
        this.config = new HashMap<>();
        this.headers = new HashMap<>();
        
        parseConfig(extend);
        initHeaders();
    }

    /**
     * 解析配置
     */
    private void parseConfig(String extend) {
        if (TextUtils.isEmpty(extend)) return;
        
        try {
            // 解析JSON配置
            Map<String, Object> configMap = Json.parse(extend, Map.class);
            if (configMap != null) {
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    config.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化请求头
     */
    private void initHeaders() {
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Accept-Encoding", "gzip, deflate");
        
        // 从配置中读取自定义请求头
        String customUA = config.get("User-Agent");
        if (!TextUtils.isEmpty(customUA)) {
            headers.put("User-Agent", customUA);
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        try {
            String homeUrl = config.get("homeUrl");
            if (TextUtils.isEmpty(homeUrl)) {
                return createEmptyResult();
            }

            Document doc = fetchDocument(homeUrl);
            List<Vod> vodList = parseHomeVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyResult();
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        try {
            String categoryUrl = buildCategoryUrl(tid, pg, extend);
            if (TextUtils.isEmpty(categoryUrl)) {
                return createEmptyResult();
            }

            Document doc = fetchDocument(categoryUrl);
            List<Vod> vodList = parseCategoryVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            result.setPage(Integer.parseInt(pg));
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyResult();
        }
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        try {
            if (ids == null || ids.isEmpty()) {
                return createEmptyResult();
            }

            String vodId = ids.get(0);
            String detailUrl = buildDetailUrl(vodId);
            if (TextUtils.isEmpty(detailUrl)) {
                return createEmptyResult();
            }

            Document doc = fetchDocument(detailUrl);
            Vod vod = parseDetailVod(doc, vodId);
            
            Result result = new Result();
            List<Vod> vodList = new ArrayList<>();
            vodList.add(vod);
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyResult();
        }
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        try {
            String searchUrl = buildSearchUrl(key);
            if (TextUtils.isEmpty(searchUrl)) {
                return createEmptyResult();
            }

            Document doc = fetchDocument(searchUrl);
            List<Vod> vodList = parseSearchVods(doc);
            
            Result result = new Result();
            result.setList(vodList);
            return Json.toJson(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyResult();
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
            return "{}";
        }
    }

    /**
     * 获取网页文档
     */
    private Document fetchDocument(String url) throws Exception {
        return Jsoup.connect(url).headers(headers).get();
    }

    /**
     * 解析首页视频列表
     */
    private List<Vod> parseHomeVods(Document doc) {
        List<Vod> vodList = new ArrayList<>();
        
        String listSelector = config.get("homeListSelector");
        if (TextUtils.isEmpty(listSelector)) {
            return vodList;
        }

        Elements elements = doc.select(listSelector);
        for (Element element : elements) {
            Vod vod = parseVodFromElement(element);
            if (vod != null) {
                vodList.add(vod);
            }
        }
        
        return vodList;
    }

    /**
     * 解析分类视频列表
     */
    private List<Vod> parseCategoryVods(Document doc) {
        return parseHomeVods(doc); // 使用相同的解析逻辑
    }

    /**
     * 解析搜索结果
     */
    private List<Vod> parseSearchVods(Document doc) {
        return parseHomeVods(doc); // 使用相同的解析逻辑
    }

    /**
     * 从元素解析Vod对象
     */
    private Vod parseVodFromElement(Element element) {
        try {
            Vod vod = new Vod();
            
            // 解析标题
            String titleSelector = config.get("titleSelector");
            if (!TextUtils.isEmpty(titleSelector)) {
                vod.setVodName(getTextBySelector(element, titleSelector));
            }
            
            // 解析链接
            String linkSelector = config.get("linkSelector");
            if (!TextUtils.isEmpty(linkSelector)) {
                vod.setVodId(getAttrBySelector(element, linkSelector, "href"));
            }
            
            // 解析图片
            String picSelector = config.get("picSelector");
            if (!TextUtils.isEmpty(picSelector)) {
                vod.setVodPic(getAttrBySelector(element, picSelector, "src"));
            }
            
            return vod;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析详情页
     */
    private Vod parseDetailVod(Document doc, String vodId) {
        Vod vod = new Vod();
        vod.setVodId(vodId);
        
        // 解析详情信息
        String nameSelector = config.get("detailNameSelector");
        if (!TextUtils.isEmpty(nameSelector)) {
            vod.setVodName(getTextBySelector(doc, nameSelector));
        }
        
        return vod;
    }

    /**
     * 构建分类URL
     */
    private String buildCategoryUrl(String tid, String pg, HashMap<String, String> extend) {
        String categoryUrlTemplate = config.get("categoryUrlTemplate");
        if (TextUtils.isEmpty(categoryUrlTemplate)) {
            return null;
        }
        
        return categoryUrlTemplate
            .replace("{tid}", tid)
            .replace("{pg}", pg);
    }

    /**
     * 构建详情URL
     */
    private String buildDetailUrl(String vodId) {
        String detailUrlTemplate = config.get("detailUrlTemplate");
        if (TextUtils.isEmpty(detailUrlTemplate)) {
            return vodId;
        }
        
        return detailUrlTemplate.replace("{id}", vodId);
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String key) {
        String searchUrlTemplate = config.get("searchUrlTemplate");
        if (TextUtils.isEmpty(searchUrlTemplate)) {
            return null;
        }
        
        return searchUrlTemplate.replace("{key}", key);
    }

    /**
     * 解析播放地址
     */
    private String parsePlayUrl(String flag, String id) {
        return id; // 简单返回原始地址
    }

    /**
     * 工具方法：通过选择器获取文本
     */
    private String getTextBySelector(Element element, String selector) {
        try {
            Element target = element.selectFirst(selector);
            return target != null ? target.text().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 工具方法：通过选择器获取属性
     */
    private String getAttrBySelector(Element element, String selector, String attr) {
        try {
            Element target = element.selectFirst(selector);
            return target != null ? target.attr(attr).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 创建空结果
     */
    private String createEmptyResult() {
        Result result = new Result();
        result.setList(new ArrayList<>());
        return Json.toJson(result);
    }
}
