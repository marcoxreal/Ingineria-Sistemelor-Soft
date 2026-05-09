package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtistCredit {

    private String name;

    public String getName() {
        return name;
    }
}