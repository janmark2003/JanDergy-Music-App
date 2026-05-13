package com.jandergy.myjandergymusic;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AudioItem item);
        void onFavoriteClick(AudioItem item);
        void onAlbumArtClick(AudioItem item, View albumArtView);
    }

    private List<AudioItem> items;
    private List<AudioItem> filteredItems;
    private final OnItemClickListener listener;

    public AudioAdapter(List<AudioItem> items, OnItemClickListener listener) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
    }

    public void updateList(List<AudioItem> newList) {
        this.items = newList;
        this.filteredItems = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(items);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (AudioItem item : items) {
                if (item.title.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    item.artist.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioItem item = filteredItems.get(position);
        holder.titleView.setText(item.title);
        holder.artistView.setText(item.artist);
        holder.descView.setText(formatDuration(item.duration));
        
        holder.favBtn.setImageResource(item.isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        
        // Load Album Art
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = holder.itemView.getContext().getContentResolver();
                Bitmap thumbnail = resolver.loadThumbnail(item.uri, new Size(200, 200), null);
                holder.albumArt.setImageBitmap(thumbnail);
            } catch (Exception e) {
                holder.albumArt.setImageResource(R.drawable.blank_icon_album);
            }
        } else {
            holder.albumArt.setImageResource(R.drawable.blank_icon_album);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.favBtn.setOnClickListener(v -> listener.onFavoriteClick(item));
        holder.albumArt.setOnClickListener(v -> listener.onAlbumArtClick(item, holder.albumArt));
    }

    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView titleView, artistView, descView;
        ImageButton favBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.album_art);
            titleView = itemView.findViewById(R.id.song_title);
            artistView = itemView.findViewById(R.id.artist_name);
            descView = itemView.findViewById(R.id.file_description);
            favBtn = itemView.findViewById(R.id.btn_favorite);
        }
    }

    public static class AudioItem {
        long id;
        Uri uri;
        String title;
        String artist;
        String folderName;
        long duration;
        long dateAdded;
        boolean isFavorite;

        public AudioItem(long id, Uri uri, String title, String artist, String folderName, long duration, long dateAdded) {
            this.id = id;
            this.uri = uri;
            this.title = title;
            this.artist = artist;
            this.folderName = folderName;
            this.duration = duration;
            this.dateAdded = dateAdded;
            this.isFavorite = false;
        }
    }
}
