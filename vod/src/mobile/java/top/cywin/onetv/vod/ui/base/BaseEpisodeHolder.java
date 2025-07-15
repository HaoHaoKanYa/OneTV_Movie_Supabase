package top.cywin.onetv.vod.ui.base;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Episode;

public abstract class BaseEpisodeHolder extends RecyclerView.ViewHolder {

    public BaseEpisodeHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void initView(Episode item);
}
