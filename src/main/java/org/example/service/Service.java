package org.example.service;

import org.example.domain.Album;
import org.example.domain.User;
import org.example.domain.Review;
import org.example.domain.UserActivity;
import org.example.repository.AlbumInteractionRepository;
import org.example.dto.ReleaseGroup;
import org.example.repository.FollowRepository;
import org.example.repository.Repository;
import org.example.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import org.example.utils.IObserver;
import org.example.utils.Logger;
import org.mindrot.jbcrypt.BCrypt;

import org.example.api.ArtworkClient;
import org.example.api.MusicBrainzClient;

public class Service implements IService{
    private static final Set<String> NOISY_SECONDARY_TYPES = Set.of(
            "Compilation",
            "Demo",
            "DJ-mix",
            "Interview",
            "Live",
            "Mixtape/Street",
            "Remix",
            "Soundtrack",
            "Spokenword"
    );

    private final Repository<Integer, User> userRepository;
    private final FollowRepository followRepository;
    private final AlbumInteractionRepository albumInteractionRepository;
    private final MusicBrainzClient musicBrainzClient;
    private final ArtworkClient artworkClient;

    private Map<Integer, IObserver> loggedClients;

    public Service(Repository<Integer, User> userRepository, FollowRepository followRepository, AlbumInteractionRepository albumInteractionRepository, MusicBrainzClient musicBrainzClient) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.albumInteractionRepository = albumInteractionRepository;
        this.loggedClients = new ConcurrentHashMap<>();
        this.musicBrainzClient = musicBrainzClient;
        this.artworkClient = new ArtworkClient();

    }

    @Override
    public Iterable<User> getAllUsers() {
        return userRepository.getAll();
    }

    @Override
    public synchronized User login(String username, String password, IObserver client) throws Exception {
        User user = getUserByUsername(username);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            if (loggedClients.containsKey(user.getId()))
                throw new Exception("User is already logged in!");
            loggedClients.put(user.getId(), client);
            Logger.info("User " + username + " logged in.");
            return user;
        }
        throw new Exception("Wrong password or username!");
    }

    @Override
    public synchronized void logout(User user, IObserver client) throws Exception {
        IObserver localClient = loggedClients.remove(user.getId());
        if (localClient == null) throw new Exception("User is not logged in!");
    }

    @Override
    public synchronized void register(String username, String password, String email) {
        User user = getUserByUsername(username);
        if (user != null) throw new IllegalArgumentException("Username already exists!");
        user = new User(null, username, BCrypt.hashpw(password, BCrypt.gensalt()), email, null);
        userRepository.save(user);
    }

    private User getUserByUsername(String username) {
        return StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    private void notifyUsers() {
        for (IObserver client : loggedClients.values()) {
            try {
                client.updateParticipanti();
            } catch (Exception e) {
                System.err.println("Error notifying a client: " + e.getMessage());
            }
        }
    }

    public void followUser(int followerId, int followedId) {
        if (followerId == followedId) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        if (followRepository.isFollowing(followerId, followedId)) {
            throw new IllegalArgumentException("Already following this user");
        }

        followRepository.follow(followerId, followedId);

        notifyUsers();
    }

    public void unfollowUser(int followerId, int followedId) {
        if (!followRepository.isFollowing(followerId, followedId)) {
            throw new IllegalArgumentException("You are not following this user");
        }

        followRepository.unfollow(followerId, followedId);

        notifyUsers();
    }

    public List<Integer> getFollowers(int userId) {
        return followRepository.getFollowerIds(userId);
    }

    public List<Integer> getFollowing(int userId) {
        return followRepository.getFollowingIds(userId);
    }

    public boolean isFollowing(int followerId, int followedId) {
        return followRepository.isFollowing(followerId, followedId);
    }

    public User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String query = username.trim();

        return StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(user -> user.getUsername().equalsIgnoreCase(query))
                .findFirst()
                .orElse(null);
    }

    public List<User> searchUsers(String query, User currentUser) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        Integer currentUserId = currentUser == null ? null : currentUser.getId();

        return StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(user -> currentUserId == null || !Objects.equals(user.getId(), currentUserId))
                .filter(user -> user.getUsername().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(user -> user.getUsername().toLowerCase(Locale.ROOT)))
                .limit(10)
                .toList();
    }

    public void markAlbumListened(User user, Album album) {
        requireUser(user);
        albumInteractionRepository.markListened(user.getId(), album);
    }

    public void logAlbum(User user, Album album) {
        requireUser(user);
        albumInteractionRepository.logAlbum(user.getId(), album);
    }

    public boolean hasListened(User user, Album album) {
        return user != null && albumInteractionRepository.hasListened(user.getId(), album);
    }

    public int countListenedAlbums(User user) {
        return user == null ? 0 : albumInteractionRepository.countListenedAlbums(user.getId());
    }

    public void saveReview(User user, Album album, double rating, String reviewText) {
        requireUser(user);
        if (rating < 0.5 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 0.5 and 5.0.");
        }
        albumInteractionRepository.saveReview(user.getId(), album, rating, reviewText);
    }

    public double getAlbumAverageRating(Album album) {
        return albumInteractionRepository.getAverageRating(album);
    }

    public List<Review> getRecentReviews(Album album) {
        return albumInteractionRepository.getRecentReviews(album, 10);
    }

    public Review getUserReviewForAlbum(User user, Album album) {
        return user == null ? null : albumInteractionRepository.getUserReviewForAlbum(user.getId(), album);
    }

    public List<UserActivity> getRecentActivity(User user) {
        return user == null ? List.of() : albumInteractionRepository.getRecentActivity(user.getId(), 10);
    }

    public void addFavoriteAlbum(User user, Album album) {
        requireUser(user);
        albumInteractionRepository.addFavoriteAlbum(user.getId(), album);
    }

    public void removeFavoriteAlbum(User user, Album album) {
        requireUser(user);
        if (album.getId() != null) {
            albumInteractionRepository.removeFavoriteAlbum(user.getId(), album.getId());
        }
    }

    public List<Album> getFavoriteAlbums(User user) {
        return user == null ? List.of() : albumInteractionRepository.getFavoriteAlbums(user.getId());
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("You must be logged in.");
        }
    }

    public List<Album> searchAlbums(String query) {

        List<Album> albums = new ArrayList<>();

        try {
            if (query == null || query.isBlank()) {
                return albums;
            }

            String q = query.trim();

            var response = musicBrainzClient.searchAlbums(q);
            Map<String, String> artworkByAlbum = getArtworkByAlbum(q);

            List<ReleaseGroup> groups =
                    (response != null && response.getReleaseGroups() != null)
                            ? response.getReleaseGroups()
                            : List.of();

            Set<String> seen = new HashSet<>();

            for (ReleaseGroup group : groups) {

                if (group == null)
                    continue;

                if (!isCleanAlbumGroup(group)) {
                    continue;
                }

                String title = group.getTitle();
                if (title == null)
                    continue;

                String artist = getArtistName(group);

                if (!matchesAlbumSearch(q, title, artist)) {
                    continue;
                }

                if (!seen.add((title + "|" + artist).toLowerCase(Locale.ROOT))) {
                    continue;
                }

                String coverUrl = artworkClient.findCover(artworkByAlbum, title, artist);
                if (coverUrl.isBlank()) {
                    coverUrl = getCoverArtArchiveUrl(group);
                }

                albums.add(new Album(
                        null,
                        group.getId(),
                        title,
                        artist,
                        null,
                        "Album",
                        coverUrl
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String normalizedQuery = normalize(query);
        long artistMatchCount = albums.stream()
                .filter(album -> normalize(album.getArtist()).contains(normalizedQuery))
                .count();

        if (artistMatchCount >= 3) {
            albums.removeIf(album -> !normalize(album.getArtist()).contains(normalizedQuery));
        }

        return albums.stream().limit(40).toList();
    }

    private Map<String, String> getArtworkByAlbum(String query) {
        try {
            return artworkClient.searchAlbumCovers(query);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String getCoverArtArchiveUrl(ReleaseGroup group) {
        if (group.getId() == null || group.getId().isBlank()) {
            return "";
        }

        return "https://coverartarchive.org/release-group/" + group.getId() + "/front";
    }

    private boolean isCleanAlbumGroup(ReleaseGroup group) {
        if (!"Album".equalsIgnoreCase(group.getPrimaryType())) {
            return false;
        }

        if (group.getSecondaryTypes() == null || group.getSecondaryTypes().isEmpty()) {
            return true;
        }

        return group.getSecondaryTypes().stream()
                .noneMatch(NOISY_SECONDARY_TYPES::contains);
    }

    private boolean matchesAlbumSearch(String query, String title, String artist) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        String normalizedArtist = normalize(artist);
        String combined = normalizedTitle + " " + normalizedArtist;

        if (normalizedTitle.contains(normalizedQuery) || normalizedArtist.contains(normalizedQuery)) {
            return true;
        }

        String[] terms = normalizedQuery.split(" ");
        for (String term : terms) {
            if (!term.isBlank() && !combined.contains(term)) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String getArtistName(ReleaseGroup group) {
        if (group.getArtistCredit() == null || group.getArtistCredit().isEmpty()) {
            return "Unknown Artist";
        }

        String name = group.getArtistCredit().get(0).getName();
        return name == null || name.isBlank() ? "Unknown Artist" : name;
    }
}
