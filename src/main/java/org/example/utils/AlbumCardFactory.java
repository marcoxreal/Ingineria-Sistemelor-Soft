package org.example.utils;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.domain.Album;
import org.example.controller.AlbumController;

public class AlbumCardFactory {

    public static VBox createAlbumCard(Album album) {
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

    private static void openAlbumPage(Album album) {
        AlbumController controller = (AlbumController)
                SceneManager.switchScene("/org/example/album-view.fxml");

        controller.setAlbum(album);
    }

    public static String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
