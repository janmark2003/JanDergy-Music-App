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
    private List<AudioAdapter.AudioItem> audioItems = new ArrayList<>();
    private AudioAdapter.OnItemClickListener listener;

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
        adapter = new AudioAdapter(audioItems, listener);
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    public void updateList(List<AudioAdapter.AudioItem> newList) {
        this.audioItems = newList;
        if (adapter != null) {
            adapter.updateList(newList);
        }
    }

    public void filter(String query) {
        if (adapter != null) {
            adapter.filter(query);
        }
    }
}