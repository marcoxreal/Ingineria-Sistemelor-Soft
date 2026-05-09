package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.domain.AlbumTest;
import org.example.utils.SceneManager;

public class AlbumController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label ratingLabel;
    @FXML private ImageView coverImage;
    @FXML private TextField searchBar;

    private AlbumTest currentAlbum;

    public void setAlbum(AlbumTest album) {
        this.currentAlbum = album;

        titleLabel.setText(displayText(album.getTitle(), "Unknown Album"));
        artistLabel.setText(displayText(album.getArtist(), "Unknown Artist"));
        ratingLabel.setText("Rating " + album.getRating());

        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            try {
                coverImage.setImage(new Image(album.getCoverUrl(), true));
            } catch (IllegalArgumentException e) {
                coverImage.setImage(null);
            }
        } else {
            coverImage.setImage(null);
        }
    }

    @FXML
    public void handleSearch() {
        HomeController controller = SceneManager.switchScene("/org/example/home-view.fxml");
        controller.startSearch(searchBar.getText());
    }

    @FXML
    public void handleBack() {
        SceneManager.switchScene("/org/example/home-view.fxml");
    }

    private String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
