package com.jandergy.myjandergymusic;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkLoader {

    private static final int CACHE_SIZE_DIVISOR = 16;

    interface BitmapCallback {
        void onBitmapLoaded(@Nullable Bitmap bitmap);
    }

    interface PaletteTransformer {
        @Nullable
        int[] transform(@Nullable Palette palette);
    }

    interface ArtworkPaletteCallback {
        void onArtworkLoaded(@Nullable Bitmap bitmap, @Nullable int[] palette);
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<String, Bitmap>(Math.max(4096, (int) (Runtime.getRuntime().maxMemory() / 1024L / CACHE_SIZE_DIVISOR))) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };
    private static final LruCache<String, int[]> PALETTE_CACHE = new LruCache<>(64);

    private ArtworkLoader() {
    }

    @Nullable
    static Bitmap getCachedBitmap(@NonNull Uri uri, int size) {
        return BITMAP_CACHE.get(buildKey(uri, size));
    }

    static void loadBitmap(@NonNull ContentResolver resolver, @NonNull Uri uri, int size, @NonNull BitmapCallback callback) {
        final String key = buildKey(uri, size);
        Bitmap cachedBitmap = BITMAP_CACHE.get(key);
        if (cachedBitmap != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                callback.onBitmapLoaded(cachedBitmap);
            } else {
                MAIN_HANDLER.post(() -> callback.onBitmapLoaded(cachedBitmap));
            }
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeBitmap(resolver, uri, size);
            if (bitmap != null) {
                BITMAP_CACHE.put(key, bitmap);
            }
            Bitmap finalBitmap = bitmap;
            MAIN_HANDLER.post(() -> callback.onBitmapLoaded(finalBitmap));
        });
    }

    static void loadBitmapAndPalette(@NonNull ContentResolver resolver,
                                     @NonNull Uri uri,
                                     int size,
                                     @NonNull PaletteTransformer transformer,
                                     @NonNull ArtworkPaletteCallback callback) {
        final String key = buildKey(uri, size);
        Bitmap cachedBitmap = BITMAP_CACHE.get(key);
        int[] cachedPalette = PALETTE_CACHE.get(key);
        if (cachedBitmap != null && cachedPalette != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                callback.onArtworkLoaded(cachedBitmap, cachedPalette);
            } else {
                MAIN_HANDLER.post(() -> callback.onArtworkLoaded(cachedBitmap, cachedPalette));
            }
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = cachedBitmap != null ? cachedBitmap : decodeBitmap(resolver, uri, size);
            if (bitmap != null && cachedBitmap == null) {
                BITMAP_CACHE.put(key, bitmap);
            }

            int[] palette = cachedPalette;
            if (bitmap != null && palette == null) {
                palette = transformer.transform(Palette.from(bitmap).generate());
                if (palette != null) {
                    PALETTE_CACHE.put(key, palette);
                }
            }

            Bitmap finalBitmap = bitmap;
            int[] finalPalette = palette;
            MAIN_HANDLER.post(() -> callback.onArtworkLoaded(finalBitmap, finalPalette));
        });
    }

    @Nullable
    static Bitmap decodeBitmap(@NonNull ContentResolver resolver, @NonNull Uri uri, int size) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return resolver.loadThumbnail(uri, new Size(size, size), null);
            }

            try (InputStream inputStream = resolver.openInputStream(uri)) {
                if (inputStream == null) {
                    return null;
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeStream(inputStream, null, options);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private static String buildKey(@NonNull Uri uri, int size) {
        return uri.toString() + "#" + size;
    }
}
