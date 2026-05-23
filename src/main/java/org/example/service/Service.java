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
            List<RankedAlbum> candidates = new ArrayList<>();
            int order = 0;

            for (Album album : artworkClient.searchAlbums(q)) {
                if (matchesAlbumSearch(q, album.getTitle(), album.getArtist())) {
                    candidates.add(new RankedAlbum(album, 60, order++));
                }
            }

            for (Album album : processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), q)) {
                candidates.add(new RankedAlbum(album, 0, order++));
            }

            return rankAndDedupeAlbums(candidates, q).stream()
                    .limit(50)
                    .toList();
        } catch (IOException | InterruptedException e) {
            Logger.error("Error searching albums: " + e.getMessage());
            return List.of();
        }
    }

    public List<Album> getRecentBigReleases() {
        try {
            List<Album> albums = artworkClient.getMostPlayedAlbums().stream()
                    .filter(album -> !isNoisyAlbum(album))
                    .limit(15)
                    .toList();

            if (!albums.isEmpty()) {
                return albums;
            }

            return getFallbackRecentBigReleases();
        } catch (IOException | InterruptedException e) {
            Logger.error("Error fetching recent big releases: " + e.getMessage());
            return getFallbackRecentBigReleases();
        }
    }

    private List<Album> getFallbackRecentBigReleases() {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(120);
            var response = musicBrainzClient.searchAlbumsByDate(startDate, endDate, 100);
            List<Album> albums = processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), null)
                    .stream()
                    .filter(album -> !isNoisyAlbum(album))
                    .limit(15)
                    .toList();

            if (!albums.isEmpty()) {
                return albums;
            }
        } catch (IOException | InterruptedException e) {
            Logger.error("Error fetching fallback recent releases: " + e.getMessage());
        }

        return resolveCuratedAlbums(List.of(
                new AlbumPick("ICEMAN", "Drake"),
                new AlbumPick("MUSIC", "Playboi Carti"),
                new AlbumPick("Mayhem", "Lady Gaga"),
                new AlbumPick("Hurry Up Tomorrow", "The Weeknd"),
                new AlbumPick("GNX", "Kendrick Lamar"),
                new AlbumPick("Short n' Sweet", "Sabrina Carpenter"),
                new AlbumPick("Hit Me Hard and Soft", "Billie Eilish"),
                new AlbumPick("Cowboy Carter", "Beyonce"),
                new AlbumPick("The Tortured Poets Department", "Taylor Swift"),
                new AlbumPick("Chromakopia", "Tyler, The Creator")
        )).stream().limit(15).toList();
    }

    public List<Album> getDevelopersPickAlbums() {
        return resolveCuratedAlbums(List.of(
                new AlbumPick("Dirt", "Alice in Chains"),
                new AlbumPick("Facelift", "Alice in Chains"),
                new AlbumPick("Rust in Peace", "Megadeth"),
                new AlbumPick("Peace Sells... But Who's Buying?", "Megadeth"),
                new AlbumPick("Superunknown", "Soundgarden"),
                new AlbumPick("Badmotorfinger", "Soundgarden"),
                new AlbumPick("Master of Puppets", "Metallica"),
                new AlbumPick("Ride the Lightning", "Metallica"),
                new AlbumPick("Paranoid", "Black Sabbath"),
                new AlbumPick("Nevermind", "Nirvana"),
                new AlbumPick("Ten", "Pearl Jam"),
                new AlbumPick("Rage Against the Machine", "Rage Against the Machine")
        ));
    }

    public List<Album> getBigDebutAlbums() {
        return resolveCuratedAlbums(List.of(
                new AlbumPick("The Doors", "The Doors"),
                new AlbumPick("Led Zeppelin", "Led Zeppelin"),
                new AlbumPick("Black Sabbath", "Black Sabbath"),
                new AlbumPick("Appetite for Destruction", "Guns N' Roses"),
                new AlbumPick("Ten", "Pearl Jam"),
                new AlbumPick("Facelift", "Alice in Chains"),
                new AlbumPick("Kill 'Em All", "Metallica"),
                new AlbumPick("Killing Is My Business... and Business Is Good!", "Megadeth"),
                new AlbumPick("Definitely Maybe", "Oasis"),
                new AlbumPick("Is This It", "The Strokes"),
                new AlbumPick("Whatever People Say I Am, That's What I'm Not", "Arctic Monkeys"),
                new AlbumPick("Rage Against the Machine", "Rage Against the Machine")
        ));
    }

    private List<Album> resolveCuratedAlbums(List<AlbumPick> picks) {
        List<Album> albums = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (AlbumPick pick : picks) {
            try {
                findBestAlbum(pick).ifPresent(album -> {
                    String key = albumDedupeKey(album);
                    if (seen.add(key)) {
                        albums.add(album);
                    }
                });
            } catch (IOException | InterruptedException e) {
                Logger.error("Error fetching curated album " + pick.title() + ": " + e.getMessage());
            }
        }

        return albums;
    }

    private Optional<Album> findBestAlbum(AlbumPick pick) throws IOException, InterruptedException {
        String query = pick.title() + " " + pick.artist();

        return artworkClient.searchAlbums(query).stream()
                .filter(album -> normalize(album.getArtist()).equals(normalize(pick.artist())))
                .filter(album -> normalize(album.getTitle()).contains(normalize(pick.title()))
                        || normalize(pick.title()).contains(normalize(album.getTitle())))
                .filter(album -> !isNoisyAlbum(album))
                .findFirst()
                .or(() -> {
                    try {
                        var response = musicBrainzClient.searchAlbumsByArtist(pick.artist(), 50);
                        return processReleaseGroups(response != null ? response.getReleaseGroups() : List.of(), pick.title()).stream()
                                .filter(album -> normalize(album.getArtist()).equals(normalize(pick.artist())))
                                .filter(album -> normalize(album.getTitle()).contains(normalize(pick.title()))
                                        || normalize(pick.title()).contains(normalize(album.getTitle())))
                                .findFirst();
                    } catch (IOException | InterruptedException e) {
                        return Optional.empty();
                    }
                });
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
                    group.getFirstReleaseDate(),
                    "Album",
                    coverUrl
            ));
        }
        if (query == null || query.isBlank()) {
            return albums;
        }

        return albums.stream()
                .sorted(Comparator.comparingInt((Album album) -> relevanceScore(album, query, 0)).reversed())
                .toList();
    }

    private List<Album> rankAndDedupeAlbums(List<RankedAlbum> candidates, String query) {
        Map<String, RankedAlbum> bestByAlbum = new LinkedHashMap<>();

        for (RankedAlbum candidate : candidates) {
            String key = albumDedupeKey(candidate.album());
            RankedAlbum existing = bestByAlbum.get(key);

            if (existing == null || compareRank(candidate, existing, query) < 0) {
                bestByAlbum.put(key, candidate);
            } else if (existing.album().getCoverUrl().isBlank() && !candidate.album().getCoverUrl().isBlank()) {
                bestByAlbum.put(key, candidate);
            }
        }

        return bestByAlbum.values().stream()
                .sorted((left, right) -> compareRank(left, right, query))
                .map(RankedAlbum::album)
                .toList();
    }

    private int compareRank(RankedAlbum left, RankedAlbum right, String query) {
        int scoreCompare = Integer.compare(
                relevanceScore(right.album(), query, right.sourceBoost()),
                relevanceScore(left.album(), query, left.sourceBoost())
        );

        if (scoreCompare != 0) {
            return scoreCompare;
        }

        return Integer.compare(left.order(), right.order());
    }

    private int relevanceScore(Album album, String query, int sourceBoost) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(album.getTitle());
        String normalizedArtist = normalize(album.getArtist());
        String combined = normalizedTitle + " " + normalizedArtist;

        int score = sourceBoost;

        if (normalizedArtist.equals(normalizedQuery)) {
            score += 350;
        } else if (normalizedArtist.contains(normalizedQuery)) {
            score += 260;
        }

        if (normalizedTitle.equals(normalizedQuery)) {
            score += 280;
        } else if (normalizedTitle.startsWith(normalizedQuery)) {
            score += 190;
        } else if (normalizedTitle.contains(normalizedQuery)) {
            score += 150;
        }

        for (String term : normalizedQuery.split(" ")) {
            if (!term.isBlank() && combined.contains(term)) {
                score += 25;
            }
        }

        score -= noisePenalty(normalizedTitle);

        if (album.getReleaseDate() != null && album.getReleaseDate().matches("\\d{4}.*")) {
            int year = Integer.parseInt(album.getReleaseDate().substring(0, 4));
            if (year >= 1960 && year <= 2015) {
                score += 15;
            }
        }

        if (normalizedTitle.length() > 45) {
            score -= 20;
        }

        return score;
    }

    private int noisePenalty(String normalizedTitle) {
        int penalty = 0;
        List<String> noisyWords = List.of(
                "greatest hits",
                "best of",
                "essential",
                "collection",
                "anthology",
                "karaoke",
                "tribute",
                "live",
                "remix",
                "interview",
                "anniversary",
                "deluxe",
                "expanded",
                "remastered",
                "single"
        );

        for (String word : noisyWords) {
            if (normalizedTitle.contains(word)) {
                penalty += 45;
            }
        }

        return penalty;
    }

    private boolean isNoisyAlbum(Album album) {
        return noisePenalty(normalize(album.getTitle())) >= 45;
    }

    private String albumDedupeKey(Album album) {
        return normalize(album.getTitle())
                .replace(" remastered", "")
                .replace(" deluxe edition", "")
                .replace(" expanded edition", "")
                + "|"
                + normalize(album.getArtist());
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

    private record RankedAlbum(Album album, int sourceBoost, int order) {
    }

    private record AlbumPick(String title, String artist) {
    }
}
