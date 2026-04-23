package org.example.service;

import org.example.domain.User;
import org.example.repository.UserRepository;
import org.example.utils.IObserver;

public interface IService {
    public Iterable<User> getAllUsers();
    public User login(String username, String password, IObserver client) throws Exception;
    public void register(String username, String password, String email);
    public void logout(User user, IObserver client) throws Exception;
}
