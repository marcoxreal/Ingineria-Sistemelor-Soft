package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ArtworkClient {
    private static final String SEARCH_URL = "https://itunes.apple.com/search";

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

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
