package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.Product;
import top.cywin.onetv.vod
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Collect;
import top.cywin.onetv.vod.bean.Hot;
import top.cywin.onetv.vod.bean.Result;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.bean.Suggest;
import top.cywin.onetv.vod.bean.Vod;
import top.cywin.onetv.vod.databinding.VodActivityCollectBinding;
import top.cywin.onetv.vod.impl.Callback;
import top.cywin.onetv.vod.impl.SiteCallback;
import top.cywin.onetv.vod.model.SiteViewModel;
import top.cywin.onetv.vod.ui.adapter.CollectAdapter;
import top.cywin.onetv.vod.ui.adapter.RecordAdapter;
import top.cywin.onetv.vod.ui.adapter.SearchAdapter;
import top.cywin.onetv.vod.ui.adapter.VodAdapter;
import com.fongmi.onetv.tv.ui.adapter.WordAdapter;
import com.fongmi.onetv.tv.ui.base.BaseActivity;
import com.fongmi.onetv.tv.ui.base.ViewType;
import com.fongmi.onetv.tv.ui.custom.CustomScroller;
import com.fongmi.onetv.tv.ui.custom.CustomTextListener;
import com.fongmi.onetv.tv.ui.dialog.SiteDialog;
import com.fongmi.onetv.tv.utils.PauseExecutor;
import com.fongmi.onetv.tv.utils.ResUtil;
import com.fongmi.onetv.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class CollectActivity extends BaseActivity implements CustomScroller.Callback, SiteCallback, WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CollectAdapter.OnClickListener, VodAdapter.OnClickListener {

    private VodActivityCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SearchAdapter mSearchAdapter;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private PauseExecutor mExecutor;
    private List<Site> mSites;

    public static void start(Activity activity) {
        start(activity, "");
    }

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("keyword", keyword);
        activity.startActivity(intent);
    }

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    private boolean empty() {
        return mBinding.keyword.getText().toString().trim().isEmpty();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mScroller = new CustomScroller(this);
        mSites = new ArrayList<>();
        setRecyclerView();
        setViewModel();
        checkKeyword();
        setViewType();
        setSite();
        getHot();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.site.setOnClickListener(this::onSite);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) search();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.collect.setHasFixedSize(true);
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addOnScrollListener(mScroller);
        mBinding.recycler.setAdapter(mSearchAdapter = new SearchAdapter(this));
        mBinding.wordRecycler.setHasFixedSize(false);
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.wordRecycler.setLayoutManager(new FlexboxLayoutManager(this, FlexDirection.ROW));
        mBinding.recordRecycler.setHasFixedSize(false);
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
        mBinding.recordRecycler.setLayoutManager(new FlexboxLayoutManager(this, FlexDirection.ROW));
    }

    private void setViewType() {
        setViewType(Setting.getViewType(ViewType.GRID));
    }

    private void setViewType(int viewType) {
        int count = Product.getColumn(this) - 1;
        mSearchAdapter.setViewType(viewType, count);
        mSearchAdapter.setSize(Product.getSpec(this, ResUtil.dp2px(128 + (count) * 16), count));
        ((GridLayoutManager) mBinding.recycler.getLayoutManager()).setSpanCount(mSearchAdapter.isGrid() ? count : 1);
        mBinding.view.setImageResource(mSearchAdapter.isGrid() ? R.drawable.vod_ic_action_list : R.drawable.vod_ic_action_grid);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            if (mCollectAdapter.getPosition() == 0) mSearchAdapter.addAll(result.getList());
            mCollectAdapter.add(Collect.create(result.getList()));
            mCollectAdapter.add(result.getList());
        });
        mViewModel.result.observe(this, result -> {
            boolean same = !result.getList().isEmpty() && mCollectAdapter.getActivated().getSite().equals(result.getList().get(0).getSite());
            if (same) mCollectAdapter.getActivated().getList().addAll(result.getList());
            if (same) mSearchAdapter.addAll(result.getList());
            mScroller.endLoading(result);
        });
    }

    private void checkKeyword() {
        if (TextUtils.isEmpty(getKeyword())) Util.showKeyboard(mBinding.keyword);
        else setKeyword(getKeyword());
    }

    private void setKeyword(String text) {
        mBinding.keyword.setText(text);
        mBinding.keyword.setSelection(text.length());
    }

    private void setSite() {
        for (Site site : VodConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        Site home = VodConfig.get().getHome();
        if (!mSites.contains(home)) return;
        mSites.remove(home);
        mSites.add(0, home);
    }

    private void search() {
        if (empty()) return;
        mSearchAdapter.clear();
        mCollectAdapter.clear();
        Util.hideKeyboard(mBinding.keyword);
        mBinding.site.setVisibility(View.GONE);
        mBinding.agent.setVisibility(View.GONE);
        mBinding.view.setVisibility(View.VISIBLE);
        mBinding.result.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
        mExecutor = new PauseExecutor(20);
        String keyword = mBinding.keyword.getText().toString().trim();
        for (Site site : mSites) mExecutor.execute(() -> search(site, keyword));
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword, false);
        } catch (Throwable ignored) {
        }
    }

    private void getHot() {
        mBinding.word.setText(R.string.vod_search_hot);
        mWordAdapter.addAll(Hot.get(Setting.getHot()));
    }

    private void getSuggest(String text) {
        mBinding.word.setText(R.string.vod_search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + URLEncoder.encode(text)).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void onSite(View view) {
        Util.hideKeyboard(mBinding.keyword);
        App.post(() -> SiteDialog.create(this).search().show(), 50);
    }

    private void toggleView(View view) {
        setViewType(mSearchAdapter.isGrid() ? ViewType.LIST : ViewType.GRID);
    }

    private void showAgent() {
        mScroller.reset();
        mSearchAdapter.clear();
        mCollectAdapter.clear();
        mBinding.view.setVisibility(View.GONE);
        mBinding.result.setVisibility(View.GONE);
        mBinding.site.setVisibility(View.VISIBLE);
        mBinding.agent.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
    }

    @Override
    public void setSite(Site item) {
    }

    @Override
    public void onChanged() {
        mSites.clear();
        setSite();
    }

    @Override
    public void onItemClick(String text) {
        setKeyword(text);
        search();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.record.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.recordRecycler.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        App.post(() -> mBinding.recordRecycler.requestLayout(), 250);
    }

    @Override
    public void onItemClick(int position, Collect item) {
        mBinding.recycler.scrollToPosition(0);
        mCollectAdapter.setActivated(position);
        mSearchAdapter.setAll(item.getList());
        mScroller.setPage(item.getPage());
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) FolderActivity.start(this, item.getSiteKey(), Result.folder(item));
        else VideoActivity.collect(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }

    @Override
    public void onLoadMore(String page) {
        Collect activated = mCollectAdapter.getActivated();
        if ("all".equals(activated.getSite().getKey())) return;
        mViewModel.searchContent(activated.getSite(), mBinding.keyword.getText().toString(), page);
        activated.setPage(Integer.parseInt(page));
        mScroller.setLoading(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mExecutor != null) mExecutor.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mExecutor != null) mExecutor.pause();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.result)) {
            showAgent();
        } else {
            super.onBackPressed();
        }
    }
}
