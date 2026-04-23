package org.example.service;

import org.example.domain.User;
import org.example.repository.FollowRepository;
import org.example.repository.Repository;
import org.example.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import org.example.utils.IObserver;
import org.example.utils.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class Service implements IService{
    private final Repository<Integer, User> userRepository;
    private final FollowRepository followRepository;

    private Map<Integer, IObserver> loggedClients;

    public Service(Repository<Integer, User> userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.loggedClients = new ConcurrentHashMap<>();
    }

    @Override
    public Iterable<User> getAllUsers() {
        return userRepository.getAll();
    }

    @Override
    public synchronized User login(String username, String password, IObserver client) throws Exception {
        User user = getUserByUsername(username);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            if (loggedClients.containsKey(user.getId()))
                throw new Exception("User is already logged in!");
            loggedClients.put(user.getId(), client);
            Logger.info("User " + username + " logged in.");
            return user;
        }
        throw new Exception("Wrong password or username!");
    }

    @Override
    public synchronized void logout(User user, IObserver client) throws Exception {
        IObserver localClient = loggedClients.remove(user.getId());
        if (localClient == null) throw new Exception("User is not logged in!");
    }

    @Override
    public synchronized void register(String username, String password, String email) {
        User user = getUserByUsername(username);
        if (user != null) throw new IllegalArgumentException("Username already exists!");
        user = new User(null, username, BCrypt.hashpw(password, BCrypt.gensalt()), email, null);
        userRepository.save(user);
    }

    private User getUserByUsername(String username) {
        return StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    private void notifyUsers() {
        for (IObserver client : loggedClients.values()) {
            try {
                client.updateParticipanti();
            } catch (Exception e) {
                System.err.println("Error notifying a client: " + e.getMessage());
            }
        }
    }

    public void followUser(int followerId, int followedId) {
        if (followerId == followedId) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        if (followRepository.isFollowing(followerId, followedId)) {
            throw new IllegalArgumentException("Already following this user");
        }

        followRepository.follow(followerId, followedId);

        notifyUsers();
    }

    public void unfollowUser(int followerId, int followedId) {
        if (!followRepository.isFollowing(followerId, followedId)) {
            throw new IllegalArgumentException("You are not following this user");
        }

        followRepository.unfollow(followerId, followedId);

        notifyUsers();
    }

    public List<Integer> getFollowers(int userId) {
        return followRepository.getFollowerIds(userId);
    }

    public List<Integer> getFollowing(int userId) {
        return followRepository.getFollowingIds(userId);
    }

    public boolean isFollowing(int followerId, int followedId) {
        return followRepository.isFollowing(followerId, followedId);
    }
}
