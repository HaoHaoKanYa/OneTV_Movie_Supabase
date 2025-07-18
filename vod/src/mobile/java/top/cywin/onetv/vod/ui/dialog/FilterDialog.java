package top.cywin.onetv.vod.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.bean.Filter;
import top.cywin.onetv.vod.databinding.VodDialogFilterBinding;
import top.cywin.onetv.vod.impl.FilterCallback;
import top.cywin.onetv.vod.ui.adapter.FilterAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class FilterDialog extends BaseDialog {

    private VodDialogFilterBinding binding;
    private FilterCallback callback;
    private List<Filter> filter;

    public static FilterDialog create() {
        return new FilterDialog();
    }

    public FilterDialog filter(List<Filter> filter) {
        this.filter = filter;
        return this;
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
        this.callback = (FilterCallback) fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = VodDialogFilterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setAdapter(new FilterAdapter(callback, filter));
        binding.recycler.setHasFixedSize(true);
    }
}
