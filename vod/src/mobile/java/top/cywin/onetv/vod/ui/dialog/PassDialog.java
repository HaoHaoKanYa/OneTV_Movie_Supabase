package top.cywin.onetv.vod.ui.dialog;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import top.cywin.onetv.vod.databinding.VodDialogPassBinding;
import top.cywin.onetv.vod.impl.PassCallback;
import top.cywin.onetv.vod.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PassDialog extends BaseDialog {

    private VodDialogPassBinding binding;
    private PassCallback callback;

    public static PassDialog create() {
        return new PassDialog();
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        this.callback = (PassCallback) activity;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = VodDialogPassBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initEvent() {
        binding.pass.setOnEditorActionListener(this::onDone);
    }

    private void onPass() {
        String pass = binding.pass.getText().toString().trim();
        if (pass.length() > 0) callback.setPass(pass);
        dismiss();
    }

    private boolean onDone(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) onPass();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ResUtil.dp2px(250), -1);
    }
}
