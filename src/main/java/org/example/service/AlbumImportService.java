package org.example.service;

import org.example.api.MusicBrainzClient;
import org.example.domain.Album;
import org.example.dto.Release;
import org.example.dto.ReleaseGroup;
import org.example.repository.AlbumRepository;
import org.example.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlbumImportService {

    private final MusicBrainzClient client;
    private final AlbumRepository albumRepository;

    public AlbumImportService(
            MusicBrainzClient client,
            AlbumRepository albumRepository
    ) {
        this.client = client;
        this.albumRepository = albumRepository;
    }

    public void importAlbums(String artistName) {

        try {

            var response = client.searchAlbums(artistName);

            List<ReleaseGroup> groups =
                    (response != null && response.getReleaseGroups() != null)
                            ? response.getReleaseGroups()
                            : List.of();

            Set<String> seen = new HashSet<>();

            for (ReleaseGroup group : groups) {

                if (group == null)
                    continue;

                String title = group.getTitle();
                if (title == null)
                    continue;

                if (!seen.add(title.toLowerCase()))
                    continue;

                String type = group.getPrimaryType();

                if (type == null || !type.equalsIgnoreCase("Album"))
                    continue;

                // NOTE: ReleaseGroup does NOT reliably contain artist
                String artist = artistName;

                Album album = new Album(
                        null,
                        group.getId(),
                        title,
                        artist,
                        null, // no reliable date here
                        type,
                        "https://coverartarchive.org/release-group/"
                                + group.getId()
                                + "/front"
                );

                albumRepository.save(album);

                Logger.info("Saved album: " + album.getTitle());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}