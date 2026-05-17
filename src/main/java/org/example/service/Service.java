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

import java.io.IOException;
import java.time.LocalDate;
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
        try {
            if (query == null || query.isBlank()) {
                return List.of();
            }

            String q = query.trim();
            var response = musicBrainzClient.searchAlbums(q);
            return processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), q);
        } catch (IOException | InterruptedException e) {
            Logger.error("Error searching albums: " + e.getMessage());
            return List.of();
        }
    }

    public List<Album> getRecentBigReleases() {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(90); // Last 3 months
            var response = musicBrainzClient.searchAlbumsByDate(startDate, endDate, 20); // Limit to 20 for a section
            return processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), null);
        } catch (IOException | InterruptedException e) {
            Logger.error("Error fetching recent big releases: " + e.getMessage());
            return List.of();
        }
    }

    public List<Album> getDevelopersPickAlbums() {
        List<Album> developerPicks = new ArrayList<>();
        List<String> artists = List.of("Alice in Chains", "Megadeth", "Soundgarden", "Metallica", "Iron Maiden"); // Example artists

        for (String artist : artists) {
            try {
                var response = musicBrainzClient.searchAlbumsByArtist(artist, 5); // Get a few albums per artist
                developerPicks.addAll(processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), artist));
            } catch (IOException | InterruptedException e) {
                Logger.error("Error fetching developer's pick albums for artist " + artist + ": " + e.getMessage());
            }
        }
        return developerPicks.stream().limit(20).toList(); // Limit total developer picks
    }

    public List<Album> getBigDebutAlbums() {
        // This is a more complex query and might require additional logic or a different API.
        // For now, I'll return an empty list or a placeholder.
        // A proper implementation would involve:
        // 1. Searching for artists.
        // 2. For each artist, finding their earliest "Album" release.
        // This is beyond the scope of a quick fix and might hit API rate limits quickly.
        // Returning an empty list for now.
        return List.of();
    }

    private List<Album> processReleaseGroups(List<ReleaseGroup> groups, String query) throws IOException, InterruptedException {
        List<Album> albums = new ArrayList<>();
        Map<String, String> artworkByAlbum = (query != null && !query.isBlank()) ? getArtworkByAlbum(query) : Map.of();
        Set<String> seen = new HashSet<>();

        for (ReleaseGroup group : groups) {
            if (group == null) continue;
            if (!isCleanAlbumGroup(group)) continue;

            String title = group.getTitle();
            if (title == null) continue;

            String artist = getArtistName(group);

            if (query != null && !query.isBlank() && !matchesAlbumSearch(query, title, artist)) {
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
        return albums;
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
