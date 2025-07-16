package top.cywin.onetv.vod.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.bean.Class;
import top.cywin.onetv.vod.bean.Result;
import top.cywin.onetv.vod.databinding.VodActivityFolderBinding;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.fragment.TypeFragment;

import java.util.HashMap;

public class FolderActivity extends BaseActivity {

    private VodActivityFolderBinding mBinding;

    public static void start(Activity activity, String key, Result result) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, FolderActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        activity.startActivity(intent);
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return getIntent().getParcelableExtra("result");
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityFolderBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Result result = getResult();
        Class type = result.getTypes().get(0);
        mBinding.text.setText(type.getTypeName());
        getSupportFragmentManager().beginTransaction().replace(R.id.container, TypeFragment.newInstance(getKey(), type.getTypeId(), type.getStyle(), new HashMap<>(), "1".equals(type.getTypeFlag())), "0").commitAllowingStateLoss();
    }

    private TypeFragment getFragment() {
        return (TypeFragment) getSupportFragmentManager().findFragmentByTag("0");
    }

    @Override
    public void onBackPressed() {
        if (getFragment().canBack()) super.onBackPressed();
    }
}
