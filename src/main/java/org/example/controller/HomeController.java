package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.domain.AlbumTest;
import org.example.utils.SceneManager;

public class HomeController {

    @FXML private TextField searchBar;

    @FXML
    private TilePane albumGrid;

    private ObservableList<AlbumTest> allAlbums;

    public void initialize() {
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
            albumGrid.getChildren().add(createAlbumCard(album));
        }
    }

    private VBox createAlbumCard(AlbumTest album) {

        ImageView cover = new ImageView();
        cover.setFitWidth(140);
        cover.setFitHeight(140);
        cover.setPreserveRatio(false);

        // Load image if available
        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            try {
                Image img = new Image(album.getCoverUrl(), false);
                cover.setImage(img);
            } catch (Exception e) {
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
        Label title = new Label(album.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-weight: bold;");

        Label artist = new Label(album.getArtist());
        artist.setStyle("-fx-text-fill: #666;");

        // Rating
        Label rating = new Label("★ " + album.getRating());
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

        // Hover effect (slightly nicer)
        card.setOnMouseEntered(e ->
                card.setScaleX(1.05));
        card.setOnMouseExited(e ->
                card.setScaleX(1.0));

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

    @FXML
    public void handleSearch() {
        String query = searchBar.getText().toLowerCase();

        ObservableList<AlbumTest> filtered = FXCollections.observableArrayList();

        for (AlbumTest album : allAlbums) {
            if (album.getTitle().toLowerCase().contains(query) ||
                    album.getArtist().toLowerCase().contains(query)) {
                filtered.add(album);
            }
        }

        renderAlbums(filtered);
    }
}
