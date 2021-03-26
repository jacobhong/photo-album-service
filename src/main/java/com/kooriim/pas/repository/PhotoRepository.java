package com.kooriim.pas.repository;

import com.kooriim.pas.domain.MediaItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class PhotoRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  public Mono<MediaItem> getPhotoById(Integer id) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where id = :id", MediaItem.class)
                                     .setParameter("id", id)
                                     .getSingleResult())
             .cast(MediaItem.class)
             .doOnNext(photo -> logger.info("Got photo {}", photo.getTitle()))
             .doOnError(error -> logger.error("Error getting photo {}", error.getMessage()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getPhotosByAlbumId(Integer albumId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item mi INNER JOIN media_item_album mia ON mi.id=mia.media_item_id WHERE mia.album_id=:id", MediaItem.class)
                                                .setParameter("id", albumId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got photos by albumId {}", albumId))
             .doOnError(error -> logger.error("Error getting photos by albumId {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getPhotosByGoogleId(String googleId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item WHERE google_id=:google_id order by created desc", MediaItem.class)
                                                .setParameter("google_id", googleId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got photos by googleId {}", googleId))
             .doOnError(error -> logger.error("Error getting photos by googleId {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<MediaItem> getPhotoTitle(String googleId, String filename) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where title = :title and google_id = :googleId", MediaItem.class)
                                     .setParameter("title", filename)
                                     .setParameter("googleId", googleId)
                                     .getSingleResult())
             .cast(MediaItem.class)
             .doOnNext(photo -> logger.info("Got photo by title {}", photo.getTitle()))
             .doOnError(error -> logger.error("Error getting photo by title {}", error.getMessage()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<MediaItem> mediaItemExists(String googleId, String filename) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where title = :title and google_id = :googleId", MediaItem.class)
                                     .setParameter("googleId", googleId)
                                     .setParameter("title", filename)
                                     .getSingleResult())
             .cast(MediaItem.class)
             .doOnNext(photo -> logger.info("Check if mediaItem exists {} {}", filename, photo))
//             .doOnError(error -> logger.error("Error Check if mediaItem exists {} {}", filename, error.getMessage()))
             .onErrorResume(e -> Mono.just(new MediaItem()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getPhotosByIds(List<Integer> ids) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item WHERE id in (:ids)", MediaItem.class)
                                                .setParameter("ids", ids)
                                                .getResultList()))
             .doOnNext(result -> logger.info("Got photoById {}", ids.toString()))
             .doOnError(error -> logger.error("Error getting photosById {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getPhotos() {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item", MediaItem.class)
                                                .getResultList()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteByPhotoIds(List<Integer> ids) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("DELETE media_item_album, media_item FROM media_item_album RIGHT JOIN media_item ON media_item_album.media_item_id=media_item.id WHERE media_item.id in (:ids)")
                           .setParameter("ids", ids)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Deleted photosById {}", ids.toString()))
             .doOnError(error -> logger.error("Error deleting photosByIds {}", error))
             .then()
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Integer> deleteAllPhotosByAlbumId(Integer albumId) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("DELETE FROM media_item_album where album_id = :id")
                           .setParameter("id", albumId)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Deleted all photos by albumId {}", albumId))
             .doOnError(error -> logger.error("Error deleting photos {} ", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }


  public Mono<MediaItem> save(MediaItem mediaItem) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.merge(mediaItem);
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Saved mediaItem {}", result.getTitle()))
             .doOnError(error -> logger.error("Error saving mediaItem {}", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }


}

