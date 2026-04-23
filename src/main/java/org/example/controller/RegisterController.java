package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;
import org.example.service.Service;

public class RegisterController {

    private Service service;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void initialize() {
        this.service = AppContext.service;
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        try {
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("All fields are required");
            }

            service.register(username, password, email);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Account created successfully!");
            alert.showAndWait();

            SceneManager.switchScene("/org/example/login-view.fxml");

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Registration failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleGoToLogin() {
        SceneManager.switchScene("/org/example/login-view.fxml");
    }
}