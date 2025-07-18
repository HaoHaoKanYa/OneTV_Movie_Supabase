package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod
import top.cywin.onetv.vod.bean.Device;
import top.cywin.onetv.vod.databinding.VodAdapterDeviceBinding;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Device> mItems;

    public DeviceAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Device item);

        boolean onLongClick(Device item);
    }

    public void addAll(List<Device> items) {
        if (items == null) return;
        mItems.removeAll(items);
        mItems.addAll(items);
        Device.Sorter.sort(mItems);
        notifyDataSetChanged();
    }

    public void remove(Device item) {
        if (item == null) return;
        mItems.remove(item);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        Device.delete();
        notifyDataSetChanged();
    }

    public List<String> getIps() {
        List<String> ips = new ArrayList<>();
        for (Device item : mItems) if (item.isApp()) ips.add(item.getIp());
        return ips;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.host.setText(item.getHost());
        holder.binding.type.setImageResource(getIcon(item));
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(v -> mListener.onLongClick(item));
    }

    private int getIcon(Device item) {
        return item.isMobile() ? R.drawable.vod_ic_cast_mobile : R.drawable.vod_ic_cast_tv;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterDeviceBinding binding;

        ViewHolder(@NonNull VodAdapterDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
