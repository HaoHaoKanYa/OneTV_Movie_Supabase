package top.cywin.onetv.vod.ui.base;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Vod;

public abstract class BaseVodHolder extends RecyclerView.ViewHolder {

    public BaseVodHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void initView(Vod item);
}
