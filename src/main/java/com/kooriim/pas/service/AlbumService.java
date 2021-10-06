package com.kooriim.pas.service;

import com.kooriim.pas.domain.MediaItem;
import com.kooriim.pas.repository.AlbumRepository;
import com.kooriim.pas.repository.MediaItemRepository;
import com.kooriim.pas.domain.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AlbumService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private MediaItemRepository mediaItemRepository;

  @Autowired
  private MediaItemService mediaItemService;

  public Flux<Album> getAlbums(Pageable pageable) {
    return ReactiveSecurityContextHolder
             .getContext()
             .map(SecurityContext::getAuthentication)
             .map(Authentication::getName)
             .doOnNext(name -> logger.info("getting photos for googleId {}", name))
             .flatMapMany(name -> albumRepository.findByGoogleId(name, pageable)
                                    .flatMap(album -> {
                                      final var preview = new ArrayList<MediaItem>();
                                      return getPhotosByAlbumId(album.getId())
                                               .flatMap(photo -> mediaItemService.setBase64Thumbnail(photo)
                                                                   .flatMap(a -> {
                                                                     preview.add(a);
                                                                     return Mono.just(a);
                                                                   }))
                                               .collectList()
                                               .map(p -> {
                                                 album.setPreviewMediaItems(p);
                                                 return album;
                                               });
                                    }));
  }

//  public Mono<Album> getAlbumById(Integer albumId, Map<String, String> queryParams) {
//    return albumRepository.getAlbumById(albumId).map(album -> {
//      if (queryParams.containsKey("withPhotos") && queryParams.get("withPhotos").equalsIgnoreCase("true")) {
//        final var preview = new ArrayList<MediaItem>();
//        album.setPreviewMediaItems(preview);
//        getMediaItemsByAlbumId(albumId)
//          .collectList()
//          .map(photos -> {
//            photos.forEach(photo -> {
//              mediaItemService.setBase64Thumbnail(photo)
//                .map(p -> {
//                  preview.add(p);
//                  return p;
//                });
//            });
//            return album;
//          });
////        return album;
//      } else {
//        return Mono.empty();
//      }
//    });
//  }

  private Flux<MediaItem> getPhotosByAlbumId(Integer albumId) {
    return mediaItemRepository.getMediaItemsByAlbumId(albumId, PageRequest.of(0, 4));
  }

  public Mono<Album> saveOrUpdateAlbum(Album album) {
    return ReactiveSecurityContextHolder
             .getContext()
             .map(SecurityContext::getAuthentication)
             .map(Authentication::getName)
             .doOnNext(name -> logger.info("getting photos for googleId {}", name))
             .flatMap(name -> {
               album.setGoogleId(name);
               return albumRepository.save(album);
             });
  }

  public Mono<Void> addPhotosToAlbum(Integer albumId, List<Integer> ids) {
    ids.forEach(id -> albumRepository.savePhotoAlbum(albumId, id)
                        .subscribe());
    return Mono.empty();
  }

  public Mono<Void> movePhotosToAlbum(Integer fromAlbumId, String toAlbum, List<Integer> ids) {
    return this.mediaItemRepository.deleteMediaItemAlbumByIds(fromAlbumId, ids)
             .flatMap(result -> {
               ids.forEach(id -> albumRepository.savePhotoAlbumByAlbumTitle(toAlbum, id).subscribe());
               return Mono.empty();
             }).then();
  }

  public Mono<Void> deleteAlbum(Integer albumId) {
    return this.mediaItemRepository.deleteAllMediaItemsByAlbumId(albumId)
             .flatMap(result -> this.albumRepository.deleteById(albumId).then())
             .then();

  }
}
