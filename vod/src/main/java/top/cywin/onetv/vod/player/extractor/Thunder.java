package top.cywin.onetv.vod.player.extractor;

import android.net.Uri;
import android.os.SystemClock;

import top.cywin.onetv.vod.bean.Episode;
import top.cywin.onetv.vod.exception.ExtractException;
import top.cywin.onetv.vod.player.Source;
import top.cywin.onetv.vod.utils.Download;
import top.cywin.onetv.vod.utils.UrlUtil;
import top.github.catvod.utils.Path;
import top.github.catvod.utils.Util;
import top.xunlei.downloadlib.XLTaskHelper;
import top.xunlei.downloadlib.parameter.GetTaskId;
import top.xunlei.downloadlib.parameter.TorrentFileInfo;
import top.xunlei.downloadlib.parameter.XLTaskInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class Thunder implements Source.Extractor {

    private GetTaskId taskId;

    @Override
    public boolean match(String scheme, String host) {
        return "magnet".equals(scheme) || "ed2k".equals(scheme);
    }

    @Override
    public String fetch(String url) throws Exception {
        return UrlUtil.scheme(url).equals("magnet") ? addTorrentTask(Uri.parse(url)) : addThunderTask(url);
    }

    private String addTorrentTask(Uri uri) throws Exception {
        File torrent = new File(uri.getPath());
        String name = uri.getQueryParameter("name");
        int index = Integer.parseInt(uri.getQueryParameter("index"));
        taskId = XLTaskHelper.get().addTorrentTask(torrent, Objects.requireNonNull(torrent.getParentFile()), index);
        while (true) {
            XLTaskInfo taskInfo = XLTaskHelper.get().getBtSubTaskInfo(taskId, index).mTaskInfo;
            if (taskInfo.mTaskStatus == 3) throw new ExtractException(taskInfo.getErrorMsg());
            if (taskInfo.mTaskStatus != 0) return XLTaskHelper.get().getLocalUrl(new File(torrent.getParent(), name));
            else SystemClock.sleep(300);
        }
    }

    private String addThunderTask(String url) {
        File folder = Path.thunder(Util.md5(url));
        taskId = XLTaskHelper.get().addThunderTask(url, folder);
        return XLTaskHelper.get().getLocalUrl(taskId.getSaveFile());
    }

    @Override
    public void stop() {
        if (taskId == null) return;
        XLTaskHelper.get().deleteTask(taskId);
        taskId = null;
    }

    @Override
    public void exit() {
        XLTaskHelper.get().release();
    }

    public static class Parser implements Callable<List<Episode>> {

        private static final Pattern THUNDER = Pattern.compile("(magnet|thunder|ed2k):.*");
        private final String url;
        private int time;

        public static boolean match(String url) {
            return THUNDER.matcher(url).find() || isTorrent(url);
        }

        public static Parser get(String url) {
            return new Parser(url);
        }

        public Parser(String url) {
            this.url = url;
        }

        private void sleep() {
            SystemClock.sleep(10);
            time += 10;
        }

        private static boolean isTorrent(String url) {
            return !url.startsWith("magnet") && url.split(";")[0].endsWith(".torrent");
        }

        @Override
        public List<Episode> call() {
            boolean torrent = isTorrent(url);
            List<Episode> episodes = new ArrayList<>();
            GetTaskId taskId = XLTaskHelper.get().parse(url, Path.thunder(Util.md5(url)));
            if (!torrent && !taskId.getRealUrl().startsWith("magnet")) return Arrays.asList(Episode.create(taskId.getFileName(), taskId.getRealUrl()));
            if (torrent) Download.create(url, taskId.getSaveFile()).start();
            else while (XLTaskHelper.get().getTaskInfo(taskId).getTaskStatus() != 2 && time < 5000) sleep();
            List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
            for (TorrentFileInfo media : medias) episodes.add(Episode.create(media.getFileName(), media.getSize(), media.getPlayUrl()));
            XLTaskHelper.get().stopTask(taskId);
            return episodes;
        }
    }
}
