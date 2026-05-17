package org.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MusicBrainzResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

        String musicBrainzQuery =
                "(artist:\"" + escapeQuery(query) + "\" AND primarytype:album) OR "
                        + "(releasegroup:\"" + escapeQuery(query) + "\" AND primarytype:album)";

        return executeSearch(musicBrainzQuery, 100);
    }

    public MusicBrainzResponse searchAlbumsByDate(LocalDate startDate, LocalDate endDate, int limit)
            throws IOException, InterruptedException {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String dateRange = "[" + startDate.format(formatter) + " TO " + endDate.format(formatter) + "]";
        String musicBrainzQuery = "date:" + dateRange + " AND primarytype:album";
        return executeSearch(musicBrainzQuery, limit);
    }

    public MusicBrainzResponse searchAlbumsByArtist(String artistName, int limit)
            throws IOException, InterruptedException {
        String musicBrainzQuery = "artist:\"" + escapeQuery(artistName) + "\" AND primarytype:album";
        return executeSearch(musicBrainzQuery, limit);
    }

    private MusicBrainzResponse executeSearch(String musicBrainzQuery, int limit)
            throws IOException, InterruptedException {
        String encoded =
                URLEncoder.encode(musicBrainzQuery, StandardCharsets.UTF_8);

        String url =
                BASE_URL +
                        "?query=" + encoded +
                        "&limit=" + limit +
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

    private String escapeQuery(String query) {
        return query.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
