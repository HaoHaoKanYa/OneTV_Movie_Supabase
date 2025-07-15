package top.cywin.onetv.vod.ui.dialog;

import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import top.cywin.onetv.vod.bean.Episode;
import top.cywin.onetv.vod.databinding.DialogEpisodeListBinding;
import top.cywin.onetv.vod.model.SiteViewModel;
import top.cywin.onetv.vod.ui.adapter.EpisodeAdapter;
import top.cywin.onetv.vod.ui.base.ViewType;
import com.google.android.material.sidesheet.SideSheetDialog;

import java.util.List;

public class EpisodeListDialog implements EpisodeAdapter.OnClickListener {

    private final FragmentActivity activity;
    private DialogEpisodeListBinding binding;
    private List<Episode> episodes;
    private SiteViewModel viewModel;
    private EpisodeAdapter adapter;
    private SideSheetDialog dialog;

    public static EpisodeListDialog create(FragmentActivity activity) {
        return new EpisodeListDialog(activity);
    }

    public EpisodeListDialog(FragmentActivity activity) {
        this.activity = activity;
    }

    public EpisodeListDialog episodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public SideSheetDialog show() {
        initDialog();
        initView();
        return dialog;
    }

    private void initDialog() {
        binding = DialogEpisodeListBinding.inflate(LayoutInflater.from(activity));
        dialog = new SideSheetDialog(activity);
        dialog.setContentView(binding.getRoot());
        dialog.getBehavior().setDraggable(false);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.show();
    }

    private void initView() {
        setRecyclerView();
        setViewModel();
        setEpisode();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new EpisodeAdapter(this, ViewType.GRID));
    }

    private void setViewModel() {
        viewModel = new ViewModelProvider(activity).get(SiteViewModel.class);
    }

    private void setEpisode() {
        adapter.addAll(episodes);
        binding.recycler.scrollToPosition(adapter.getPosition());
    }

    @Override
    public void onItemClick(Episode item) {
        viewModel.setEpisode(item);
    }
}
