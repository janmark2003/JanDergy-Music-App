package com.jandergy.myjandergymusic;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
    private String currentQuery = "";

    public AudioAdapter(List<AudioItem> items, OnItemClickListener listener) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateList(List<AudioItem> newList) {
        this.items = newList;
        applyFilter(currentQuery);
    }

    public void filter(String query) {
        currentQuery = query == null ? "" : query.trim();
        applyFilter(currentQuery);
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
        
        holder.albumArt.setTag(item.id);
        holder.albumArt.setImageResource(R.drawable.blank_icon_album);
        ArtworkLoader.loadBitmap(holder.itemView.getContext().getContentResolver(), item.uri, 160, bitmap -> {
            Object tag = holder.albumArt.getTag();
            if (!(tag instanceof Long) || !tag.equals(item.id)) {
                return;
            }
            if (bitmap != null) {
                holder.albumArt.setImageBitmap(bitmap);
            } else {
                holder.albumArt.setImageResource(R.drawable.blank_icon_album);
            }
        });

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

    @Override
    public long getItemId(int position) {
        return filteredItems.get(position).id;
    }

    private void applyFilter(String query) {
        List<AudioItem> nextFilteredItems = new ArrayList<>();
        if (query.isEmpty()) {
            nextFilteredItems.addAll(items);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (AudioItem item : items) {
                if (item.title.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)
                        || item.artist.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    nextFilteredItems.add(item);
                }
            }
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return filteredItems.size();
            }

            @Override
            public int getNewListSize() {
                return nextFilteredItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return filteredItems.get(oldItemPosition).id == nextFilteredItems.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                AudioItem oldItem = filteredItems.get(oldItemPosition);
                AudioItem newItem = nextFilteredItems.get(newItemPosition);
                return oldItem.id == newItem.id
                        && oldItem.duration == newItem.duration
                        && oldItem.dateAdded == newItem.dateAdded
                        && oldItem.isFavorite == newItem.isFavorite
                        && oldItem.title.equals(newItem.title)
                        && oldItem.artist.equals(newItem.artist)
                        && oldItem.folderName.equals(newItem.folderName)
                        && oldItem.uri.equals(newItem.uri);
            }
        });

        filteredItems = nextFilteredItems;
        diffResult.dispatchUpdatesTo(this);
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
