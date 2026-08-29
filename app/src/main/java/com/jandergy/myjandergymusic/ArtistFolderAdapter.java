package com.jandergy.myjandergymusic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtistFolderAdapter extends RecyclerView.Adapter<ArtistFolderAdapter.ViewHolder> {

    public interface OnArtistClickListener {
        void onArtistClick(String artistName, List<AudioAdapter.AudioItem> songs);
    }

    private final List<ArtistGroup> artistGroups;
    private List<ArtistGroup> filteredGroups;
    private final OnArtistClickListener listener;
    private String currentQuery = "";

    public String getCurrentQuery() {
        return currentQuery;
    }

    public ArtistFolderAdapter(List<ArtistGroup> artistGroups, OnArtistClickListener listener) {
        this.artistGroups = new ArrayList<>(artistGroups);
        this.filteredGroups = new ArrayList<>(artistGroups);
        this.listener = listener;
    }

    public void updateData(List<ArtistGroup> newGroups) {
        this.artistGroups.clear();
        this.artistGroups.addAll(newGroups);
        filter(currentQuery); // Re-apply current search/filter
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArtistGroup group = filteredGroups.get(position);
        holder.artistName.setText(group.artistName);
        holder.songCount.setText(String.format(Locale.getDefault(), "%d songs", group.songs.size()));
        holder.itemView.setOnClickListener(v -> listener.onArtistClick(group.artistName, group.songs));
    }

    @Override
    public int getItemCount() {
        return filteredGroups.size();
    }

    public void filter(String query) {
        currentQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        filteredGroups = new ArrayList<>();
        if (currentQuery.isEmpty()) {
            filteredGroups.addAll(artistGroups);
        } else {
            for (ArtistGroup group : artistGroups) {
                if (group.artistName.toLowerCase(Locale.getDefault()).contains(currentQuery)) {
                    filteredGroups.add(group);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView artistName, songCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artist_name);
            songCount = itemView.findViewById(R.id.song_count);
        }
    }

    public static class ArtistGroup {
        String artistName;
        List<AudioAdapter.AudioItem> songs;

        public ArtistGroup(String artistName, List<AudioAdapter.AudioItem> songs) {
            this.artistName = artistName;
            this.songs = songs;
        }
    }
}
