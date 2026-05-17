package com.jandergy.myjandergymusic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MusicListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private ArtistFolderAdapter artistAdapter;
    private List<AudioAdapter.AudioItem> audioItems = new ArrayList<>();
    private List<ArtistFolderAdapter.ArtistGroup> artistGroups = new ArrayList<>();
    private AudioAdapter.OnItemClickListener listener;
    private boolean isArtistTab = false;
    private boolean showingSongsInArtistTab = false;

    public void setArtistTab(boolean isArtistTab) {
        this.isArtistTab = isArtistTab;
    }

    public boolean handleBack() {
        if (isArtistTab && showingSongsInArtistTab) {
            showingSongsInArtistTab = false;
            if (artistAdapter != null) {
                recyclerView.setAdapter(artistAdapter);
            }
            return true;
        }
        return false;
    }

    // Added setter to wire up clicks on early-allocated instances
    public void setOnItemClickListener(AudioAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        int verticalPadding = (int) (12 * requireContext().getResources().getDisplayMetrics().density);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, verticalPadding, 0, verticalPadding * 2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        if (isArtistTab) {
            artistAdapter = new ArtistFolderAdapter(artistGroups, (artistName, songs) -> {
                // When an artist folder is clicked, we show the songs for that artist
                // For now, we can just play the first song or show a sub-list
                // If the user wants a sub-list, we would need to navigate.
                // But the user said "folder by folder", so we show the artist list first.
                // Let's implement a simple way to "enter" the folder.
                showArtistSongs(songs);
            });
            recyclerView.setAdapter(artistAdapter);
        } else {
            adapter = new AudioAdapter(audioItems, listener);
            recyclerView.setAdapter(adapter);
        }
        return recyclerView;
    }

    private void showArtistSongs(List<AudioAdapter.AudioItem> songs) {
        // Switch from artistAdapter to regular adapter to show songs
        showingSongsInArtistTab = true;
        adapter = new AudioAdapter(songs, listener);
        recyclerView.setAdapter(adapter);
    }

    public void updateList(List<AudioAdapter.AudioItem> newList) {
        this.audioItems = newList;
        if (isArtistTab) {
            // Group by artist
            artistGroups = groupSongsByArtist(newList);
            if (artistAdapter != null && !showingSongsInArtistTab) {
                artistAdapter = new ArtistFolderAdapter(artistGroups, (artistName, songs) -> showArtistSongs(songs));
                recyclerView.setAdapter(artistAdapter);
            }
        } else {
            if (adapter != null) {
                adapter.updateList(newList);
            }
        }
    }

    private List<ArtistFolderAdapter.ArtistGroup> groupSongsByArtist(List<AudioAdapter.AudioItem> songs) {
        java.util.Map<String, List<AudioAdapter.AudioItem>> map = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (AudioAdapter.AudioItem item : songs) {
            String artist = item.artist != null ? item.artist : "Unknown Artist";
            if (!map.containsKey(artist)) {
                map.put(artist, new ArrayList<>());
            }
            map.get(artist).add(item);
        }
        List<ArtistFolderAdapter.ArtistGroup> groups = new ArrayList<>();
        for (java.util.Map.Entry<String, List<AudioAdapter.AudioItem>> entry : map.entrySet()) {
            groups.add(new ArtistFolderAdapter.ArtistGroup(entry.getKey(), entry.getValue()));
        }
        return groups;
    }

    public void filter(String query) {
        if (isArtistTab && !showingSongsInArtistTab && artistAdapter != null) {
            artistAdapter.filter(query);
        } else if (adapter != null) {
            adapter.filter(query);
        }
    }
}