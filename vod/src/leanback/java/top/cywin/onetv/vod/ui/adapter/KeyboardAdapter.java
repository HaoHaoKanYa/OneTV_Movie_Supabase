package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.Setting;
import top.cywin.onetv.vod.databinding.VodAdapterKeyboardIconBinding;
import top.cywin.onetv.vod.databinding.VodAdapterKeyboardTextBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class KeyboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Integer> icons = Arrays.asList(R.drawable.vod_ic_keyboard_remote, R.drawable.vod_ic_keyboard_left, R.drawable.vod_ic_keyboard_right, R.drawable.vod_ic_keyboard_back, R.drawable.vod_ic_keyboard_search, R.drawable.vod_ic_keyboard, R.drawable.vod_ic_setting_home);
    private final List<String> enList = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private final List<String> twList = Arrays.asList("↩", "ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ", "ㄧ", "ㄨ", "ㄩ", "ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private final OnClickListener mListener;
    private final List<Object> mItems;

    public KeyboardAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        this.mItems.addAll(icons);
        this.mItems.addAll(Setting.isZhuyin() ? twList : enList);
    }

    public interface OnClickListener {

        void onTextClick(String text);

        void onIconClick(int resId);

        boolean onLongClick(int resId);
    }

    public void toggle() {
        Setting.putZhuyin(!Setting.isZhuyin());
        mItems.removeAll(Setting.isZhuyin() ? enList : twList);
        mItems.addAll(icons.size(), Setting.isZhuyin() ? twList : enList);
        notifyItemRangeRemoved(icons.size(), Setting.isZhuyin() ? enList.size() : twList.size());
        notifyItemRangeInserted(icons.size(), Setting.isZhuyin() ? twList.size() : enList.size());
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) return new TextHolder(VodAdapterKeyboardTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        else return new IconHolder(VodAdapterKeyboardIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case 0:
                TextHolder text = (TextHolder) holder;
                text.binding.text.setText(mItems.get(position).toString());
                break;
            case 1:
                IconHolder icon = (IconHolder) holder;
                icon.binding.icon.setImageResource((int) mItems.get(position));
                break;
        }
    }

    class TextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final VodAdapterKeyboardTextBinding binding;

        TextHolder(@NonNull VodAdapterKeyboardTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onTextClick(mItems.get(getLayoutPosition()).toString());
        }
    }

    class IconHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final VodAdapterKeyboardIconBinding binding;

        IconHolder(@NonNull VodAdapterKeyboardIconBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onIconClick((int) mItems.get(getLayoutPosition()));
        }

        @Override
        public boolean onLongClick(View view) {
            return mListener.onLongClick((int) mItems.get(getLayoutPosition()));
        }
    }
}
