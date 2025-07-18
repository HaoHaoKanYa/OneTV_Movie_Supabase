package top.cywin.onetv.vod.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.databinding.VodActivityFileBinding;
import top.cywin.onetv.vod.ui.base.BaseActivity;
import top.cywin.onetv.vod.ui.presenter.FilePresenter;
import top.cywin.onetv.vod.utils.ResUtil;
import top.github.catvod.utils.Path;
import com.permissionx.guolindev.PermissionX;

import java.io.File;

public class FileActivity extends BaseActivity implements FilePresenter.OnClickListener {

    private VodActivityFileBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private File dir;

    private boolean isRoot() {
        return Path.root().equals(dir);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = VodActivityFileBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        checkPermission();
    }

    private void checkPermission() {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) update(Path.root());
            else finish();
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new FilePresenter(this))));
    }

    private void update(File dir) {
        mBinding.recycler.setSelectedPosition(0);
        mAdapter.setItems(Path.list(this.dir = dir), null);
        mBinding.progressLayout.showContent(true, mAdapter.size());
    }

    @Override
    public void onItemClick(File file) {
        if (file.isDirectory()) {
            update(file);
        } else {
            setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRoot()) {
            super.onBackPressed();
        } else {
            update(dir.getParentFile());
        }
    }
}
