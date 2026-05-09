package org.example.domain;

public class Album  extends Entity<Integer> {
    private String musicBrainzId;
    private String title;
    private String artist;
    private String releaseDate;
    private String type;
    private String coverUrl;

    public Album(Integer id,
                 String musicBrainzId,
                 String title,
                 String artist,
                 String releaseDate,
                 String type,
                 String coverUrl) {

        super(id);
        this.musicBrainzId = musicBrainzId;
        this.title = title;
        this.artist = artist;
        this.releaseDate = releaseDate;
        this.type = type;
        this.coverUrl = coverUrl;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getType() {
        return type;
    }

    public String getCoverUrl() {
        return coverUrl;
    }
}