package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.utils.AppContext;
import org.example.utils.SceneManager;
import org.example.service.Service;
import org.example.utils.IObserver;

import javafx.event.ActionEvent;

public class LoginController {

    private Service service;

    @FXML
    private TextField username;

    @FXML
    private PasswordField password;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        this.service = AppContext.service;
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            service.login(
                    username.getText(),
                    password.getText(),
                    new IObserver() {
                        @Override
                        public void updateParticipanti() {
                            // ignore for now
                        }
                    }
            );

            SceneManager.switchScene("/org/example/home-view.fxml");

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Login error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleGoToRegister(ActionEvent event) {
        SceneManager.switchScene("/org/example/register-view.fxml");
    }
}