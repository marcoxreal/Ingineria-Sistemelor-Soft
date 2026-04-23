package org.example.domain;

import java.util.List;

public class User extends Entity<Integer>{
    private String username;
    private String password;
    private String email;
    private String pfpUrl;
    public User(Integer id, String username, String password, String email, String pfpUrl) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
        this.pfpUrl = pfpUrl;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPfpUrl() {
        return pfpUrl;
    }
    public void setPfpUrl(String pfpUrl) {
        this.pfpUrl = pfpUrl;
    }
}