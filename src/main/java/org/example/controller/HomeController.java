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
import org.example.domain.AlbumTest;
import org.example.service.Service;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeController {

    @FXML private TextField searchBar;

    @FXML
    private TilePane albumGrid;

    private ObservableList<AlbumTest> allAlbums;

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
                new AlbumTest("To Pimp a Butterfly", "Kendrick Lamar", "", 4.6),
                new AlbumTest("In Rainbows", "Radiohead", "https://upload.wikimedia.org/wikipedia/en/2/2e/In_Rainbows_Official_Cover.jpg", 4.7),
                new AlbumTest("Rust In Peace", "Megadeth", "", 5.0),
                new AlbumTest("Master of Puppets", "Metallica", "", 4.8),
                new AlbumTest("Blonde", "Frank Ocean", "", 4.9),
                new AlbumTest("Currents", "Tame Impala", "", 4.5)
        );

        renderAlbums(allAlbums);
    }

    private void renderAlbums(ObservableList<AlbumTest> albums) {
        albumGrid.getChildren().clear();

        for (AlbumTest album : albums) {
            if (album != null) {
                albumGrid.getChildren().add(createAlbumCard(album));
            }
        }
    }

    private VBox createAlbumCard(AlbumTest album) {

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

        Label rating = new Label("Rating " + album.getRating());
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

    private void openAlbumPage(AlbumTest album) {
        AlbumController controller = (AlbumController)
                SceneManager.switchScene("/org/example/album-view.fxml");

        controller.setAlbum(album);
    }

    public List<AlbumTest> searchAlbums(String query) {

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
            List<AlbumTest> searchResults;
            try {
                searchResults = service.searchAlbums(query);
            } catch (Exception e) {
                searchResults = List.of();
            }
            final List<AlbumTest> results = searchResults;

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

}
