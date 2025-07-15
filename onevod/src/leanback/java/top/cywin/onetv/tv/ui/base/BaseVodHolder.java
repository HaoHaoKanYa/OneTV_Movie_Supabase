package top.cywin.onetv.tv.ui.base;

import android.view.View;

import androidx.leanback.widget.Presenter;

import top.cywin.onetv.tv.bean.Vod;

public abstract class BaseVodHolder extends Presenter.ViewHolder {

    public BaseVodHolder(View view) {
        super(view);
    }

    public abstract void initView(Vod item);
}
