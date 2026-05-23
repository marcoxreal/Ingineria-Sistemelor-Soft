package org.example.utils;

import javafx.scene.image.Image;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Duration EXPIRE_AFTER_ACCESS = Duration.ofMinutes(30);
    private static final int MAX_ENTRIES = 250;
    private static final Map<String, CachedImage> CACHE = new ConcurrentHashMap<>();
    private static Instant lastCleanup = Instant.EPOCH;

    private ImageCache() {
    }

    public static Image get(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        cleanupIfNeeded();

        CachedImage cached = CACHE.get(url);
        if (cached != null) {
            cached.touch();
            return cached.image();
        }

        try {
            Image image = new Image(url, true);
            CACHE.put(url, new CachedImage(image));
            trimIfNeeded();
            return image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void cleanupIfNeeded() {
        Instant now = Instant.now();
        if (Duration.between(lastCleanup, now).compareTo(Duration.ofMinutes(1)) < 0) {
            return;
        }

        lastCleanup = now;
        CACHE.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().lastAccessed(), now).compareTo(EXPIRE_AFTER_ACCESS) > 0
        );
    }

    private static void trimIfNeeded() {
        if (CACHE.size() <= MAX_ENTRIES) {
            return;
        }

        CACHE.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().lastAccessed()))
                .limit(CACHE.size() - MAX_ENTRIES)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(CACHE::remove);
    }

    private static class CachedImage {
        private final Image image;
        private Instant lastAccessed;

        private CachedImage(Image image) {
            this.image = image;
            this.lastAccessed = Instant.now();
        }

        private Image image() {
            return image;
        }

        private Instant lastAccessed() {
            return lastAccessed;
        }

        private void touch() {
            lastAccessed = Instant.now();
        }
    }
}
