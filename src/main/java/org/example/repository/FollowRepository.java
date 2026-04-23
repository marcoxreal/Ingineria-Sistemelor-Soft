package org.example.repository;

import org.example.domain.Follow;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FollowRepository {

    private final String url;
    private final String dbUsername;
    private final String dbPassword;

    public FollowRepository(String url, String dbUsername, String dbPassword) {
        this.url = url;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUsername, dbPassword);
    }

    // -------------------------
    // FOLLOW USER
    // -------------------------
    public void follow(int followerId, int followedId) {
        String sql = """
            INSERT INTO user_follows (follower_id, followed_id)
            VALUES (?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (followerId == followedId) {
                throw new IllegalArgumentException("User cannot follow themselves");
            }

            stmt.setInt(1, followerId);
            stmt.setInt(2, followedId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error while following user", e);
        }
    }

    // -------------------------
    // UNFOLLOW USER
    // -------------------------
    public void unfollow(int followerId, int followedId) {
        String sql = """
            DELETE FROM user_follows
            WHERE follower_id = ? AND followed_id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, followerId);
            stmt.setInt(2, followedId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error while unfollowing user", e);
        }
    }

    // -------------------------
    // GET FOLLOWERS (IDs only)
    // -------------------------
    public List<Integer> getFollowerIds(int userId) {
        String sql = """
            SELECT follower_id
            FROM user_follows
            WHERE followed_id = ?
        """;

        List<Integer> followers = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                followers.add(rs.getInt("follower_id"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching followers", e);
        }

        return followers;
    }

    // -------------------------
    // GET FOLLOWING (IDs only)
    // -------------------------
    public List<Integer> getFollowingIds(int userId) {
        String sql = """
            SELECT followed_id
            FROM user_follows
            WHERE follower_id = ?
        """;

        List<Integer> following = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                following.add(rs.getInt("followed_id"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching following", e);
        }

        return following;
    }

    // -------------------------
    // CHECK IF FOLLOWING
    // -------------------------
    public boolean isFollowing(int followerId, int followedId) {
        String sql = """
            SELECT 1
            FROM user_follows
            WHERE follower_id = ? AND followed_id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, followerId);
            stmt.setInt(2, followedId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Error checking follow relationship", e);
        }
    }
}