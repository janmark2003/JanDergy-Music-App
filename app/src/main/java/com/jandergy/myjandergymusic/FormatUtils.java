package com.jandergy.myjandergymusic;

import java.io.File;

public class FormatUtils {

    public static String cleanArtist(String artist) {
        if (artist == null) {
            return "Local Artist";
        }
        String trimmed = artist.trim();
        if (trimmed.isEmpty()) {
            return "Local Artist";
        }
        String lower = trimmed.toLowerCase();
        if (lower.equals("<unknown>") || lower.equals("unknown")
                || lower.equals("<unknown artist>") || lower.equals("unknown artist")
                || lower.equals("undefined") || lower.equals("<undefined>")
                || lower.equals("null")) {
            return "Local Artist";
        }
        return trimmed;
    }

    public static String cleanTitle(String title, String dataPath) {
        if (title != null) {
            String trimmed = title.trim();
            String lower = trimmed.toLowerCase();
            if (!trimmed.isEmpty()
                    && !lower.equals("<unknown>") && !lower.equals("unknown")
                    && !lower.equals("<unknown title>") && !lower.equals("unknown title")
                    && !lower.equals("undefined") && !lower.equals("<undefined>")
                    && !lower.equals("null")) {
                return trimmed;
            }
        }
        if (dataPath != null && !dataPath.trim().isEmpty()) {
            File file = new File(dataPath);
            String fileName = file.getName();
            int dotPos = fileName.lastIndexOf('.');
            if (dotPos > 0) {
                return fileName.substring(0, dotPos);
            } else if (!fileName.isEmpty()) {
                return fileName;
            }
        }
        return "Untitled Track";
    }
}
