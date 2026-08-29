package com.jandergy.myjandergymusic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FlyerPagerAdapter extends RecyclerView.Adapter<FlyerPagerAdapter.ViewHolder> {

    private final List<Integer> imageResList;
    private final View.OnClickListener onClickListener;

    public FlyerPagerAdapter(List<Integer> imageResList, View.OnClickListener onClickListener) {
        this.imageResList = imageResList;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flyer_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(imageResList.get(position));
        if (onClickListener != null) {
            holder.itemView.setOnClickListener(onClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return imageResList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.flyer_image);
        }
    }
}
