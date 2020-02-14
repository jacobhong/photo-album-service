package com.kooriim.pas.service;

import com.kooriim.pas.repository.AlbumRepository;
import com.kooriim.pas.repository.PhotoRepository;
import com.kooriim.pas.domain.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class AlbumService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private PhotoRepository photoRepository;

  public List<Album> getAlbums() {
    return albumRepository.findByGoogleId(SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Transactional
  public Album saveAlbum(Album album) {
    album.setGoogleId(SecurityContextHolder.getContext().getAuthentication().getName());
    final var savedAlbum = this.albumRepository.save(album);

    album.getPhotoIds().forEach(id -> {
      albumRepository.savePhotoAlbum(savedAlbum.getId(), id);
      logger.info("saved record to photo_album, albumId: {} | photoId: {}", savedAlbum.getId(), id);
    });
    return savedAlbum;
  }
}
