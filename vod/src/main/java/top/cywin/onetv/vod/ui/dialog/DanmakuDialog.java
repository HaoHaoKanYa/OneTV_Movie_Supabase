
package top.cywin.onetv.vod.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.bean.Danmaku;
import top.cywin.onetv.vod.databinding.VodDialogDanmakuBinding;
import top.cywin.onetv.vod.player.Players;
import top.cywin.onetv.vod.ui.adapter.DanmakuAdapter;
import top.cywin.onetv.vod.ui.custom.SpaceItemDecoration;
import top.cywin.onetv.vod.utils.FileChooser;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class DanmakuDialog extends BaseDialog implements DanmakuAdapter.OnClickListener {

    private final DanmakuAdapter adapter;
    private VodDialogDanmakuBinding binding;
    private Players player;

    public static DanmakuDialog create() {
        return new DanmakuDialog();
    }

    public DanmakuDialog() {
        this.adapter = new DanmakuAdapter(this);
    }

    public DanmakuDialog player(Players player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = VodDialogDanmakuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(player.getDanmakus()));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(adapter.getSelected()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void initEvent() {
        binding.choose.setOnClickListener(this::showChooser);
    }

    private void showChooser(View view) {
        FileChooser.from(this).show(new String[]{"text/*"});
        player.pause();
    }

    @Override
    public void onItemClick(Danmaku item) {
        player.setDanmaku(item.isSelected() ? Danmaku.empty() : item);
        dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        player.setDanmaku(Danmaku.from(FileChooser.getPathFromUri(data.getData())));
        dismiss();
    }
}