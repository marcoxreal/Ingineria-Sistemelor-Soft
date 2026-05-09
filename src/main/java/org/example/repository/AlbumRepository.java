package org.example.repository;

import org.example.domain.Album;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlbumRepository implements IAlbumRepo {

    private final String url;
    private final String username;
    private final String password;

    public AlbumRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private Album extractAlbum(ResultSet rs) throws SQLException {
        return new Album(
                rs.getInt("id"),
                rs.getString("musicbrainz_id"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("release_date"),
                rs.getString("type"),
                rs.getString("cover_url")
        );
    }

    @Override
    public void save(Album album) {

        String sql = """
            INSERT INTO albums(
                musicbrainz_id,
                title,
                artist,
                release_date,
                type,
                cover_url
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (musicbrainz_id) DO NOTHING
        """;

        try (
                Connection connection =
                        DriverManager.getConnection(url, username, password);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, album.getMusicBrainzId());
            statement.setString(2, album.getTitle());
            statement.setString(3, album.getArtist());
            statement.setString(4, album.getReleaseDate());
            statement.setString(5, album.getType());
            statement.setString(6, album.getCoverUrl());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Album> getAll() {

        List<Album> albums = new ArrayList<>();

        String sql = "SELECT * FROM albums";

        try (
                Connection connection =
                        DriverManager.getConnection(url, username, password);

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs = statement.executeQuery()
        ) {

            while (rs.next()) {
                albums.add(extractAlbum(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return albums;
    }
}