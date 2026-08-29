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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MusicListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private ArtistFolderAdapter artistAdapter;
    private List<AudioAdapter.AudioItem> audioItems = new ArrayList<>();
    private List<ArtistFolderAdapter.ArtistGroup> artistGroups = new ArrayList<>();
    private AudioAdapter.OnItemClickListener listener;
    private boolean showingSongsInArtistTab = false;
    private Set<String> favoriteIds = new HashSet<>();

    public static MusicListFragment newInstance(int position) {
        MusicListFragment fragment = new MusicListFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putBoolean("isArtistTab", position == 1);
        fragment.setArguments(args);
        return fragment;
    }

    public int getPosition() {
        if (getArguments() != null) {
            return getArguments().getInt("position", 0);
        }
        return 0;
    }

    public boolean isArtistTab() {
        if (getArguments() != null) {
            return getArguments().getBoolean("isArtistTab", false);
        }
        return false;
    }

    public boolean handleBack() {
        if (isArtistTab() && showingSongsInArtistTab) {
            showingSongsInArtistTab = false;
            if (artistAdapter != null) {
                recyclerView.setAdapter(artistAdapter);
            }
            return true;
        }
        return false;
    }

    public void setOnItemClickListener(AudioAdapter.OnItemClickListener listener) {
        this.listener = listener;
        if (adapter != null) {
            adapter.setOnItemClickListener(listener);
        }
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
        recyclerView.setItemViewCacheSize(12);

        if (isArtistTab()) {
            artistAdapter = new ArtistFolderAdapter(artistGroups, (artistName, songs) -> showArtistSongs(songs));
            recyclerView.setAdapter(artistAdapter);
        } else {
            adapter = new AudioAdapter(audioItems, listener);
            adapter.setFavoriteIds(favoriteIds);
            recyclerView.setAdapter(adapter);
        }
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).populateFragment(this);
        }
    }

    private void showArtistSongs(List<AudioAdapter.AudioItem> songs) {
        showingSongsInArtistTab = true;
        adapter = new AudioAdapter(songs, listener);
        adapter.setFavoriteIds(favoriteIds);
        recyclerView.setAdapter(adapter);
    }

    public void setFavoriteIds(Set<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        if (adapter != null) {
            adapter.setFavoriteIds(favoriteIds);
        }
    }

    public void updateList(List<AudioAdapter.AudioItem> newList) {
        this.audioItems = newList != null ? newList : new ArrayList<>();
        if (isArtistTab()) {
            artistGroups = groupSongsByArtist(this.audioItems);
        }

        if (recyclerView == null) return; // Wait for onCreateView

        if (isArtistTab()) {
            if (!showingSongsInArtistTab) {
                if (artistAdapter != null) {
                    artistAdapter.updateData(artistGroups);
                } else {
                    artistAdapter = new ArtistFolderAdapter(artistGroups, (artistName, songs) -> showArtistSongs(songs));
                    recyclerView.setAdapter(artistAdapter);
                }
            }
        } else {
            if (adapter != null) {
                adapter.updateList(this.audioItems);
            } else {
                adapter = new AudioAdapter(this.audioItems, listener);
                adapter.setFavoriteIds(favoriteIds);
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private List<ArtistFolderAdapter.ArtistGroup> groupSongsByArtist(List<AudioAdapter.AudioItem> songs) {
        java.util.Map<String, List<AudioAdapter.AudioItem>> map = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (AudioAdapter.AudioItem item : songs) {
            String artist = FormatUtils.cleanArtist(item.artist);
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
        if (isArtistTab() && !showingSongsInArtistTab && artistAdapter != null) {
            artistAdapter.filter(query);
        } else if (adapter != null) {
            adapter.filter(query);
        }
    }
}