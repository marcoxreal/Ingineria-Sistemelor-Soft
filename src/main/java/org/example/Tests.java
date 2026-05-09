package org.example;

import org.example.api.MusicBrainzClient;
import org.example.repository.AlbumRepository;
import org.example.service.AlbumImportService;

public class Tests {
    public static void main(String[] args) throws Exception {
        MusicBrainzClient client = new MusicBrainzClient();

        AlbumRepository albumRepository =
                new AlbumRepository("jdbc:postgresql://localhost:5432/tuneboxd", "postgres", "123skem2");

        AlbumImportService importService =
                new AlbumImportService(client, albumRepository);

        importService.importAlbums("artist:Radiohead");
    }
}
