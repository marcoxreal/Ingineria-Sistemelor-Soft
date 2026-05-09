package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseGroup {

    private String title;
    private String id;

    @JsonProperty("primary-type")
    private String primaryType;

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

    public List<ArtistCredit> getArtistCredit() {
        return artistCredit;
    }
}
