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

    @FXML private TilePane albumGrid; // This will be for search results

    // New FXML elements for sections
    @FXML private VBox recentBigReleasesSection;
    @FXML private Label recentBigReleasesTitle;
    @FXML private TilePane recentBigReleasesGrid;

    @FXML private VBox bigDebutAlbumsSection;
    @FXML private Label bigDebutAlbumsTitle;
    @FXML private TilePane bigDebutAlbumsGrid;

    @FXML private VBox developersPickSection;
    @FXML private Label developersPickTitle;
    @FXML private TilePane developersPickGrid;


    private ObservableList<Album> allAlbums; // This will now hold combined data for initial display or search context

    private Service service;

    private javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
    private final AtomicInteger searchVersion = new AtomicInteger();
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
        // Clear any mock data
        if (allAlbums != null) {
            allAlbums.clear();
        } else {
            allAlbums = FXCollections.observableArrayList();
        }

        // Load Recent Big Releases
        loadRecentBigReleases();

        // Load Big Debut Albums (currently a placeholder)
        loadBigDebutAlbums();

        // Load Developer's Pick
        loadDevelopersPick();
    }

    private void loadRecentBigReleases() {
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getRecentBigReleases();
                Platform.runLater(() -> {
                    renderAlbums(recentBigReleasesGrid, FXCollections.observableArrayList(albums));
                    if (!albums.isEmpty()) {
                        recentBigReleasesSection.setVisible(true);
                        recentBigReleasesSection.setManaged(true);
                    } else {
                        recentBigReleasesSection.setVisible(false);
                        recentBigReleasesSection.setManaged(false);
                    }
                });
            } catch (Exception e) {
                Logger.error("Error loading recent big releases: " + e.getMessage());
                Platform.runLater(() -> {
                    recentBigReleasesSection.setVisible(false);
                    recentBigReleasesSection.setManaged(false);
                });
            }
        }, "recent-releases-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadBigDebutAlbums() {
        // This section is currently a placeholder as the service method returns an empty list.
        // Once the service method is implemented, this can be uncommented and used.
        Platform.runLater(() -> {
            bigDebutAlbumsSection.setVisible(false);
            bigDebutAlbumsSection.setManaged(false);
        });
        /*
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getBigDebutAlbums();
                Platform.runLater(() -> {
                    renderAlbums(bigDebutAlbumsGrid, FXCollections.observableArrayList(albums));
                    if (!albums.isEmpty()) {
                        bigDebutAlbumsSection.setVisible(true);
                        bigDebutAlbumsSection.setManaged(true);
                    } else {
                        bigDebutAlbumsSection.setVisible(false);
                        bigDebutAlbumsSection.setManaged(false);
                    }
                });
            } catch (Exception e) {
                Logger.error("Error loading big debut albums: " + e.getMessage());
                Platform.runLater(() -> {
                    bigDebutAlbumsSection.setVisible(false);
                    bigDebutAlbumsSection.setManaged(false);
                });
            }
        }, "debut-albums-loader");
        thread.setDaemon(true);
        thread.start();
        */
    }

    private void loadDevelopersPick() {
        Thread thread = new Thread(() -> {
            try {
                List<Album> albums = service.getDevelopersPickAlbums();
                Platform.runLater(() -> {
                    renderAlbums(developersPickGrid, FXCollections.observableArrayList(albums));
                    if (!albums.isEmpty()) {
                        developersPickSection.setVisible(true);
                        developersPickSection.setManaged(true);
                    } else {
                        developersPickSection.setVisible(false);
                        developersPickSection.setManaged(false);
                    }
                });
            } catch (Exception e) {
                Logger.error("Error loading developer's pick albums: " + e.getMessage());
                Platform.runLater(() -> {
                    developersPickSection.setVisible(false);
                    developersPickSection.setManaged(false);
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

    public List<Album> searchAlbums(String query) {
        // This method is primarily for local filtering if allAlbums contains enough data,
        // but with API calls, handleSearch will directly use the service.
        return List.of(); // No longer used for primary search
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
            // If search bar is empty, show the home page feed again
            albumGrid.getChildren().clear(); // Clear search results grid
            loadHomePageFeed(); // Reload the categorized feed
            return;
        }

        if (service == null) {
            // This case should ideally not happen if service is initialized
            renderAlbums(albumGrid, FXCollections.observableArrayList());
            return;
        }

        // Hide categorized sections when searching
        recentBigReleasesSection.setVisible(false);
        recentBigReleasesSection.setManaged(false);
        bigDebutAlbumsSection.setVisible(false);
        bigDebutAlbumsSection.setManaged(false);
        developersPickSection.setVisible(false);
        developersPickSection.setManaged(false);


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
