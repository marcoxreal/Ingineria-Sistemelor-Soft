package org.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MusicBrainzResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MusicBrainzClient {

    private static final String BASE_URL =
            "https://musicbrainz.org/ws/2/release-group/";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public MusicBrainzClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public MusicBrainzResponse searchAlbums(String query)
            throws IOException, InterruptedException {

        String encoded =
                URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url =
                BASE_URL +
                        "?query=" + encoded +
                        "&fmt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header(
                        "User-Agent",
                        "MusicApp/1.0 ( your@email.com )"
                )
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        return mapper.readValue(
                response.body(),
                MusicBrainzResponse.class
        );
    }
}
