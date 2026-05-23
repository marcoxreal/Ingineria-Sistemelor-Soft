package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.domain.Album;
import org.example.domain.User;
import org.example.service.Service;
import org.example.utils.AlbumCardFactory;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;
import org.example.utils.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeController {

    @FXML private TextField searchBar;
    @FXML private TextField userSearchBar;

    @FXML private TilePane albumGrid;

    @FXML private VBox recentBigReleasesSection;
    @FXML private Label recentBigReleasesTitle;
    @FXML private TilePane recentBigReleasesGrid;

    @FXML private VBox bigDebutAlbumsSection;
    @FXML private Label bigDebutAlbumsTitle;
    @FXML private TilePane bigDebutAlbumsGrid;

    @FXML private VBox developersPickSection;
    @FXML private Label developersPickTitle;
    @FXML private TilePane developersPickGrid;

    private Service service;

    private javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
    private final AtomicInteger searchVersion = new AtomicInteger();
    private final AtomicInteger homeFeedVersion = new AtomicInteger();
    private boolean suppressSearchListener;

    public void initialize() {
        this.service = AppContext.service;

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSearchListener) {
                return;
            }
            pause.setOnFinished(e -> handleSearch());
            pause.playFromStart();
        });
        loadHomePageFeed();
    }

    private void loadHomePageFeed() {
        int feedVersion = homeFeedVersion.incrementAndGet();
        albumGrid.getChildren().clear();
        setSectionVisible(recentBigReleasesSection, true);
        setSectionVisible(bigDebutAlbumsSection, true);
        setSectionVisible(developersPickSection, true);
        loadRecentBigReleases(feedVersion);
        loadBigDebutAlbums(feedVersion);
        loadDevelopersPick(feedVersion);
    }

    private void loadRecentBigReleases(int feedVersion) {
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getRecentBigReleases();
                Platform.runLater(() -> {
                    if (!canRenderHomeFeed(feedVersion)) {
                        return;
                    }
                    renderAlbums(recentBigReleasesGrid, FXCollections.observableArrayList(albums));
                    setSectionVisible(recentBigReleasesSection, !albums.isEmpty());
                });
            } catch (Exception e) {
                Logger.error("Error loading recent big releases: " + e.getMessage());
                Platform.runLater(() -> {
                    if (canRenderHomeFeed(feedVersion)) {
                        setSectionVisible(recentBigReleasesSection, false);
                    }
                });
            }
        }, "recent-releases-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadBigDebutAlbums(int feedVersion) {
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getBigDebutAlbums();
                Platform.runLater(() -> {
                    if (!canRenderHomeFeed(feedVersion)) {
                        return;
                    }
                    renderAlbums(bigDebutAlbumsGrid, FXCollections.observableArrayList(albums));
                    setSectionVisible(bigDebutAlbumsSection, !albums.isEmpty());
                });
            } catch (Exception e) {
                Logger.error("Error loading big debut albums: " + e.getMessage());
                Platform.runLater(() -> {
                    if (canRenderHomeFeed(feedVersion)) {
                        setSectionVisible(bigDebutAlbumsSection, false);
                    }
                });
            }
        }, "debut-albums-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadDevelopersPick(int feedVersion) {
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getDevelopersPickAlbums();
                Platform.runLater(() -> {
                    if (!canRenderHomeFeed(feedVersion)) {
                        return;
                    }
                    renderAlbums(developersPickGrid, FXCollections.observableArrayList(albums));
                    setSectionVisible(developersPickSection, !albums.isEmpty());
                });
            } catch (Exception e) {
                Logger.error("Error loading developer's pick albums: " + e.getMessage());
                Platform.runLater(() -> {
                    if (canRenderHomeFeed(feedVersion)) {
                        setSectionVisible(developersPickSection, false);
                    }
                });
            }
        }, "developers-pick-loader");
        thread.setDaemon(true);
        thread.start();
    }


    private void renderAlbums(TilePane targetGrid, ObservableList<Album> albums) {
        targetGrid.getChildren().clear();
        for (Album album : albums) {
            if (album != null) {
                targetGrid.getChildren().add(AlbumCardFactory.createAlbumCard(album));
            }
        }
    }

    private void setSectionVisible(VBox section, boolean visible) {
        section.setVisible(visible);
        section.setManaged(visible);
    }

    private boolean canRenderHomeFeed(int feedVersion) {
        return feedVersion == homeFeedVersion.get()
                && (searchBar.getText() == null || searchBar.getText().isBlank());
    }

    public void startSearch(String query) {
        suppressSearchListener = true;
        searchBar.setText(query == null ? "" : query);
        suppressSearchListener = false;
        handleSearch();
    }

    @FXML
    public void handleUserSearch() {
        if (service == null) {
            showUserSearchError("User search is not available right now.");
            return;
        }

        String query = userSearchBar.getText();
        List<User> users = service.searchUsers(query, AppContext.currentUser);

        if (users.isEmpty()) {
            showUserSearchError("No users found.");
            return;
        }

        User exactMatch = users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(query.trim()))
                .findFirst()
                .orElse(users.get(0));

        openUserPage(exactMatch);
    }

    @FXML
    public void handleMyProfile() {
        if (AppContext.currentUser == null) {
            showUserSearchError("You must be logged in to view your profile.");
            return;
        }

        openUserPage(AppContext.currentUser);
    }

    private void openUserPage(User user) {
        UserController controller = SceneManager.switchScene("/org/example/user-view.fxml");
        controller.setUser(user);
    }

    @FXML
    public void handleSearch() {
        String query = searchBar.getText();
        int requestVersion = searchVersion.incrementAndGet();

        if (query == null || query.isBlank()) {
            loadHomePageFeed();
            return;
        }

        homeFeedVersion.incrementAndGet();

        if (service == null) {
            renderAlbums(albumGrid, FXCollections.observableArrayList());
            return;
        }

        setSectionVisible(recentBigReleasesSection, false);
        setSectionVisible(bigDebutAlbumsSection, false);
        setSectionVisible(developersPickSection, false);


        Thread searchThread = new Thread(() -> {
            List<Album> searchResults;
            try {
                searchResults = service.searchAlbums(query);
            } catch (Exception e) {
                Logger.error("Error during album search: " + e.getMessage());
                searchResults = List.of();
            }
            final List<Album> results = searchResults;

            Platform.runLater(() -> {
                if (requestVersion == searchVersion.get()) {
                    renderAlbums(albumGrid, FXCollections.observableArrayList(results));
                }
            });
        }, "album-search");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void showUserSearchError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("User search");
        alert.setContentText(message);
        alert.showAndWait();
    }

}
