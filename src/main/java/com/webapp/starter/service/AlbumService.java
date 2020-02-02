package com.webapp.starter.service;

import com.webapp.starter.domain.Album;
import com.webapp.starter.repository.AlbumRepository;
import com.webapp.starter.repository.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

//  public Set<Photo> getPhotosByAlbumId(Integer id) {
//    return photoRepository.getPhotosByAlbumId(id);
//  }

  public List<Album> getAlbums() {
    return albumRepository.findAll();
  }

  @Transactional
  public Album saveAlbum(Album album) {
    final var savedAlbum = this.albumRepository.save(album);

    album.getPhotoIds().forEach(id -> {
      albumRepository.savePhotoAlbum(savedAlbum.getId(), id);
      logger.info("saved record to photo_album, albumId: {} | photoId: {}", savedAlbum.getId(), id);
    });
    return savedAlbum;
  }
}
