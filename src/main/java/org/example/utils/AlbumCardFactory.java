package org.example.utils;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.example.domain.Album;
import org.example.controller.AlbumController;

public class AlbumCardFactory {

    public static VBox createAlbumCard(Album album) {
        ImageView cover = new ImageView();
        cover.setFitWidth(148);
        cover.setFitHeight(148);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("album-cover");

        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            cover.setImage(ImageCache.get(album.getCoverUrl()));
        }

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(148, 148);
        imageContainer.setMinSize(148, 148);
        imageContainer.setMaxSize(148, 148);
        imageContainer.getStyleClass().add("album-cover-frame");

        if (cover.getImage() != null) {
            imageContainer.getChildren().add(cover);
        } else {
            Label placeholder = new Label("No Image");
            placeholder.getStyleClass().add("muted-text");
            imageContainer.getChildren().add(placeholder);
        }

        Label title = new Label(displayText(album.getTitle(), "Unknown Album"));
        title.setWrapText(true);
        title.setMaxWidth(148);
        title.setTextAlignment(TextAlignment.LEFT);
        title.getStyleClass().add("album-card-title");

        Label artist = new Label(displayText(album.getArtist(), "Unknown Artist"));
        artist.setWrapText(true);
        artist.setMaxWidth(148);
        artist.getStyleClass().add("album-card-artist");

        VBox info = new VBox(3, title, artist);

        VBox card = new VBox(8, imageContainer, info);
        card.setPrefWidth(172);
        card.setMinWidth(172);
        card.setMaxWidth(172);
        card.getStyleClass().add("album-card");

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
