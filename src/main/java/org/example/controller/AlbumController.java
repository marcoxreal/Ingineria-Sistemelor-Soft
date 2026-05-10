package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.example.domain.Album;
import org.example.domain.Review;
import org.example.service.Service;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;

import java.util.Arrays;
import java.util.List;

public class AlbumController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label ratingLabel;
    @FXML private ImageView coverImage;
    @FXML private TextField searchBar;
    @FXML private Label listenedLabel;
    @FXML private TextArea reviewTextArea;
    @FXML private ListView<String> reviewsList;

    @FXML private ImageView star1;
    @FXML private ImageView star2;
    @FXML private ImageView star3;
    @FXML private ImageView star4;
    @FXML private ImageView star5;

    private Album currentAlbum;
    private Service service;
    private double currentRating = 0.0;

    private Image starFull;
    private Image starHalf;
    private Image starEmpty;

    @FXML
    public void initialize() {
        this.service = AppContext.service;
        loadStarImages();
        updateStarVisuals(); // Initialize stars to empty
    }

    private void loadStarImages() {
        try {
            starFull = new Image(getClass().getResourceAsStream("/org/example/images/star-full.png"));
            starHalf = new Image(getClass().getResourceAsStream("/org/example/images/star-half.png"));
            starEmpty = new Image(getClass().getResourceAsStream("/org/example/images/star-empty.png"));
        } catch (Exception e) {
            System.err.println("Error loading star images: " + e.getMessage());
            // Fallback or display an error to the user
        }
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
        if (currentRating == 0.0) {
            showError("Please select a rating before submitting a review.");
            return;
        }
        try {
            service.saveReview(
                    AppContext.currentUser,
                    currentAlbum,
                    currentRating,
                    reviewTextArea.getText()
            );
            reviewTextArea.clear();
            currentRating = 0.0; // Reset rating after submission
            updateStarVisuals();
            refreshAlbumState();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleStarClick(MouseEvent event) {
        ImageView clickedStar = (ImageView) event.getSource();
        int starIndex = Integer.parseInt(clickedStar.getId().replace("star", "")); // 1-5

        double clickX = event.getX();
        double starWidth = clickedStar.getFitWidth();

        if (starWidth == 0) { // Fallback if fitWidth is not set yet
            starWidth = clickedStar.getImage() != null ? clickedStar.getImage().getWidth() : 20; // Assume default width
        }

        double newRating;
        if (clickX < starWidth / 2) {
            newRating = starIndex - 0.5;
        } else {
            newRating = starIndex;
        }

        if (newRating == currentRating) {
            // If clicking the same rating again, reset to 0
            currentRating = 0.0;
        } else {
            currentRating = newRating;
        }
        updateStarVisuals();
    }

    private void updateStarVisuals() {
        List<ImageView> stars = Arrays.asList(star1, star2, star3, star4, star5);
        for (int i = 0; i < stars.size(); i++) {
            ImageView star = stars.get(i);
            star.setFitWidth(20); // Ensure stars have a consistent size
            star.setFitHeight(20);

            if (currentRating >= i + 1) {
                star.setImage(starFull);
            } else if (currentRating >= i + 0.5) {
                star.setImage(starHalf);
            } else {
                star.setImage(starEmpty);
            }
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

        // Set user's previous rating
        Review userReview = service.getUserReviewForAlbum(AppContext.currentUser, currentAlbum);
        if (userReview != null) {
            currentRating = userReview.getRating();
            reviewTextArea.setText(userReview.getReviewText());
        } else {
            currentRating = 0.0;
            reviewTextArea.clear();
        }
        updateStarVisuals();


        reviewsList.getItems().clear();
        for (Review review : service.getRecentReviews(currentAlbum)) {
            reviewsList.getItems().add(
                    review.getUsername()
                            + " rated " + String.format("%.1f", review.getRating()) + "/5"
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
