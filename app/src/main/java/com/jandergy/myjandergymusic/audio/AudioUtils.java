package com.jandergy.myjandergymusic.audio;

import android.util.Log;

public class AudioUtils {
    /**
     * Interpolates 10-band UI EQ levels to hardware bands.
     * @param uiLevels Array of 10 float levels in dB (-15.0 to 15.0)
     * @param hardwareBands Number of hardware bands available (usually 5)
     * @return Array of short levels for hardware bands in milliBel (100 * dB)
     */
    public static short[] interpolateBands(float[] uiLevels, int hardwareBands) {
        short[] result = new short[hardwareBands];
        if (uiLevels.length != 10) {
            Log.w("AudioUtils", "Expected 10 UI bands, got " + uiLevels.length);
            return result;
        }

        if (hardwareBands == 5) {
            // Simplified mapping for standard 5-band hardware
            result[0] = (short) ((uiLevels[0] * 0.3f + uiLevels[1] * 0.4f + uiLevels[2] * 0.3f) * 100);
            result[1] = (short) ((uiLevels[3] * 0.5f + uiLevels[4] * 0.5f) * 100);
            result[2] = (short) ((uiLevels[5] * 0.5f + uiLevels[6] * 0.5f) * 100);
            result[3] = (short) ((uiLevels[7] * 0.5f + uiLevels[8] * 0.5f) * 100);
            result[4] = (short) (uiLevels[9] * 100);
        } else {
            // Linear interpolation fallback for other band counts
            float ratio = (uiLevels.length - 1) / (float) (hardwareBands - 1);
            for (int i = 0; i < hardwareBands; i++) {
                float srcPos = i * ratio;
                int low = (int) srcPos;
                int high = Math.min(low + 1, uiLevels.length - 1);
                float weight = srcPos - low;
                result[i] = (short) ((uiLevels[low] * (1 - weight) + uiLevels[high] * weight) * 100);
            }
        }
        return result;
    }
}
