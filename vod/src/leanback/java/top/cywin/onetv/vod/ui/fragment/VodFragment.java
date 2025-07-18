package top.cywin.onetv.vod.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.Product;
import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Filter;
import top.cywin.onetv.vod.bean.Page;
import top.cywin.onetv.vod.bean.Result;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.bean.Style;
import top.cywin.onetv.vod.bean.Value;
import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.VodFragmentVodBinding;
import top.cywin.onetv.vod.model.SiteViewModel;
import top.cywin.onetv.vod.ui.activity.CollectActivity;
import top.cywin.onetv.vod.ui.activity.VideoActivity;
import top.cywin.onetv.vod.ui.base.BaseFragment;
import top.cywin.onetv.vod.ui.custom.CustomRowPresenter;
import top.cywin.onetv.vod.ui.custom.CustomScroller;
import top.cywin.onetv.vod.ui.custom.CustomSelector;
import top.cywin.onetv.vod.ui.presenter.FilterPresenter;
import top.cywin.onetv.vod.ui.presenter.VodPresenter;
import top.cywin.onetv.vod.utils.Notify;
import top.cywin.onetv.vod.utils.ResUtil;
import top.github.catvod.utils.Prefers;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VodFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private HashMap<String, String> mExtends;
    private VodFragmentVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<Filter> mFilters;
    private List<Page> mPages;
    private boolean mOpen;
    private Page mPage;

    public static VodFragment newInstance(String key, String typeId, Style style, HashMap<String, String> extend, boolean folder) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        args.putParcelable("style", style);
        args.putSerializable("extend", extend);
        VodFragment fragment = new VodFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKey() {
        return getArguments().getString("key");
    }

    private String getTypeId() {
        return mPages.isEmpty() ? getArguments().getString("typeId") : getLastPage().getVodId();
    }

    private List<Filter> getFilter() {
        return Filter.arrayFrom(Prefers.getString("filter_" + getKey() + "_" + getTypeId()));
    }

    private HashMap<String, String> getExtend() {
        Serializable extend = getArguments().getSerializable("extend");
        return extend == null ? new HashMap<>() : (HashMap<String, String>) extend;
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    private Style getStyle() {
        return isFolder() ? Style.list() : getSite().getStyle(mPages.isEmpty() ? getArguments().getParcelable("style") : getLastPage().getStyle());
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = VodFragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mPages = new ArrayList<>();
        mExtends = getExtend();
        mFilters = getFilter();
        setRecyclerView();
        setViewModel();
        setFilters();
    }

    @Override
    protected void initData() {
        getVideo();
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED), FilterPresenter.class);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setHeader(getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.action.observe(getViewLifecycleOwner(), result -> Notify.show(result.getMsg()));
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            boolean first = mScroller.first();
            int size = result.getList().size();
            if (size > 0) addVideo(result);
            mScroller.endLoading(result);
            checkPosition(first);
            checkMore(size);
            hideProgress();
        });
    }

    private void setFilters() {
        for (Filter filter : mFilters) {
            if (mExtends.containsKey(filter.getKey())) {
                filter.setActivated(mExtends.get(filter.getKey()));
            }
        }
    }

    private void setClick(ArrayObjectAdapter adapter, String key, Value item) {
        for (int i = 0; i < adapter.size(); i++) ((Value) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        if (item.isActivated()) mExtends.put(key, item.getV());
        else mExtends.remove(key);
        onRefresh();
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void getVideo(String typeId, String page) {
        boolean first = "1".equals(page);
        if (first) mLast = null;
        if (first) showProgress();
        int filterSize = mOpen ? mFilters.size() : 0;
        boolean clear = first && mAdapter.size() > filterSize;
        if (clear) mAdapter.removeItems(filterSize, mAdapter.size() - filterSize);
        mViewModel.categoryContent(getKey(), typeId, page, true, mExtends);
    }

    private void addVideo(Result result) {
        Style style = result.getStyle(getStyle());
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
    }

    private void checkPosition(boolean first) {
        if (mPage != null && mPage.getPosition() > 0) mBinding.recycler.hideHeader();
        if (mPage != null && mPage.getPosition() < 1) mBinding.recycler.showHeader();
        if (mPage != null) mBinding.recycler.setSelectedPosition(mPage.getPosition());
        else if (first && !mOpen) mBinding.recycler.moveToTop();
        mPage = null;
    }

    private void checkMore(int count) {
        if (mScroller.isDisable() || count == 0 || mAdapter.size() >= 5) return;
        getVideo(getTypeId(), String.valueOf(mScroller.addPage()));
    }

    private boolean checkLastSize(List<Vod> items, Style style) {
        if (mLast == null || items.size() == 0) return false;
        int size = Product.getColumn(style) - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addGrid(new ArrayList<>(items.subList(size, items.size())), style);
        return true;
    }

    private void addGrid(List<Vod> items, Style style) {
        if (checkLastSize(items, style)) return;
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this, style));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private ListRow getRow(Filter filter) {
        FilterPresenter presenter = new FilterPresenter(filter.getKey());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        presenter.setOnClickListener((key, item) -> setClick(adapter, key, item));
        adapter.setItems(filter.getValue(), null);
        return new ListRow(adapter);
    }

    private void showProgress() {
        if (!mOpen) mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    private void showFilter() {
        List<ListRow> rows = new ArrayList<>();
        for (Filter filter : mFilters) rows.add(getRow(filter));
        App.post(() -> mBinding.recycler.scrollToPosition(0), 48);
        mAdapter.addAll(0, rows);
        hideProgress();
    }

    private void hideFilter() {
        mAdapter.removeItems(0, mFilters.size());
    }

    public void toggleFilter(boolean open) {
        if (open) showFilter();
        else hideFilter();
        mOpen = open;
    }

    public void onRefresh() {
        getVideo();
    }

    public boolean canBack() {
        return !mPages.isEmpty();
    }

    public void goBack() {
        if (mPages.size() == 1) mBinding.recycler.setMoveTop(true);
        mPages.remove(mPage = getLastPage());
        onRefresh();
    }

    public boolean goRoot() {
        if (mPages.isEmpty()) return false;
        mPages.clear();
        getVideo();
        return true;
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) {
            mViewModel.action(getKey(), item.getAction());
        } else if (item.isFolder()) {
            mPages.add(Page.get(item, mBinding.recycler.getSelectedPosition()));
            mBinding.recycler.setMoveTop(false);
            getVideo(item.getVodId(), "1");
        } else {
            if (getSite().isIndex()) CollectActivity.start(getActivity(), item.getVodName());
            else VideoActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodPic(), isFolder() ? item.getVodName() : null);
        }
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public void onLoadMore(String page) {
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
