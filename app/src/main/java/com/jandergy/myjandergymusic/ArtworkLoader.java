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
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<String, Bitmap>(
            Math.max(4096, (int) (Runtime.getRuntime().maxMemory() / 1024L / CACHE_SIZE_DIVISOR))) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };
    private static final LruCache<String, int[]> PALETTE_CACHE = new LruCache<>(64);

    private ArtworkLoader() {
    }

    @Nullable
    static Bitmap getCachedBitmap(@NonNull Uri uri, int size) {
        Bitmap cached = BITMAP_CACHE.get(buildKey(uri, size));
        if (cached != null && cached.isRecycled()) {
            BITMAP_CACHE.remove(buildKey(uri, size));
            return null;
        }
        return cached;
    }

    static void loadBitmap(@NonNull ContentResolver resolver, @NonNull Uri uri, int size, @NonNull BitmapCallback callback) {
        final String key = buildKey(uri, size);
        Bitmap cachedBitmap = getCachedBitmap(uri, size);
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
            if (bitmap != null && !bitmap.isRecycled()) {
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
        Bitmap cachedBitmap = getCachedBitmap(uri, size);
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
            if (bitmap != null && !bitmap.isRecycled() && cachedBitmap == null) {
                BITMAP_CACHE.put(key, bitmap);
            }

            int[] palette = cachedPalette;
            if (bitmap != null && !bitmap.isRecycled() && palette == null) {
                try {
                    palette = transformer.transform(Palette.from(bitmap).generate());
                    if (palette != null) {
                        PALETTE_CACHE.put(key, palette);
                    }
                } catch (Throwable ignored) {
                    palette = null;
                }
            }

            Bitmap finalBitmap = (bitmap != null && !bitmap.isRecycled()) ? bitmap : null;
            int[] finalPalette = palette;
            MAIN_HANDLER.post(() -> callback.onArtworkLoaded(finalBitmap, finalPalette));
        });
    }

    @Nullable
    static Bitmap decodeBitmap(@NonNull ContentResolver resolver, @NonNull Uri uri, int size) {
        int safeSize = Math.max(64, size);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return resolver.loadThumbnail(uri, new Size(safeSize, safeSize), null);
            }

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream boundsStream = resolver.openInputStream(uri)) {
                if (boundsStream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, safeSize, safeSize);
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream inputStream = resolver.openInputStream(uri)) {
                if (inputStream == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(inputStream, null, decodeOptions);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @NonNull
    private static String buildKey(@NonNull Uri uri, int size) {
        return uri.toString() + "#" + size;
    }
}
