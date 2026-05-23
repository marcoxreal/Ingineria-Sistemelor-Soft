package org.example.repository;

import org.example.domain.Album;
import org.example.domain.Review;
import org.example.domain.UserActivity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlbumInteractionRepository {
    private final String url;
    private final String username;
    private final String password;

    public AlbumInteractionRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        ensureTables();
    }

    public int saveAlbum(Album album) {
        String musicBrainzId = albumKey(album);
        String sql = """
                INSERT INTO albums(musicbrainz_id, title, artist, release_date, type, cover_url)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (musicbrainz_id)
                DO UPDATE SET title = EXCLUDED.title,
                              artist = EXCLUDED.artist,
                              cover_url = EXCLUDED.cover_url
                RETURNING id
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, musicBrainzId);
            statement.setString(2, album.getTitle());
            statement.setString(3, album.getArtist());
            statement.setString(4, album.getReleaseDate());
            statement.setString(5, album.getType());
            statement.setString(6, album.getCoverUrl());

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

            throw new SQLException("Album upsert did not return an id");
        } catch (SQLException e) {
            throw new RuntimeException("Error saving album", e);
        }
    }

    public void markListened(int userId, Album album) {
        int albumId = saveAlbum(album);
        String sql = """
                INSERT INTO user_listened_albums(user_id, album_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, album_id) DO NOTHING
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking album listened", e);
        }
    }

    public void logAlbum(int userId, Album album) {
        int albumId = saveAlbum(album);
        markListened(userId, album);

        String sql = "INSERT INTO album_logs(user_id, album_id) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error logging album", e);
        }
    }

    public void saveReview(int userId, Album album, double rating, String reviewText) {
        int albumId = saveAlbum(album);
        markListened(userId, album);

        String sql = """
                INSERT INTO album_reviews(user_id, album_id, rating, review_text)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, album_id)
                DO UPDATE SET rating = EXCLUDED.rating,
                              review_text = EXCLUDED.review_text,
                              updated_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            statement.setDouble(3, rating);
            statement.setString(4, reviewText);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving review", e);
        }
    }

    public boolean hasListened(int userId, Album album) {
        int albumId = saveAlbum(album);
        String sql = "SELECT 1 FROM user_listened_albums WHERE user_id = ? AND album_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking listened album", e);
        }
    }

    public int countListenedAlbums(int userId) {
        String sql = "SELECT COUNT(*) FROM user_listened_albums WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error counting listened albums", e);
        }
    }

    public double getAverageRating(Album album) {
        int albumId = saveAlbum(album);
        String sql = "SELECT AVG(rating) FROM album_reviews WHERE album_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, albumId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating rating", e);
        }
    }

    public List<Review> getRecentReviews(Album album, int limit) {
        int albumId = saveAlbum(album);
        String sql = """
                SELECT u.username, u.pfp_url, a.title, a.artist, r.rating, r.review_text, r.updated_at
                FROM album_reviews r
                JOIN users u ON u.id = r.user_id
                JOIN albums a ON a.id = r.album_id
                WHERE r.album_id = ?
                ORDER BY r.updated_at DESC
                LIMIT ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, albumId);
            statement.setInt(2, limit);
            ResultSet rs = statement.executeQuery();

            List<Review> reviews = new ArrayList<>();
            while (rs.next()) {
                reviews.add(new Review(
                        rs.getString("username"),
                        rs.getString("pfp_url"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getDouble("rating"),
                        rs.getString("review_text"),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ));
            }
            return reviews;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading reviews", e);
        }
    }

    public Review getUserReviewForAlbum(int userId, Album album) {
        int albumId = saveAlbum(album);
        String sql = """
                SELECT u.username, u.pfp_url, a.title, a.artist, r.rating, r.review_text, r.updated_at
                FROM album_reviews r
                JOIN users u ON u.id = r.user_id
                JOIN albums a ON a.id = r.album_id
                WHERE r.user_id = ? AND r.album_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return new Review(
                        rs.getString("username"),
                        rs.getString("pfp_url"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getDouble("rating"),
                        rs.getString("review_text"),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading user review", e);
        }
    }

    public List<UserActivity> getRecentActivity(int userId, int limit) {
        String sql = """
                SELECT activity_text, activity_time
                FROM (
                    SELECT 'Logged ' || a.title || ' by ' || a.artist AS activity_text, l.logged_at AS activity_time
                    FROM album_logs l
                    JOIN albums a ON a.id = l.album_id
                    WHERE l.user_id = ?
                    UNION ALL
                    SELECT 'Reviewed ' || a.title || ' by ' || a.artist || ' (' || r.rating || '/5)' AS activity_text,
                           r.updated_at AS activity_time
                    FROM album_reviews r
                    JOIN albums a ON a.id = r.album_id
                    WHERE r.user_id = ?
                ) activity
                ORDER BY activity_time DESC
                LIMIT ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, limit);
            ResultSet rs = statement.executeQuery();

            List<UserActivity> activities = new ArrayList<>();
            while (rs.next()) {
                activities.add(new UserActivity(
                        rs.getString("activity_text"),
                        toLocalDateTime(rs.getTimestamp("activity_time"))
                ));
            }
            return activities;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading activity", e);
        }
    }

    public void addFavoriteAlbum(int userId, Album album) {
        if (countFavoriteAlbums(userId) >= 5) {
            throw new IllegalArgumentException("You can only have up to five favorite albums.");
        }

        int albumId = saveAlbum(album);
        String sql = """
                INSERT INTO favorite_albums(user_id, album_id, position)
                SELECT ?, ?, COALESCE(MAX(position), 0) + 1
                FROM favorite_albums
                WHERE user_id = ?
                HAVING COUNT(*) < 5
                ON CONFLICT (user_id, album_id) DO NOTHING
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            statement.setInt(3, userId);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("This album is already in your favorites.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding favorite album", e);
        }
    }

    public void removeFavoriteAlbum(int userId, int albumId) {
        String sql = "DELETE FROM favorite_albums WHERE user_id = ? AND album_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, albumId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error removing favorite album", e);
        }
    }

    public List<Album> getFavoriteAlbums(int userId) {
        String sql = """
                SELECT a.id, a.musicbrainz_id, a.title, a.artist, a.cover_url
                FROM favorite_albums f
                JOIN albums a ON a.id = f.album_id
                WHERE f.user_id = ?
                ORDER BY f.position
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();

            List<Album> albums = new ArrayList<>();
            while (rs.next()) {
                Album album = new Album(
                        rs.getInt("id"),
                        rs.getString("musicbrainz_id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        null,
                        "Album",
                        rs.getString("cover_url")
                );
                albums.add(album);
            }
            return albums;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading favorite albums", e);
        }
    }

    private void ensureTables() {
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS albums (
                    id SERIAL PRIMARY KEY,
                    musicbrainz_id TEXT UNIQUE NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    release_date TEXT,
                    type TEXT,
                    cover_url TEXT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS user_listened_albums (
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    album_id INTEGER NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
                    listened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, album_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS album_logs (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    album_id INTEGER NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
                    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS album_reviews (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    album_id INTEGER NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
                    rating REAL NOT NULL CHECK (rating BETWEEN 0.5 AND 5.0),
                    review_text TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (user_id, album_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS favorite_albums (
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    album_id INTEGER NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
                    position INTEGER NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, album_id),
                    UNIQUE (user_id, position)
                )
                """
        };

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ensuring album interaction tables", e);
        }
    }

    private int countFavoriteAlbums(int userId) {
        String sql = "SELECT COUNT(*) FROM favorite_albums WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error counting favorite albums", e);
        }
    }

    private String albumKey(Album album) {
        if (album.getMusicBrainzId() != null && !album.getMusicBrainzId().isBlank()) {
            return album.getMusicBrainzId();
        }

        return "local:" + normalize(album.getTitle()) + ":" + normalize(album.getArtist());
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
