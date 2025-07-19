package top.cywin.onetv.movie.adapter;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import top.cywin.onetv.movie.ui.model.*;

/**
 * ViewModel适配器 - 纯粹的数据转换器
 * 只负责FongMi_TV数据模型与Compose UI数据模型的转换
 */
public class ViewModelAdapter {

    private static final String TAG = "ViewModelAdapter";

    public ViewModelAdapter() {
        Log.d(TAG, "🏗️ ViewModelAdapter 初始化完成");
    }

    /**
     * 转换FongMi_TV的Vod为Compose UI的Movie
     */
    public static MovieItem convertVodToMovie(top.cywin.onetv.movie.bean.Vod vod) {
        if (vod == null) return null;

        return new MovieItem(
            vod.getVodId(),
            vod.getVodName(),
            vod.getVodPic(),
            vod.getVodRemarks(),
            vod.getVodYear(),
            vod.getVodArea(),
            vod.getVodDirector(),
            vod.getVodActor(),
            vod.getVodContent(),
            vod.getSite() != null ? vod.getSite().getKey() : "",
            false, // isCollected - 需要单独查询
            0f,    // watchProgress - 需要单独查询
            0L     // lastWatchTime - 需要单独查询
        );
    }

    /**
     * 转换FongMi_TV的Site为Compose UI的SiteInfo
     */
    public static SiteInfo convertSiteToSiteInfo(top.cywin.onetv.movie.bean.Site site) {
        if (site == null) return null;

        return new SiteInfo(
            site.getKey(),
            site.getName(),
            site.getApi(),
            site.getSearchable() == 1,
            site.getPlayable() == 1,
            false // isActive - 需要单独判断
        );
    }

    /**
     * 转换FongMi_TV的Class为Compose UI的Category
     */
    public static CategoryInfo convertClassToCategory(top.cywin.onetv.movie.bean.Class clazz) {
        if (clazz == null) return null;

        return new CategoryInfo(
            clazz.getTypeId(),
            clazz.getTypeName(),
            clazz.getTypeFlag(),
            false // isSelected - 需要单独设置
        );
    }

    /**
     * 转换FongMi_TV的Result为Compose UI的SearchResult
     */
    public static SearchResult convertResultToSearchResult(top.cywin.onetv.movie.bean.Result result) {
        if (result == null) return null;

        List<MovieItem> movies = new ArrayList<>();
        if (result.getList() != null) {
            for (top.cywin.onetv.movie.bean.Vod vod : result.getList()) {
                MovieItem movie = convertVodToMovie(vod);
                if (movie != null) {
                    movies.add(movie);
                }
            }
        }

        return new SearchResult(
            movies,
            result.getPage(),
            result.getPagecount(),
            result.getTotal()
        );
    }

    /**
     * 转换播放标志列表
     */
    public static List<PlayFlag> convertVodFlags(top.cywin.onetv.movie.bean.Vod vod) {
        List<PlayFlag> flags = new ArrayList<>();

        if (vod != null && vod.getVodFlags() != null) {
            String[] flagArray = vod.getVodFlags().split("\\$\\$\\$");
            String[] urlArray = vod.getVodUrls().split("\\$\\$\\$");

            for (int i = 0; i < flagArray.length && i < urlArray.length; i++) {
                flags.add(new PlayFlag(
                    flagArray[i],
                    urlArray[i],
                    i == 0 // 第一个为默认选中
                ));
            }
        }

        return flags;
    }

    /**
     * 转换剧集列表
     */
    public static List<Episode> convertVodEpisodes(String flagUrls) {
        List<Episode> episodes = new ArrayList<>();

        if (flagUrls != null && !flagUrls.isEmpty()) {
            String[] episodeArray = flagUrls.split("#");

            for (int i = 0; i < episodeArray.length; i++) {
                String[] parts = episodeArray[i].split("\\$");
                if (parts.length >= 2) {
                    episodes.add(new Episode(
                        i,
                        parts[0], // 剧集名称
                        parts[1], // 播放地址
                        false,    // 未播放
                        0f        // 进度
                    ));
                }
            }
        }

        return episodes;
    }

    /**
     * 转换观看历史
     */
    public static WatchHistory convertHistoryToWatchHistory(top.cywin.onetv.movie.bean.History history) {
        if (history == null) return null;

        return new WatchHistory(
            history.getVodId(),
            history.getVodName(),
            "", // siteKey - 需要从其他地方获取
            history.getVodRemarks() != null ? history.getVodRemarks() : "",
            history.getPosition(),
            history.getDuration(),
            history.getCreateTime(),
            history.getPosition() >= history.getDuration() * 0.9
        );
    }

    /**
     * 转换收藏项
     */
    public static FavoriteItem convertKeepToFavorite(top.cywin.onetv.movie.bean.Keep keep) {
        if (keep == null) return null;

        return new FavoriteItem(
            keep.getVodId(),
            keep.getVodName(),
            keep.getVodPic(),
            keep.getSiteName(), // 使用siteName作为siteKey
            keep.getSiteName(),
            keep.getCreateTime()
        );
    }

    /**
     * 批量转换Vod列表为MovieItem列表
     */
    public static List<MovieItem> convertVodListToMovieList(List<top.cywin.onetv.movie.bean.Vod> vodList) {
        List<MovieItem> movieList = new ArrayList<>();

        if (vodList != null) {
            for (top.cywin.onetv.movie.bean.Vod vod : vodList) {
                MovieItem movie = convertVodToMovie(vod);
                if (movie != null) {
                    movieList.add(movie);
                }
            }
        }

        return movieList;
    }

    /**
     * 批量转换Site列表为SiteInfo列表
     */
    public static List<SiteInfo> convertSiteListToSiteInfoList(List<top.cywin.onetv.movie.bean.Site> siteList) {
        List<SiteInfo> siteInfoList = new ArrayList<>();

        if (siteList != null) {
            for (top.cywin.onetv.movie.bean.Site site : siteList) {
                SiteInfo siteInfo = convertSiteToSiteInfo(site);
                if (siteInfo != null) {
                    siteInfoList.add(siteInfo);
                }
            }
        }

        return siteInfoList;
    }

    /**
     * 批量转换Class列表为CategoryInfo列表
     */
    public static List<CategoryInfo> convertClassListToCategoryList(List<top.cywin.onetv.movie.bean.Class> classList) {
        List<CategoryInfo> categoryList = new ArrayList<>();

        if (classList != null) {
            for (top.cywin.onetv.movie.bean.Class clazz : classList) {
                CategoryInfo category = convertClassToCategory(clazz);
                if (category != null) {
                    categoryList.add(category);
                }
            }
        }

        return categoryList;
    }

    /**
     * 转换云盘配置对象为UI模型
     */
    public static top.cywin.onetv.movie.ui.model.CloudDriveConfig convertToCloudDriveConfig(Object config) {
        if (config == null) return null;

        try {
            // 这里需要根据实际的FongMi_TV云盘配置对象进行转换
            // 暂时返回一个示例配置
            return new top.cywin.onetv.movie.ui.model.CloudDriveConfig(
                "default_id",
                "默认云盘",
                "alist",
                "http://localhost:5244",
                "",
                "",
                true
            );
        } catch (Exception e) {
            Log.e(TAG, "转换云盘配置失败", e);
            return null;
        }
    }

    /**
     * 转换云盘文件对象为UI模型
     */
    public static top.cywin.onetv.movie.ui.model.CloudFile convertToCloudFile(Object file) {
        if (file == null) return null;

        try {
            // 这里需要根据实际的FongMi_TV云盘文件对象进行转换
            if (file instanceof top.cywin.onetv.movie.cloudrive.bean.CloudFile) {
                top.cywin.onetv.movie.cloudrive.bean.CloudFile cloudFile =
                    (top.cywin.onetv.movie.cloudrive.bean.CloudFile) file;

                return new top.cywin.onetv.movie.ui.model.CloudFile(
                    cloudFile.getName(),
                    cloudFile.getPath(),
                    cloudFile.getSize(),
                    cloudFile.isFolder(),
                    System.currentTimeMillis(), // lastModified
                    cloudFile.getDownloadUrl()  // playUrl
                );
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "转换云盘文件失败", e);
            return null;
        }
    }
}
