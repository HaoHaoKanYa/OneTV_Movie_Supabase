package top.cywin.onetv.vod.ui.custom;

import android.annotation.SuppressLint;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.databinding.VodActivitySearchBinding;
import top.cywin.onetv.vod.ui.adapter.KeyboardAdapter;

public class CustomKeyboard implements KeyboardAdapter.OnClickListener {

    private final VodActivitySearchBinding binding;
    private final Callback callback;
    private KeyboardAdapter adapter;

    public static void init(Callback callback, VodActivitySearchBinding binding) {
        new CustomKeyboard(callback, binding).initView();
    }

    public CustomKeyboard(Callback callback, VodActivitySearchBinding binding) {
        this.callback = callback;
        this.binding = binding;
    }

    private void initView() {
        binding.keyboard.setItemAnimator(null);
        binding.keyboard.setHasFixedSize(false);
        binding.keyboard.addItemDecoration(new SpaceItemDecoration(7, 8));
        binding.keyboard.setAdapter(adapter = new KeyboardAdapter(this));
    }

    @Override
    public void onTextClick(String text) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        if (binding.keyword.length() > 19) return;
        sb.insert(cursor, text);
        binding.keyword.setText(sb.toString());
        binding.keyword.setSelection(cursor + 1);
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onIconClick(int resId) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        if (resId == R.drawable.vod_ic_setting_home) {
            callback.showDialog();
        } else if (resId == R.drawable.vod_ic_keyboard_remote) {
            callback.onRemote();
        } else if (resId == R.drawable.vod_ic_keyboard_search) {
            callback.onSearch();
        } else if (resId == R.drawable.vod_ic_keyboard_left) {
            binding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
        } else if (resId == R.drawable.vod_ic_keyboard_right) {
            binding.keyword.setSelection(++cursor > binding.keyword.length() ? binding.keyword.length() : cursor);
        } else if (resId == R.drawable.vod_ic_keyboard_back) {
            if (cursor == 0) return;
            sb.deleteCharAt(cursor - 1);
            binding.keyword.setText(sb.toString());
            binding.keyword.setSelection(cursor - 1);
        } else if (resId == R.drawable.vod_ic_keyboard) {
            adapter.toggle();
        }
    }

    @Override
    public boolean onLongClick(int resId) {
        if (resId != R.drawable.vod_ic_keyboard_back) return false;
        binding.keyword.setText("");
        return true;
    }

    public interface Callback {

        void showDialog();

        void onRemote();

        void onSearch();
    }
}
