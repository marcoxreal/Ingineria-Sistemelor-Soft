package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.domain.User;
import org.example.repository.FollowRepository;
import org.example.repository.Repository;
import org.example.repository.factory.RepositoryFactory;
import org.example.service.Service;
import org.example.utils.SceneManager;

import static org.example.repository.factory.RepositoryEntity.USERS;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) {

        try {
            Repository<Integer, User> userRepository =
                    RepositoryFactory.getInstance().createRepository(USERS);
            FollowRepository followRepository =
                    RepositoryFactory.getInstance().createFollowRepository();
            Service service = new Service(userRepository, followRepository);
            SceneManager.init(stage);
            org.example.utils.AppContext.service = service;
            SceneManager.switchScene("/org/example/login-view.fxml");
            stage.setTitle("Tuneboxd");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}