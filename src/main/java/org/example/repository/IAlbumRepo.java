package org.example.repository;

import org.example.domain.Album;

import java.util.List;

public interface IAlbumRepo {
    public void save(Album album);
    public List<Album> getAll();
}
