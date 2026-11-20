package com.jandergy.myjandergymusic;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Per-song DSP & A-B Loop profile store backed by SharedPreferences.
 * Remembers each track's custom speed, keep-pitch mode, vibe mode, vocal/karaoke mode,
 * bass boost strength, and A-B loop timestamps.
 */
public final class SongDspMemoryStore {

    private static final String PREFS_NAME = "SongDspMemoryPrefs";
    private static final String KEY_PROFILES_JSON = "profiles_v1";
    private static final int MAX_STORED_SONGS = 300;

    public static final class Profile {
        public float speed = 1.0f;
        public boolean keepPitch = true;
        public int vibeMode = 0;
        public int vocalMode = 0;
        public int bassBoost = 0;
        public long abLoopA = -1L;
        public long abLoopB = -1L;
        public boolean abLoopActive = false;
        public long updatedAt = 0L;

        public boolean isDefault() {
            return Math.abs(speed - 1.0f) < 0.01f
                    && keepPitch
                    && vibeMode == 0
                    && vocalMode == 0
                    && bassBoost == 0
                    && !abLoopActive;
        }
    }

    private SongDspMemoryStore() {
    }

    @Nullable
    public static synchronized Profile load(@NonNull Context context, @Nullable String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) return null;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROFILES_JSON, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject obj = root.optJSONObject(mediaId);
            if (obj == null) return null;
            Profile p = new Profile();
            p.speed = (float) obj.optDouble("speed", 1.0);
            if (Float.isNaN(p.speed) || p.speed < 0.5f || p.speed > 2.0f) p.speed = 1.0f;
            p.keepPitch = obj.optBoolean("keepPitch", true);
            p.vibeMode = obj.optInt("vibeMode", 0);
            p.vocalMode = obj.optInt("vocalMode", 0);
            p.bassBoost = obj.optInt("bassBoost", 0);
            p.abLoopA = obj.optLong("abLoopA", -1L);
            p.abLoopB = obj.optLong("abLoopB", -1L);
            p.abLoopActive = obj.optBoolean("abLoopActive", false);
            p.updatedAt = obj.optLong("updatedAt", 0L);
            return p;
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static synchronized void save(@NonNull Context context, @Nullable String mediaId, @NonNull Profile profile) {
        if (mediaId == null || mediaId.isEmpty()) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROFILES_JSON, null);
        JSONObject root;
        try {
            root = (raw != null && !raw.isEmpty()) ? new JSONObject(raw) : new JSONObject();
        } catch (JSONException ignored) {
            root = new JSONObject();
        }

        try {
            if (profile.isDefault()) {
                root.remove(mediaId);
            } else {
                JSONObject obj = new JSONObject();
                obj.put("speed", profile.speed);
                obj.put("keepPitch", profile.keepPitch);
                obj.put("vibeMode", profile.vibeMode);
                obj.put("vocalMode", profile.vocalMode);
                obj.put("bassBoost", profile.bassBoost);
                obj.put("abLoopA", profile.abLoopA);
                obj.put("abLoopB", profile.abLoopB);
                obj.put("abLoopActive", profile.abLoopActive);
                obj.put("updatedAt", System.currentTimeMillis());
                root.put(mediaId, obj);
                pruneOldestIfNeeded(root);
            }
            prefs.edit().putString(KEY_PROFILES_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private static void pruneOldestIfNeeded(JSONObject root) {
        while (root.length() > MAX_STORED_SONGS) {
            String oldestKey = null;
            long oldestTs = Long.MAX_VALUE;
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject o = root.optJSONObject(k);
                long ts = o != null ? o.optLong("updatedAt", 0L) : 0L;
                if (ts < oldestTs) {
                    oldestTs = ts;
                    oldestKey = k;
                }
            }
            if (oldestKey != null) {
                root.remove(oldestKey);
            } else {
                break;
            }
        }
    }
}
