package org.example.domain;

import java.time.LocalDateTime;

public class Review {
    private final String username;
    private final String albumTitle;
    private final String artist;
    private final int rating;
    private final String reviewText;
    private final LocalDateTime createdAt;

    public Review(String username, String albumTitle, String artist, int rating, String reviewText, LocalDateTime createdAt) {
        this.username = username;
        this.albumTitle = albumTitle;
        this.artist = artist;
        this.rating = rating;
        this.reviewText = reviewText;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getArtist() {
        return artist;
    }

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
