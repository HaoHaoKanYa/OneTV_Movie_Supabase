package top.cywin.onetv.vod.bean;

import android.annotation.SuppressLint;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.utils.ResUtil;

public class Func {

    private final int resId;
    private int drawable;

    public static Func create(int resId) {
        return new Func(resId);
    }

    public Func(int resId) {
        this.resId = resId;
        this.setDrawable();
    }

    public int getResId() {
        return resId;
    }

    public int getDrawable() {
        return drawable;
    }

    public String getText() {
        return ResUtil.getString(resId);
    }

    @SuppressLint("NonConstantResourceId")
    public void setDrawable() {
        if (resId == R.string.home_vod) {
            this.drawable = R.drawable.vod_ic_home_vod;
        } else if (resId == R.string.home_live) {
            this.drawable = R.drawable.vod_ic_home_live;
        } else if (resId == R.string.home_keep) {
            this.drawable = R.drawable.vod_ic_home_keep;
        } else if (resId == R.string.home_push) {
            this.drawable = R.drawable.vod_ic_home_push;
        } else if (resId == R.string.home_cast) {
            this.drawable = R.drawable.vod_ic_home_cast;
        } else if (resId == R.string.home_search) {
            this.drawable = R.drawable.vod_ic_home_search;
        } else if (resId == R.string.home_setting) {
            this.drawable = R.drawable.vod_ic_home_setting;
        }
    }
}
