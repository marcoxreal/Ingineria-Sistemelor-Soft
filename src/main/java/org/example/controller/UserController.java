package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import org.example.domain.Album;
import org.example.domain.User;
import org.example.domain.UserActivity;
import org.example.service.Service;
import org.example.utils.AlbumCardFactory;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;

import java.io.File;

public class UserController {
    @FXML private ImageView profileImage;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label followersLabel;
    @FXML private Label followingLabel;
    @FXML private Label listenedLabel;
    @FXML private Label activityLabel;
    @FXML private ListView<String> activityList;
    @FXML private TilePane favoriteAlbumsGrid;
    @FXML private Label profilePlaceholder;
    @FXML private Button addFavoriteButton;
    @FXML private Button followButton;
    @FXML private HBox profilePictureEditor;
    @FXML private TextField profilePictureUrlField;

    private Service service;
    private User profileUser;

    @FXML
    public void initialize() {
        this.service = AppContext.service;
    }

    public void setUser(User user) {
        this.profileUser = user;
        renderUser();
    }

    @FXML
    public void handleBack() {
        SceneManager.switchScene("/org/example/home-view.fxml");
    }

    @FXML
    public void handleFollowToggle() {
        User currentUser = AppContext.currentUser;
        if (currentUser == null || profileUser == null || service == null) {
            showError("Follow is not available right now.");
            return;
        }

        try {
            if (service.isFollowing(currentUser.getId(), profileUser.getId())) {
                service.unfollowUser(currentUser.getId(), profileUser.getId());
            } else {
                service.followUser(currentUser.getId(), profileUser.getId());
            }
            renderFollowState();
            renderUserStats();
            renderRecentActivity();
            renderFavoriteAlbums();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleAddFavoriteAlbum() {
        if (!isOwnProfile()) {
            return;
        }

        Album album = AppContext.lastViewedAlbum;
        if (album == null) {
            showError("Open an album first, then add it to favorites from your profile.");
            return;
        }

        try {
            service.addFavoriteAlbum(AppContext.currentUser, album);
            renderFavoriteAlbums();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleSaveProfilePicture() {
        if (!isOwnProfile()) {
            return;
        }

        try {
            service.updateProfilePicture(AppContext.currentUser, profilePictureUrlField.getText());
            profileUser.setPfpUrl(AppContext.currentUser.getPfpUrl());
            renderProfileImage();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleChooseProfilePicture() {
        if (!isOwnProfile()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose profile picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(SceneManager.getStage());
        if (selectedFile == null) {
            return;
        }

        String imageUri = selectedFile.toURI().toString();
        profilePictureUrlField.setText(imageUri);
        profileImage.setImage(new Image(imageUri, true));
        profilePlaceholder.setVisible(false);
    }

    private void renderUser() {
        if (profileUser == null) {
            return;
        }

        usernameLabel.setText(profileUser.getUsername());
        emailLabel.setText(displayText(profileUser.getEmail(), "No email listed"));
        activityLabel.setText("");

        renderProfileImage();

        renderFollowState();
        renderUserStats();
        renderRecentActivity();
        renderFavoriteAlbums();
    }

    private void renderFollowState() {
        User currentUser = AppContext.currentUser;
        int followers = service == null ? 0 : service.getFollowers(profileUser.getId()).size();
        int following = service == null ? 0 : service.getFollowing(profileUser.getId()).size();

        followersLabel.setText(followers + " followers");
        followingLabel.setText(following + " following");

        if (currentUser == null || currentUser.getId().equals(profileUser.getId())) {
            followButton.setVisible(false);
            followButton.setManaged(false);
            return;
        }

        followButton.setVisible(true);
        followButton.setManaged(true);
        boolean isFollowing = service != null && service.isFollowing(currentUser.getId(), profileUser.getId());
        followButton.setText(isFollowing ? "Unfollow" : "Follow");
    }

    private void renderUserStats() {
        int listenedCount = service == null ? 0 : service.countListenedAlbums(profileUser);
        listenedLabel.setText("Listened to " + listenedCount + " albums");
    }

    private void renderRecentActivity() {
        activityList.getItems().clear();

        if (service != null) {
            for (UserActivity activity : service.getRecentActivity(profileUser)) {
                activityList.getItems().add(activity.getText());
            }
        }

        if (activityList.getItems().isEmpty()) {
            activityList.getItems().add("No recent activity yet.");
        }
    }

    private void renderFavoriteAlbums() {
        favoriteAlbumsGrid.getChildren().clear();

        if (service != null) {
            for (Album album : service.getFavoriteAlbums(profileUser)) {
                favoriteAlbumsGrid.getChildren().add(AlbumCardFactory.createAlbumCard(album));
            }
        }

        if (favoriteAlbumsGrid.getChildren().isEmpty()) {
            Label noAlbumsLabel = new Label("No favorite albums yet.");
            noAlbumsLabel.setStyle("-fx-text-fill: #999;");
            favoriteAlbumsGrid.getChildren().add(noAlbumsLabel);
        }

        boolean ownProfile = isOwnProfile();
        addFavoriteButton.setVisible(ownProfile);
        addFavoriteButton.setManaged(ownProfile);
        profilePictureEditor.setVisible(ownProfile);
        profilePictureEditor.setManaged(ownProfile);
    }

    private void renderProfileImage() {
        profilePictureUrlField.setText(displayText(profileUser.getPfpUrl(), ""));

        if (profileUser.getPfpUrl() != null && !profileUser.getPfpUrl().isBlank()) {
            try {
                profileImage.setImage(new Image(profileUser.getPfpUrl(), true));
                profilePlaceholder.setVisible(false);
            } catch (IllegalArgumentException e) {
                profileImage.setImage(null);
                profilePlaceholder.setVisible(true);
            }
        } else {
            profileImage.setImage(null);
            profilePlaceholder.setVisible(true);
        }
    }

    private boolean isOwnProfile() {
        return AppContext.currentUser != null
                && profileUser != null
                && AppContext.currentUser.getId().equals(profileUser.getId());
    }

    private String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("User profile");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
