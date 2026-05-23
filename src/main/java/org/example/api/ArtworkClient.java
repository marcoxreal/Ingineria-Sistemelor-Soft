package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.Album;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ArtworkClient {
    private static final String SEARCH_URL = "https://itunes.apple.com/search";
    private static final String MOST_PLAYED_ALBUMS_URL =
            "https://rss.applemarketingtools.com/api/v2/us/music/most-played/50/albums.json";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public ArtworkClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.mapper = new ObjectMapper();
    }

    public Map<String, String> searchAlbumCovers(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?term=" + encoded + "&entity=album&limit=100";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Map.of();
        }

        Map<String, String> covers = new HashMap<>();
        JsonNode results = mapper.readTree(response.body()).path("results");
        if (!results.isArray()) {
            return covers;
        }

        for (JsonNode result : results) {
            String title = result.path("collectionName").asText("");
            String artist = result.path("artistName").asText("");
            String artworkUrl = result.path("artworkUrl100").asText("");

            if (!title.isBlank() && !artist.isBlank() && !artworkUrl.isBlank()) {
                covers.putIfAbsent(coverKey(title, artist), highResolutionArtworkUrl(artworkUrl));
            }
        }

        return covers;
    }

    public List<Album> searchAlbums(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?term=" + encoded + "&entity=album&limit=100";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return List.of();
        }

        JsonNode results = mapper.readTree(response.body()).path("results");
        if (!results.isArray()) {
            return List.of();
        }

        List<Album> albums = new ArrayList<>();
        for (JsonNode result : results) {
            String title = result.path("collectionName").asText("");
            String artist = result.path("artistName").asText("");
            String artworkUrl = result.path("artworkUrl100").asText("");
            String releaseDate = result.path("releaseDate").asText("");
            long collectionId = result.path("collectionId").asLong(0);

            if (title.isBlank() || artist.isBlank() || isNoisyCollection(title)) {
                continue;
            }

            albums.add(new Album(
                    null,
                    collectionId > 0 ? "itunes:" + collectionId : "itunes:" + coverKey(title, artist),
                    title,
                    artist,
                    releaseDate.length() >= 10 ? releaseDate.substring(0, 10) : null,
                    "Album",
                    artworkUrl.isBlank() ? "" : highResolutionArtworkUrl(artworkUrl)
            ));
        }

        return albums;
    }

    public List<Album> getMostPlayedAlbums() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOST_PLAYED_ALBUMS_URL))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return List.of();
        }

        JsonNode results = mapper.readTree(response.body()).path("feed").path("results");
        if (!results.isArray()) {
            return List.of();
        }

        List<Album> albums = new ArrayList<>();
        for (JsonNode result : results) {
            String title = result.path("name").asText("");
            String artist = result.path("artistName").asText("");
            String artworkUrl = result.path("artworkUrl100").asText("");
            String releaseDate = result.path("releaseDate").asText("");
            String id = result.path("id").asText("");

            if (title.isBlank() || artist.isBlank() || isNoisyCollection(title)) {
                continue;
            }

            albums.add(new Album(
                    null,
                    id.isBlank() ? "apple:" + coverKey(title, artist) : "apple:" + id,
                    title,
                    artist,
                    releaseDate.isBlank() ? null : releaseDate,
                    "Album",
                    artworkUrl.isBlank() ? "" : highResolutionArtworkUrl(artworkUrl)
            ));
        }

        return albums;
    }

    public String findCover(Map<String, String> covers, String title, String artist) {
        if (covers == null || covers.isEmpty()) {
            return "";
        }

        String exact = covers.get(coverKey(title, artist));
        if (exact != null) {
            return exact;
        }

        String normalizedTitle = normalize(title);
        String normalizedArtist = normalize(artist);

        for (Map.Entry<String, String> entry : covers.entrySet()) {
            String key = entry.getKey();
            if (key.contains(normalizedTitle) && key.contains(normalizedArtist)) {
                return entry.getValue();
            }
        }

        return "";
    }

    private String coverKey(String title, String artist) {
        return normalize(title) + "|" + normalize(artist);
    }

    private String highResolutionArtworkUrl(String artworkUrl) {
        return artworkUrl.replace("100x100bb", "600x600bb");
    }

    private boolean isNoisyCollection(String title) {
        String normalized = normalize(title);
        return normalized.endsWith(" single")
                || normalized.contains(" karaoke")
                || normalized.contains(" tribute")
                || normalized.contains(" made famous by");
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
