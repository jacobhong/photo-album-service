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
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public class MediaItemRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  public Mono<MediaItem> getMediaItemById(Integer id) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where id = :id", MediaItem.class)
                                     .setParameter("id", id)
                                     .getSingleResult())
             .cast(MediaItem.class)
             .doOnNext(mediaItem -> logger.debug("Got mediaItem {}", mediaItem.getTitle()))
             .doOnError(error -> logger.error("Error getting mediaItem {}", error.getMessage()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getMediaItemsByAlbumId(Integer albumId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item mi INNER JOIN media_item_album mia ON mi.id=mia.media_item_id WHERE mia.album_id=:id", MediaItem.class)
                                                .setParameter("id", albumId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got mediaItems by albumId {}", albumId))
             .doOnError(error -> logger.error("Error getting mediaItems by albumId {}", ((Throwable) error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getMediaItemsByGoogleId(String googleId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item WHERE google_id=:google_id order by original_date desc", MediaItem.class)
                                                .setParameter("google_id", googleId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got mediaItems by googleId {}", googleId))
             .doOnError(error -> logger.error("Error getting mediaItems by googleId {}", ((Throwable) error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

//  public Mono<MediaItem> getPhotoTitle(String googleId, String filename) {
//    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where title = :title and google_id = :googleId", MediaItem.class)
//                                     .setParameter("title", filename)
//                                     .setParameter("googleId", googleId)
//                                     .getSingleResult())
//             .cast(MediaItem.class)
//             .doOnNext(photo -> logger.info("Got photo by title {}", photo.getTitle()))
//             .doOnError(error -> logger.error("Error getting photo by title {}", error.getMessage()))
//             .onErrorResume(e -> Mono.empty())
//             .subscribeOn(Schedulers.boundedElastic());
//  }

  public Mono<MediaItem> mediaItemExists(String googleId, String filename) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item where title = :title and google_id = :googleId", MediaItem.class)
                                     .setParameter("googleId", googleId)
                                     .setParameter("title", filename)
                                     .getSingleResult())
             .cast(MediaItem.class)
             .doOnNext(mediaItem -> logger.debug("Check if mediaItem exists {} {}", filename, mediaItem))
             .onErrorResume(e -> Mono.just(new MediaItem()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Integer> updateMediaItemOriginalDate(Integer mediaItemId, LocalDate date) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("UPDATE media_item_meta_data SET created_date = :date where media_item_id = :mediaItemId")
                           .setParameter("mediaItemId", mediaItemId)
                           .setParameter("date", date)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    })
             .doOnNext(mediaItem -> logger.debug("updated originaldate on mediaItem {}", mediaItem))
             .doOnError(error -> logger.error("error updating originaldate on mediaItem {} {}", mediaItemId, error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getMediaItemsByIds(List<Integer> ids) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item WHERE id in (:ids)", MediaItem.class)
                                                .setParameter("ids", ids)
                                                .getResultList()))
             .doOnNext(result -> logger.info("Got mediaItem by id {}", ids.toString()))
             .doOnError(error -> logger.error("Error getting mediaItems by ids {}", ((Throwable) error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<MediaItem> getMediaItems() {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM media_item", MediaItem.class)
                                                .getResultList()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteByMediaItemIds(List<Integer> ids) {
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
    }).doOnNext(result -> logger.info("Deleted mediaItems by ids {}", ids.toString()))
             .doOnError(error -> logger.error("Error deleting mediaItems by ids {}", error))
             .then()
             .subscribeOn(Schedulers.boundedElastic());
  }



  public Mono<Integer> deleteAllMediaItemsByAlbumId(Integer albumId) {
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
    }).doOnNext(result -> logger.info("Deleted all mediaItems by albumId {}", albumId))
             .doOnError(error -> logger.error("Error deleting mediaItems {} ", error.getMessage()))
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

