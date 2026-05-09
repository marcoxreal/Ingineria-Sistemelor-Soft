package org.example.domain;

public class AlbumTest {
    private String title;
    private String artist;
    private String coverUrl;
    private double rating;
    public AlbumTest(String title, String artist, String coverUrl, double rating) {
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.rating = rating;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public String getCoverUrl() {
        return coverUrl;
    }
    public double getRating() {
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
