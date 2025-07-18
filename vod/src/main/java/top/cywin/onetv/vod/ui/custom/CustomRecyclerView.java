package top.cywin.onetv.vod.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.utils.ResUtil;

public class CustomRecyclerView extends RecyclerView {

    private int maxHeight;
    private float x1;
    private float y1;

    public CustomRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = ResUtil.dp2px(maxHeight);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.vod_CustomRecyclerView);
        maxHeight = a.getLayoutDimension(R.styleable.vod_CustomRecyclerView_vod_maxHeight, maxHeight);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        a.recycle();
    }

    private void focus(int position) {
        ViewHolder holder = findViewHolderForLayoutPosition(position);
        if (holder != null) holder.itemView.requestFocus();
    }

    private int getNewSpec(int heightMeasureSpec) {
        int newHeight = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        if (heightMeasureSpec > newHeight) heightMeasureSpec = newHeight;
        return heightMeasureSpec;
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        postDelayed(() -> focus(position), 50);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight != 0) heightMeasureSpec = getNewSpec(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getPointerCount() != 1) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                x1 = y1 = 0;
                break;
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float x2 = event.getX();
                float y2 = event.getY();
                float offsetX = Math.abs(x2 - x1);
                float offsetY = Math.abs(y2 - y1);
                if (Math.abs(offsetX) > Math.abs(offsetY)) getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
