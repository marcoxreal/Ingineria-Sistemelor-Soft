package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import org.example.domain.Album;
import org.example.domain.User;
import org.example.service.Service;
import org.example.utils.AlbumCardFactory;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeController {

    @FXML private TextField searchBar;
    @FXML private TextField userSearchBar;

    @FXML
    private TilePane albumGrid;

    private ObservableList<Album> allAlbums;

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
        loadMockFeed();
    }

    private void loadMockFeed() {
        allAlbums = FXCollections.observableArrayList(
                new Album(null, "local:to-pimp-a-butterfly:kendrick-lamar", "To Pimp a Butterfly", "Kendrick Lamar", null, "Album", ""),
                new Album(null, "local:in-rainbows:radiohead", "In Rainbows", "Radiohead", null, "Album", "https://upload.wikimedia.org/wikipedia/en/2/2e/In_Rainbows_Official_Cover.jpg"),
                new Album(null, "local:rust-in-peace:megadeth", "Rust In Peace", "Megadeth", null, "Album", ""),
                new Album(null, "local:master-of-puppets:metallica", "Master of Puppets", "Metallica", null, "Album", ""),
                new Album(null, "local:blonde:frank-ocean", "Blonde", "Frank Ocean", null, "Album", ""),
                new Album(null, "local:currents:tame-impala", "Currents", "Tame Impala", null, "Album", "")
        );

        renderAlbums(allAlbums);
    }

    private void renderAlbums(ObservableList<Album> albums) {
        albumGrid.getChildren().clear();

        for (Album album : albums) {
            if (album != null) {
                albumGrid.getChildren().add(AlbumCardFactory.createAlbumCard(album));
            }
        }
    }

    public List<Album> searchAlbums(String query) {

        final String finalQuery = query == null ? "" : query.toLowerCase();

        return allAlbums.stream()
                .filter(album ->
                        AlbumCardFactory.displayText(album.getTitle(), "").toLowerCase().contains(finalQuery)
                                ||
                                AlbumCardFactory.displayText(album.getArtist(), "").toLowerCase().contains(finalQuery)
                )
                .toList();
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
            renderAlbums(allAlbums);
            return;
        }

        if (service == null) {
            renderAlbums(FXCollections.observableArrayList(searchAlbums(query)));
            return;
        }

        Thread searchThread = new Thread(() -> {
            List<Album> searchResults;
            try {
                searchResults = service.searchAlbums(query);
            } catch (Exception e) {
                searchResults = List.of();
            }
            final List<Album> results = searchResults;

            Platform.runLater(() -> {
                if (requestVersion == searchVersion.get()) {
                    renderAlbums(FXCollections.observableArrayList(results));
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
