package org.example.repository;

import org.example.domain.User;
import org.example.exceptions.InexistentEntityException;
import org.example.utils.Logger;

import java.sql.*;
import java.util.*;

public class UserRepository implements IUserRepo {

    private final String url;
    private final String dbUsername;
    private final String dbPassword;

    public UserRepository(String url, String dbUsername, String dbPassword) {
        this.url = url;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    // -------------------------
    // Connection helper
    // -------------------------
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUsername, dbPassword);
    }

    // -------------------------
    // Mapping helper
    // -------------------------
    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("email"),
                rs.getString("pfp_url")
        );
    }

    // -------------------------
    // Exists
    // -------------------------
    private boolean exists(Integer id) {
        String sql = "SELECT 1 FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Error checking user existence", e);
        }
    }

    // -------------------------
    // FIND BY ID
    // -------------------------
    @Override
    public User find(Integer id) {
        Logger.debug("Finding user by id: " + id);

        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }

        String sql = "SELECT id, username, password_hash, email, pfp_url FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new InexistentEntityException("User with id " + id + " does not exist");
            }

            return mapUser(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id", e);
        }
    }

    // -------------------------
    // FIND BY USERNAME
    // -------------------------
    @Override
    public User findByUsername(String username) {
        Logger.debug("Finding user by username: " + username);

        if (username == null) {
            throw new IllegalArgumentException("Username must not be null");
        }

        String sql = "SELECT id, username, password_hash, email, pfp_url FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new InexistentEntityException("User " + username + " does not exist");
            }

            return mapUser(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username", e);
        }
    }

    // -------------------------
    // GET ALL
    // -------------------------
    @Override
    public Iterable<User> getAll() {
        Logger.debug("Fetching all users");

        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, email, pfp_url FROM users";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all users", e);
        }

        return users;
    }

    // -------------------------
    // GET MAP
    // -------------------------
    @Override
    public Map<Integer, User> getMap() {
        Logger.debug("Fetching all users as map");

        Map<Integer, User> map = new HashMap<>();
        String sql = "SELECT id, username, password_hash, email, pfp_url FROM users";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = mapUser(rs);
                map.put(user.getId(), user);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching users map", e);
        }

        return map;
    }

    // -------------------------
    // SAVE
    // -------------------------
    public void save(User user) {
        Logger.info("Saving user: " + user.getUsername());

        String sql = """
                INSERT INTO users (username, email, password_hash, pfp_url)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getPfpUrl());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    // -------------------------
    // CLEAR
    // -------------------------
    @Override
    public void clear() {
        Logger.warning("Clearing all users");

        String sql = "DELETE FROM users";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error clearing users", e);
        }
    }
}