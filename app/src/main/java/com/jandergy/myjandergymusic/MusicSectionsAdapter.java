package com.jandergy.myjandergymusic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MusicSectionsAdapter extends FragmentStateAdapter {

    private final String[] titles = {"All", "Artist", "Recent", "Favorites"};

    public MusicSectionsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return MusicListFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public String getTitle(int position) {
        return titles[position];
    }
}