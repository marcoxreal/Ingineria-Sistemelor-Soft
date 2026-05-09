package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.domain.Album;
import org.example.domain.User;
import org.example.service.Service;
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
                albumGrid.getChildren().add(createAlbumCard(album));
            }
        }
    }

    private VBox createAlbumCard(Album album) {

        ImageView cover = new ImageView();
        cover.setFitWidth(140);
        cover.setFitHeight(140);
        cover.setPreserveRatio(false);

        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            try {
                Image img = new Image(album.getCoverUrl(), true);
                cover.setImage(img);
            } catch (IllegalArgumentException e) {
                cover.setImage(null);
            }
        }

        // Placeholder if no image
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(140, 140);

        if (cover.getImage() != null) {
            imageContainer.getChildren().add(cover);
        } else {
            Label placeholder = new Label("No Image");
            placeholder.setStyle("-fx-text-fill: #999;");
            imageContainer.setStyle("""
            -fx-background-color: #ddd;
            -fx-background-radius: 8;
        """);
            imageContainer.getChildren().add(placeholder);
        }

        // Title & artist
        Label title = new Label(displayText(album.getTitle(), "Unknown Album"));
        title.setWrapText(true);
        title.setStyle("-fx-font-weight: bold;");

        Label artist = new Label(displayText(album.getArtist(), "Unknown Artist"));
        artist.setStyle("-fx-text-fill: #666;");

        Label rating = new Label("Album");
        rating.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");

        VBox info = new VBox(3, title, artist, rating);

        VBox card = new VBox(8, imageContainer, info);
        card.setPrefWidth(150);

        card.setStyle("""
        -fx-background-color: white;
        -fx-padding: 10;
        -fx-background-radius: 10;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0.2, 0, 3);
    """);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });

        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> openAlbumPage(album));
        return card;
    }

    private void openAlbumPage(Album album) {
        AlbumController controller = (AlbumController)
                SceneManager.switchScene("/org/example/album-view.fxml");

        controller.setAlbum(album);
    }

    public List<Album> searchAlbums(String query) {

        final String finalQuery = query == null ? "" : query.toLowerCase();

        return allAlbums.stream()
                .filter(album ->
                        displayText(album.getTitle(), "").toLowerCase().contains(finalQuery)
                                ||
                                displayText(album.getArtist(), "").toLowerCase().contains(finalQuery)
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

    private String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showUserSearchError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("User search");
        alert.setContentText(message);
        alert.showAndWait();
    }

}
