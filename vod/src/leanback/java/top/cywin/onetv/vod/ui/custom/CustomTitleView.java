package top.cywin.onetv.vod.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.impl.SiteCallback;
import top.cywin.onetv.vod.utils.KeyUtil;
import top.cywin.onetv.vod.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomTitleView extends AppCompatTextView {

    private Listener listener;
    private Animation flicker;
    private boolean coolDown;

    public CustomTitleView(@NonNull Context context) {
        super(context);
    }

    public CustomTitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        flicker = ResUtil.getAnim(R.anim.vod_flicker);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        setOnClickListener(v -> listener.showDialog());
    }

    private boolean hasEvent(KeyEvent event) {
        return !getSites().isEmpty() && (KeyUtil.isEnterKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event) || (KeyUtil.isUpKey(event) && !coolDown));
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) startAnimation(flicker);
        else clearAnimation();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && KeyUtil.isEnterKey(event)) {
            listener.showDialog();
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && KeyUtil.isLeftKey(event)) {
            listener.setSite(getSite(true));
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && KeyUtil.isRightKey(event)) {
            listener.setSite(getSite(false));
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && KeyUtil.isUpKey(event)) {
            onKeyUp();
        }
        return true;
    }

    private void onKeyUp() {
        App.post(() -> coolDown = false, 3000);
        listener.onRefresh();
        coolDown = true;
    }

    private Site getSite(boolean next) {
        List<Site> items = getSites();
        int position = VodConfig.getHomeIndex();
        if (next) position = position > 0 ? --position : items.size() - 1;
        else position = position < items.size() - 1 ? ++position : 0;
        return items.get(position);
    }

    private List<Site> getSites() {
        List<Site> items = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) if (!site.isHide()) items.add(site);
        return items;
    }

    public interface Listener extends SiteCallback {

        void showDialog();

        void onRefresh();
    }
}
