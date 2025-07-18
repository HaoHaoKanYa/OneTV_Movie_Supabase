package top.cywin.onetv.movie.bean;

import android.text.TextUtils;

import top.cywin.onetv.movie.App;
import top.cywin.onetv.movie.api.EpgParser;
import top.cywin.onetv.movie.utils.Util;
import top.cywin.onetv.movie.catvod.utils.Json;
import top.cywin.onetv.movie.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class Epg {

    @SerializedName("key")
    private String key;
    @SerializedName("date")
    private String date;
    @SerializedName("epg_data")
    private List<EpgData> list;

    private int width;

    public static Epg objectFrom(String str, String key, List<SimpleDateFormat> formats) throws Exception {
        if (!Json.isObj(str)) return EpgParser.getEpg(str, key);
        Epg item = App.gson().fromJson(str, Epg.class);
        item.setTime(formats);
        item.setKey(key);
        return item;
    }

    public static Epg create(String key, String date) {
        Epg item = new Epg();
        item.setKey(key);
        item.setDate(date);
        item.setList(new ArrayList<>());
        return item;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDate() {
        return TextUtils.isEmpty(date) ? "" : date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<EpgData> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<EpgData> list) {
        this.list = list;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean equal(String date) {
        return getDate().equals(date);
    }

    private void setTime(List<SimpleDateFormat> formats) {
        setList(new ArrayList<>(new LinkedHashSet<>(getList())));
        for (EpgData item : getList()) {
            item.setStartTime(Util.format(getDate().concat(item.getStart()), formats));
            item.setEndTime(Util.format(getDate().concat(item.getEnd()), formats));
            if (item.getEndTime() < item.getStartTime()) item.checkDay();
            item.setTitle(Trans.s2t(item.getTitle()));
        }
    }

    public EpgData getEpgData() {
        for (EpgData item : getList()) if (item.isSelected()) return item;
        return new EpgData();
    }

    public Epg selected() {
        for (EpgData item : getList()) item.setSelected(item.isInRange());
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isSelected()) return i;
        return -1;
    }

    public int getInRange() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isInRange()) return i;
        return -1;
    }
}
