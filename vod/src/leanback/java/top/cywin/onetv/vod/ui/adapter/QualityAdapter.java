package top.cywin.onetv.vod.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import top.cywin.onetv.vod.bean.Result;
import top.cywin.onetv.vod.databinding.VodAdapterQualityBinding;

public class QualityAdapter extends RecyclerView.Adapter<QualityAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private Result mResult;
    private int position;

    public QualityAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mResult = Result.empty();
    }

    public interface OnClickListener {

        void onItemClick(Result result);
    }

    public int getPosition() {
        return position;
    }

    public void addAll(Result result) {
        mResult = result;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mResult.getUrl().getValues().size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(VodAdapterQualityBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.text.setText(mResult.getUrl().n(position));
        holder.binding.text.setOnClickListener(v -> onItemClick(position));
        holder.binding.text.setActivated(mResult.getUrl().getPosition() == position);
    }

    private void onItemClick(int position) {
        this.position = position;
        mResult.getUrl().set(position);
        mListener.onItemClick(mResult);
        notifyItemRangeChanged(0, getItemCount());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final VodAdapterQualityBinding binding;

        ViewHolder(@NonNull VodAdapterQualityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}