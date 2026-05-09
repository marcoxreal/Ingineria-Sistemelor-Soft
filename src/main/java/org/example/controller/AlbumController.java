package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.domain.AlbumTest;

public class AlbumController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label ratingLabel;
    @FXML private ImageView coverImage;
    @FXML private TextField searchBar;

    private AlbumTest currentAlbum;

    public void setAlbum(AlbumTest album) {
        this.currentAlbum = album;

        titleLabel.setText(album.getTitle());
        artistLabel.setText(album.getArtist());
        ratingLabel.setText("★ " + album.getRating());

        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            coverImage.setImage(new Image(album.getCoverUrl(), false));
        }
    }

    @FXML
    public void handleSearch() {
        System.out.println("Search from album page: " + searchBar.getText());
    }
}