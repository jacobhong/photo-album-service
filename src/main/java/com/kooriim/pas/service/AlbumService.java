package com.kooriim.pas.service;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.repository.AlbumRepository;
import com.kooriim.pas.repository.PhotoRepository;
import com.kooriim.pas.domain.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

  public Album getAlbumById(Integer albumId, Boolean withPhotos) {
    final var album = albumRepository.findById(albumId);
    if (album.isPresent()) {
      if (withPhotos == true) {
        final var photoIds = getPhotoIdsByAlbumId(albumId);
        album.get().setPhotoIds(photoIds);
      }
      return album.get();
    }
    throw new RuntimeException("No album found with id " + albumId);
  }

  private Set<Integer> getPhotoIdsByAlbumId(Integer albumId) {
    final var photosOptional = photoRepository.getPhotosByAlbumId(albumId);
    var ids = new HashSet<Integer>();
    if (photosOptional.isPresent()) {
      final var photos = photosOptional.get();
      ids.addAll(photos
                   .stream()
                   .map(Photo::getId)
                   .collect(Collectors.toSet()));
      logger.info("Found ids {} for albumID {}", ids, albumId);
    }
    return ids;
  }

  @Transactional
  public Album saveAlbum(Album album) {
    album.setGoogleId(SecurityContextHolder.getContext().getAuthentication().getName());
    final var savedAlbum = this.albumRepository.save(album);
    logger.info("saved album: {} id: {}", savedAlbum.getTitle(), savedAlbum.getId());
    addPhotosToAlbum(album.getId(), album.getPhotoIds().stream().collect(Collectors.toList()));
    return savedAlbum;
  }

  @Transactional
  public void addPhotosToAlbum(Integer albumId, List<Integer> ids) {
    photoRepository.deleteAllPhotosByAlbumId(albumId);
    ids.forEach(id -> {
      albumRepository.savePhotoAlbum(albumId, id);
      logger.info("saved record to photo_album, albumId: {} | photoId: {}", albumId, id);
    });
  }
}
