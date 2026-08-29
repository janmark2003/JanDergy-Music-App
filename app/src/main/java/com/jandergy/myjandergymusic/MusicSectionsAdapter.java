package com.jandergy.myjandergymusic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MusicSectionsAdapter extends FragmentStateAdapter {

    private final MusicListFragment[] fragments = new MusicListFragment[4];
    private final String[] titles = {"All", "Artist", "Recent", "Favorites"};

    public MusicSectionsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        // Instantiated immediately so they always match what ViewPager2 renders
        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = new MusicListFragment();
            if (i == 1) { // Artist tab
                fragments[i].setArtistTab(true);
            }
        }
    }

    public MusicListFragment getFragment(int position) {
        return fragments[position];
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public String getTitle(int position) {
        return titles[position];
    }
}