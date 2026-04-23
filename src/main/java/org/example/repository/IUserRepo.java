package org.example.repository;

import org.example.domain.User;

public interface IUserRepo extends Repository<Integer, User> {
    /**
     * Returns the User that has the given username
     * @param username the username of the User to be returned (must not be null)
     * @return the User with the specified username, or null if not found
     * @throws IllegalArgumentException if the username is null
     */
    User findByUsername(String username);

    /**
     * Saves a new User to the repository
     * @param user the User to be saved
     */
    void save(User user);
}
