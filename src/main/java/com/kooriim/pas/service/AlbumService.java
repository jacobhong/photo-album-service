//package com.kooriim.pas.service;
//
//import com.kooriim.pas.domain.Photo;
//import com.kooriim.pas.repository.AlbumRepository;
//import com.kooriim.pas.repository.PhotoRepository;
//import com.kooriim.pas.domain.Album;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//
//@Service
//public class AlbumService {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  @Autowired
//  private AlbumRepository albumRepository;
//
//  @Autowired
//  private PhotoRepository photoRepository;
//
//  @Autowired
//  private PhotoService photoService;
//
//  public List<Album> getAlbums(Pageable pageable) {
//    final var albums = albumRepository.findByGoogleId(SecurityContextHolder.getContext().getAuthentication().getName(), pageable);
//    albums.forEach(album -> {
//      final var photosOptional = getPhotosByAlbumId(album.getId());
//      if (photosOptional.isPresent()) {
//        final var preview = new ArrayList<Photo>();
//        final var photos = photosOptional.get();
//        for (int i = 0; i <= 1 && i < photos.size(); i++) {
//          photoService.setBase64Thumbnail(photos.get(i));
//          preview.add(photos.get(i));
//        }
//        album.setPreviewPhotos(preview);
//      }
//    });
//    return albums;
//  }
//
////  public Album getAlbumById(Integer albumId, Map<String, String> queryParams) {
////    final var album = albumRepository.findById(albumId);
////    if (album.isPresent()) {
////      if (queryParams.containsKey("withPhotos") && queryParams.get("withPhotos").equalsIgnoreCase("true")) {
////        final var photoIds = getPhotosByAlbumId(albumId);
////        album.get().setPreviewPhotos(photoIds);
////      }
////      return album.get();
////    }
////    throw new RuntimeException("No album found with id " + albumId);
////  }
//
//  private Optional<List<Photo>> getPhotosByAlbumId(Integer albumId) {
//
//    return photoRepository.getPhotosByAlbumId(albumId, PageRequest.of(0, 2));
////    final var photosOptional = photoRepository.getPhotosByAlbumId(albumId);
////    final var photos = new ArrayList<Photo>();
////    if (photosOptional.isPresent()) {
////      photos.addAll(photosOptional.get());
////      logger.info("Found photos {} for albumID {}", photos, albumId);
////    }
////    return photos;
//  }
//
//  @Transactional
//  public Album saveOrUpdateAlbum(Album album) {
//    album.setGoogleId(SecurityContextHolder.getContext().getAuthentication().getName());
//    final var savedAlbum = this.albumRepository.save(album);
//    logger.info("saved album: {} id: {}", savedAlbum.getTitle(), savedAlbum.getId());
//    return savedAlbum;
//  }
//
//  @Transactional
//  public void addPhotosToAlbum(Integer albumId, List<Integer> ids) {
//    photoRepository.deleteAllPhotosByAlbumId(albumId);
//    ids.forEach(id -> {
//      albumRepository.savePhotoAlbum(albumId, id);
//      logger.info("saved record to photo_album, albumId: {} | photoId: {}", albumId, id);
//    });
//  }
//
//  @Transactional
//  public void deleteAlbum(Integer albumId) {
//    this.photoRepository.deleteAllPhotosByAlbumId(albumId);
//    this.albumRepository.deleteById(albumId);
//  }
//}
