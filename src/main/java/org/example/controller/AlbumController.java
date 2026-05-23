package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.domain.Album;
import org.example.domain.Review;
import org.example.service.Service;
import org.example.utils.AppContext;
import org.example.utils.ImageCache;
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
    @FXML private ListView<Review> reviewsList;

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
        reviewsList.setCellFactory(list -> new ReviewCell());
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
            coverImage.setImage(ImageCache.get(album.getCoverUrl()));
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
            reviewsList.getItems().add(review);
        }

        if (reviewsList.getItems().isEmpty()) {
            reviewsList.setPlaceholder(new Label("No reviews yet."));
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Album action");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ReviewCell extends ListCell<Review> {
        @Override
        protected void updateItem(Review review, boolean empty) {
            super.updateItem(review, empty);

            if (empty || review == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            ImageView avatar = new ImageView();
            avatar.setFitWidth(42);
            avatar.setFitHeight(42);
            avatar.setPreserveRatio(false);
            avatar.getStyleClass().add("review-avatar");

            StackPane avatarFrame = new StackPane();
            avatarFrame.getStyleClass().add("review-avatar-frame");
            avatarFrame.setMinSize(42, 42);
            avatarFrame.setPrefSize(42, 42);
            avatarFrame.setMaxSize(42, 42);

            if (review.getUserPfpUrl() != null && !review.getUserPfpUrl().isBlank()) {
                avatar.setImage(ImageCache.get(review.getUserPfpUrl()));
                if (avatar.getImage() != null) {
                    avatarFrame.getChildren().add(avatar);
                }
            }

            if (avatarFrame.getChildren().isEmpty()) {
                Label initial = new Label(review.getUsername() == null || review.getUsername().isBlank()
                        ? "?"
                        : review.getUsername().substring(0, 1).toUpperCase());
                initial.getStyleClass().add("review-avatar-initial");
                avatarFrame.getChildren().add(initial);
            }

            Label username = new Label(review.getUsername());
            username.getStyleClass().add("review-username");

            Label stars = new Label(formatStars(review.getRating()) + "  " + String.format("%.1f", review.getRating()));
            stars.getStyleClass().add("review-stars");

            Label body = new Label(review.getReviewText() == null || review.getReviewText().isBlank()
                    ? "No written review."
                    : review.getReviewText());
            body.setWrapText(true);
            body.getStyleClass().add("review-body");

            HBox header = new HBox(8, username, stars);
            header.getStyleClass().add("review-header");

            VBox content = new VBox(5, header, body);
            HBox card = new HBox(12, avatarFrame, content);
            card.getStyleClass().add("review-card");

            setText(null);
            setGraphic(card);
        }

        private static String formatStars(double rating) {
            StringBuilder builder = new StringBuilder();
            int fullStars = (int) Math.floor(rating);
            boolean halfStar = rating - fullStars >= 0.5;

            for (int i = 1; i <= 5; i++) {
                if (i <= fullStars) {
                    builder.append("★");
                } else if (i == fullStars + 1 && halfStar) {
                    builder.append("½");
                } else {
                    builder.append("☆");
                }
            }

            return builder.toString();
        }
    }
}
