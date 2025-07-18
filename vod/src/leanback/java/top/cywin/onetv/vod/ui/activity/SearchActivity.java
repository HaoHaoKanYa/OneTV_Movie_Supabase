package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.bean.Hot;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.bean.Suggest;
import top.cywin.onetv.vod.databinding.VodActivitySearchBinding;
import top.cywin.onetv.vod.impl.Callback;
import top.cywin.onetv.vod.impl.SiteCallback;
import top.cywin.onetv.vod.ui.adapter.RecordAdapter;
import top.cywin.onetv.vod.ui.adapter.WordAdapter;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.custom.CustomKeyboard;
import top.cywin.onetv.vod.ui.custom.CustomTextListener;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.ui.dialog.SiteDialog;
import top.cywin.onetv.vod.utils.KeyUtil;
import top.cywin.onetv.vod.utils.Util;
import top.github.catvod.net.OkHttp;
import top.github.catvod.utils.ZhuToPin;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

public class SearchActivity extends BaseActivity implements WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CustomKeyboard.Callback, SiteCallback {

    private VodActivitySearchBinding mBinding;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SearchActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivitySearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        CustomKeyboard.init(this, mBinding);
        setRecyclerView();
        getHot();
    }

    @Override
    protected void initEvent() {
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onSearch();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
        mBinding.mic.setListener(this, new CustomTextListener() {
            @Override
            public void onEndOfSpeech() {
                mBinding.keyword.requestFocus();
                mBinding.mic.stop();
            }

            @Override
            public void onResults(String result) {
                mBinding.keyword.setText(result);
                mBinding.keyword.setSelection(mBinding.keyword.length());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.wordRecycler.setHasFixedSize(true);
        mBinding.wordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.recordRecycler.setHasFixedSize(true);
        mBinding.recordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
    }

    private void getHot() {
        mBinding.hint.setText(R.string.vod_search_hot);
        mWordAdapter.addAll(Hot.get(Setting.getHot()));
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1", Headers.of(HttpHeaders.REFERER, "https://www.360kan.com/rank/general")).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Hot.get(response.body().string());
                if (mWordAdapter.getItemCount() > 0) return;
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void getSuggest(String text) {
        mBinding.hint.setText(R.string.vod_search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + URLEncoder.encode(ZhuToPin.get(text))).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        onSearch();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.recordLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        mBinding.keyword.setSelection(mBinding.keyword.length());
        Util.hideKeyboard(mBinding.keyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).search().show();
    }

    @Override
    public void onRemote() {
        PushActivity.start(this, 1);
    }

    @Override
    public void setSite(Site item) {
    }

    @Override
    public void onChanged() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.keyword.requestFocus();
    }
}
