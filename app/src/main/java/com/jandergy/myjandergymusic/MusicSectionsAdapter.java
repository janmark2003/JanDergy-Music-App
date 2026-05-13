package com.jandergy.myjandergymusic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class MusicSectionsAdapter extends FragmentStateAdapter {

    private final List<MusicListFragment> fragments = new ArrayList<>();
    private final String[] titles = {"All", "Artists", "Recent", "Favorites"};

    public MusicSectionsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        for (int i = 0; i < titles.length; i++) {
            fragments.add(null);
        }
    }

    public void setFragment(int position, MusicListFragment fragment) {
        fragments.set(position, fragment);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MusicListFragment fragment = fragments.get(position);
        if (fragment == null) {
            // This should ideally not happen as we set them in setupFragments
            return new MusicListFragment();
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public String getTitle(int position) {
        return titles[position];
    }
}
