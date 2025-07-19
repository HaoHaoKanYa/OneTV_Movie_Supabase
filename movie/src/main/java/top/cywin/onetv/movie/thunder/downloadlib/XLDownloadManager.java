package top.cywin.onetv.movie.thunder.downloadlib;

import android.content.Context;
import android.os.Build;

import top.cywin.onetv.movie.catvod.Init;
import top.cywin.onetv.movie.catvod.utils.Prefers;
import top.cywin.onetv.movie.thunder.downloadlib.android.XLUtil;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.BtIndexSet;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.BtSubTaskDetail;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.BtTaskParam;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.EmuleTaskParam;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.GetDownloadLibVersion;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.GetFileName;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.GetTaskId;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.InitParam;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.MagnetTaskParam;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.P2spTaskParam;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.ThunderUrlInfo;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.TorrentInfo;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.XLTaskInfo;
import top.cywin.onetv.movie.thunder.downloadlib.parameter.XLTaskLocalUrl;

public class XLDownloadManager {

    private XLLoader loader;
    private Context context;

    public XLDownloadManager() {
        this.context = Init.context();
        this.loader = new XLLoader();
        this.init();
    }

    public void init() {
        InitParam param = new InitParam(context.getFilesDir().getPath());
        loader.init(param.getSoKey(), "com.onetv.providers.downloads", param.mAppVersion, "", getPeerId(), getGuid(), param.mStatSavePath, param.mStatCfgSavePath, 0, param.mPermissionLevel, param.mQueryConfOnInit);
        getDownloadLibVersion(new GetDownloadLibVersion());
        setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
        setLocalProperty("PhoneModel", Build.MODEL);
        setStatReportSwitch(false);
        setSpeedLimit(-1, -1);
    }

    public void release() {
        if (loader != null) loader.unInit();
        context = null;
        loader = null;
    }

    private String getPeerId() {
        String uuid = Prefers.getString("phoneId5");
        if (uuid.isEmpty()) Prefers.put("phoneId5", uuid = XLUtil.getPeerId());
        return uuid;
    }

    private String getGuid() {
        return XLUtil.getGuid();
    }

    public void releaseTask(long taskId) {
        loader.releaseTask(taskId);
    }

    public void startTask(long taskId) {
        loader.startTask(taskId);
    }

    public void stopTask(long taskId) {
        loader.stopTask(taskId);
    }

    public void getTaskInfo(long taskId, int i, XLTaskInfo taskInfo) {
        loader.getTaskInfo(taskId, i, taskInfo);
    }

    public void getLocalUrl(String filePath, XLTaskLocalUrl localUrl) {
        loader.getLocalUrl(filePath, localUrl);
    }

    public void getDownloadLibVersion(GetDownloadLibVersion version) {
        loader.getDownloadLibVersion(version);
    }

    public void setStatReportSwitch(boolean value) {
        loader.setStatReportSwitch(value);
    }

    private void setLocalProperty(String key, String value) {
        loader.setLocalProperty(key, value);
    }

    public void setOSVersion(String str) {
        loader.setMiUiVersion(str);
    }

    public void setOriginUserAgent(long taskId, String userAgent) {
        loader.setOriginUserAgent(taskId, userAgent);
    }

    public void setDownloadTaskOrigin(long taskId, String str) {
        loader.setDownloadTaskOrigin(taskId, str);
    }

    public void setTaskGsState(long j, int i, int i2) {
        loader.setTaskGsState(j, i, i2);
    }

    public int createP2spTask(P2spTaskParam param, GetTaskId taskId) {
        return loader.createP2spTask(param.mUrl, param.mRefUrl, param.mCookie, param.mUser, param.mPass, param.mFilePath, param.mFileName, param.mCreateMode, param.mSeqId, taskId);
    }

    public int createBtMagnetTask(MagnetTaskParam param, GetTaskId taskId) {
        return loader.createBtMagnetTask(param.mUrl, param.mFilePath, param.mFileName, taskId);
    }

    public int createEmuleTask(EmuleTaskParam param, GetTaskId taskId) {
        return loader.createEmuleTask(param.mUrl, param.mFilePath, param.mFileName, param.mCreateMode, param.mSeqId, taskId);
    }

    public int createBtTask(BtTaskParam param, GetTaskId taskId) {
        return loader.createBtTask(param.mTorrentPath, param.mFilePath, param.mMaxConcurrent, param.mCreateMode, param.mSeqId, taskId);
    }

    public void getTorrentInfo(TorrentInfo info) {
        loader.getTorrentInfo(info.getFile().getAbsolutePath(), info);
    }

    public void getBtSubTaskInfo(long taskId, int index, BtSubTaskDetail detail) {
        loader.getBtSubTaskInfo(taskId, index, detail);
    }

    public void deselectBtSubTask(long taskId, BtIndexSet btIndexSet) {
        loader.deselectBtSubTask(taskId, btIndexSet);
    }

    public String parserThunderUrl(String url) {
        ThunderUrlInfo thunderUrlInfo = new ThunderUrlInfo();
        loader.parserThunderUrl(url, thunderUrlInfo);
        return thunderUrlInfo.mUrl;
    }

    public String getFileNameFromUrl(String url) {
        GetFileName getFileName = new GetFileName();
        loader.getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    public void setSpeedLimit(long min, long max) {
        loader.setSpeedLimit(min, max);
    }
}
