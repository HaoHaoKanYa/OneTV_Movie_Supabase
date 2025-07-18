package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.api.config.VodConfig;
import top.cywin.onetv.vod.bean.Site;
import top.cywin.onetv.vod.databinding.VodAdapterSiteBinding;

import java.util.ArrayList;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Site> mItems;
    private boolean search;
    private boolean change;

    public SiteAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.addAll();
    }

    public SiteAdapter search(boolean search) {
        this.search = search;
        return this;
    }

    public SiteAdapter change(boolean change) {
        this.change = change;
        return this;
    }

    private void addAll() {
        for (Site site : VodConfig.get().getSites()) if (!site.isHide()) mItems.add(site);
    }

    public List<Site> getItems() {
        return mItems;
    }

    public interface OnClickListener {

        void onTextClick(Site item);

        void onSearchClick(int position, Site item);

        void onChangeClick(int position, Site item);

        boolean onSearchLongClick(Site item);

        boolean onChangeLongClick(Site item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        boolean on = !search || change;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setEnabled(on);
        holder.binding.text.setFocusable(on);
        holder.binding.text.setSelected(on && item.isActivated());
        holder.binding.text.setActivated(on && item.isActivated());
        holder.binding.search.setImageResource(getSearchIcon(item));
        holder.binding.change.setImageResource(getChangeIcon(item));
        holder.binding.search.setVisibility(search ? View.VISIBLE : View.GONE);
        holder.binding.change.setVisibility(change ? View.VISIBLE : View.GONE);
        holder.binding.text.setOnClickListener(v -> mListener.onTextClick(item));
        holder.binding.search.setOnClickListener(v -> mListener.onSearchClick(position, item));
        holder.binding.change.setOnClickListener(v -> mListener.onChangeClick(position, item));
        holder.binding.search.setOnLongClickListener(v -> mListener.onSearchLongClick(item));
        holder.binding.change.setOnLongClickListener(v -> mListener.onChangeLongClick(item));
    }

    private int getSearchIcon(Site item) {
        return item.isSearchable() ? R.drawable.vod_ic_site_search : R.drawable.vod_ic_site_block;
    }

    private int getChangeIcon(Site item) {
        return item.isChangeable() ? R.drawable.vod_ic_site_change : R.drawable.vod_ic_site_block;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterSiteBinding binding;

        ViewHolder(@NonNull VodAdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
