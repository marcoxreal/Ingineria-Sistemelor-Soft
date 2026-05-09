package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Release {

    private String id;
    private String title;
    private String date;

    @JsonProperty("artist-credit")
    private List<ArtistCredit> artistCredit;

    @JsonProperty("release-group")
    private ReleaseGroup releaseGroup;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public List<ArtistCredit> getArtistCredit() {
        return artistCredit;
    }

    public ReleaseGroup getReleaseGroup() {
        return releaseGroup;
    }
}