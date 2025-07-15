package top.cywin.onetv.vod.ui.base;

import android.view.View;

import androidx.leanback.widget.Presenter;

import top.cywin.onetv.vod.bean.Vod;

public abstract class BaseVodHolder extends Presenter.ViewHolder {

    public BaseVodHolder(View view) {
        super(view);
    }

    public abstract void initView(Vod item);
}
