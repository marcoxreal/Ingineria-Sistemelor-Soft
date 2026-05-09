package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.domain.Album;
import org.example.domain.Review;
import org.example.service.Service;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;

public class AlbumController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label ratingLabel;
    @FXML private ImageView coverImage;
    @FXML private TextField searchBar;
    @FXML private Label listenedLabel;
    @FXML private Spinner<Integer> ratingSpinner;
    @FXML private TextArea reviewTextArea;
    @FXML private ListView<String> reviewsList;

    private Album currentAlbum;
    private Service service;

    @FXML
    public void initialize() {
        this.service = AppContext.service;
        ratingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));
    }

    public void setAlbum(Album album) {
        this.currentAlbum = album;
        AppContext.lastViewedAlbum = album;

        titleLabel.setText(displayText(album.getTitle(), "Unknown Album"));
        artistLabel.setText(displayText(album.getArtist(), "Unknown Artist"));

        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            try {
                coverImage.setImage(new Image(album.getCoverUrl(), true));
            } catch (IllegalArgumentException e) {
                coverImage.setImage(null);
            }
        } else {
            coverImage.setImage(null);
        }

        refreshAlbumState();
    }

    @FXML
    public void handleMarkListened() {
        try {
            service.markAlbumListened(AppContext.currentUser, currentAlbum);
            refreshAlbumState();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleLogAlbum() {
        try {
            service.logAlbum(AppContext.currentUser, currentAlbum);
            refreshAlbumState();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleSubmitReview() {
        try {
            service.saveReview(
                    AppContext.currentUser,
                    currentAlbum,
                    ratingSpinner.getValue(),
                    reviewTextArea.getText()
            );
            reviewTextArea.clear();
            refreshAlbumState();
        } catch (Exception e) {
            showError(e.getMessage());
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

    private void refreshAlbumState() {
        if (service == null || currentAlbum == null) {
            return;
        }

        boolean listened = service.hasListened(AppContext.currentUser, currentAlbum);
        listenedLabel.setText(listened ? "Listened" : "Not listened yet");

        double averageRating = service.getAlbumAverageRating(currentAlbum);
        ratingLabel.setText(averageRating > 0
                ? "Average rating " + String.format("%.1f", averageRating) + "/5"
                : "No ratings yet");

        reviewsList.getItems().clear();
        for (Review review : service.getRecentReviews(currentAlbum)) {
            reviewsList.getItems().add(
                    review.getUsername()
                            + " rated " + review.getRating() + "/5"
                            + (review.getReviewText() == null || review.getReviewText().isBlank()
                            ? ""
                            : ": " + review.getReviewText())
            );
        }

        if (reviewsList.getItems().isEmpty()) {
            reviewsList.getItems().add("No reviews yet.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Album action");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
