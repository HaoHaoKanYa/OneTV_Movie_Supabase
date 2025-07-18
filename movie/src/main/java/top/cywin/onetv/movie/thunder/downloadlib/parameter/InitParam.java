package top.cywin.onetv.movie.thunder.downloadlib.parameter;

import android.util.Base64;

import top.cywin.onetv.movie.thunder.downloadlib.android.XLUtil;

import java.nio.charset.StandardCharsets;

public class InitParam {

    public String mAppKey;
    public String mAppVersion;
    public String mStatSavePath;
    public String mStatCfgSavePath;
    public int mQueryConfOnInit;
    public int mPermissionLevel;

    public InitParam(String path) {
        this.mAppKey = "xzNjAwMQ^^yb==0^852^083dbcff^cee25055f125a2fde";
        this.mAppVersion = "21.01.07.800002";
        this.mPermissionLevel = 1;
        this.mQueryConfOnInit = 0;
        this.mStatSavePath = path;
        this.mStatCfgSavePath = path;
    }

    public String getSoKey() {
        String[] split = mAppKey.split("==");
        String replace = split[0].replace('^', '=');
        String str = new String(Base64.decode(replace.substring(2, replace.length() - 2), 0), StandardCharsets.UTF_8);
        return XLUtil.generateAppKey("com.onetv.providers.downloads", Short.parseShort(str.split(";")[0]), (byte) 1);
    }
}
