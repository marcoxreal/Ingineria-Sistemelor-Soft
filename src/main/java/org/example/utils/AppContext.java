package org.example.utils;

import org.example.service.Service;
import org.example.domain.Album;
import org.example.domain.User;

public class AppContext {
    public static Service service;
    public static User currentUser;
    public static Album lastViewedAlbum;
}
