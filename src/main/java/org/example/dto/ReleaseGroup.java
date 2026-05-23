package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseGroup {

    private String title;
    private String id;
    private Integer score;

    @JsonProperty("first-release-date")
    private String firstReleaseDate;

    @JsonProperty("primary-type")
    private String primaryType;

    @JsonProperty("secondary-types")
    private List<String> secondaryTypes;

    @JsonProperty("artist-credit")
    private List<ArtistCredit> artistCredit;

    public String getTitle() {
        return title;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public String getId() {
        return id;
    }

    public Integer getScore() {
        return score;
    }

    public String getFirstReleaseDate() {
        return firstReleaseDate;
    }

    public List<ArtistCredit> getArtistCredit() {
        return artistCredit;
    }

    public List<String> getSecondaryTypes() {
        return secondaryTypes;
    }
}
