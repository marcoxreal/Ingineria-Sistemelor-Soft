package org.example.repository.factory;

import org.example.domain.User;
import org.example.repository.FollowRepository;
import org.example.repository.Repository;
import org.example.repository.UserRepository;

public class RepositoryFactory {
    private static final RepositoryFactory instance = new RepositoryFactory();
    private String url;
    private String username;
    private String password;

    private static Repository<Integer, User> createUserRepository(){
        return new UserRepository("jdbc:postgresql://localhost:5432/tuneboxd",
                "postgres", "123skem2");
    }

    public FollowRepository createFollowRepository(){
        return new FollowRepository("jdbc:postgresql://localhost:5432/tuneboxd",
                "postgres", "123skem2");
    }

    public Repository createRepository(RepositoryEntity repositoryEntity) {
        switch (repositoryEntity) {
            case USERS:
                return createUserRepository();

            default:
                return null;
        }
    }

    /**
     * Method that retuns the instance of the repository factory
     * @return the repository factory
     */
    public static RepositoryFactory getInstance() {
        return instance;
    }
}
