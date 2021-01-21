package com.kooriim.pas.repository;

import com.kooriim.pas.domain.Photo;
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

  public Mono<Photo> getPhotoById(Integer id) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM photo where id = :id", Photo.class)
                                     .setParameter("id", id)
                                     .getSingleResult())
             .cast(Photo.class)
             .doOnNext(photo -> logger.info("Got photo {}", photo.getTitle()))
             .doOnError(error -> logger.error("Error getting photo {}", error.getMessage()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<Photo> getPhotosByAlbumId(Integer albumId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM photo p INNER JOIN photo_album pa ON p.id=pa.photo_id WHERE pa.album_id=:id", Photo.class)
                                                .setParameter("id", albumId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got photos by albumId {}", albumId))
             .doOnError(error -> logger.error("Error getting photos by albumId {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<Photo> getPhotosByGoogleId(String googleId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM photo WHERE google_id=:google_id order by created desc", Photo.class)
                                                .setParameter("google_id", googleId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Got photos by googleId {}", googleId))
             .doOnError(error -> logger.error("Error getting photos by googleId {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<Photo> getPhotosByIds(List<Integer> ids) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM photo WHERE id in (:ids)", Photo.class)
                                                .setParameter("ids", ids)
                                                .getResultList()))
             .doOnNext(result -> logger.info("Got photoById {}", ids.toString()))
             .doOnError(error -> logger.error("Error getting photosById {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<Photo> getPhotos() {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM photo", Photo.class)
                                                .getResultList()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteByPhotoIds(List<Integer> ids) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("DELETE photo_album, photo FROM photo_album RIGHT JOIN photo ON photo_album.photo_id=photo.id WHERE photo.id in (:ids)")
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
      final var result = em.createNativeQuery("DELETE FROM photo_album where album_id = :id")
                           .setParameter("id", albumId)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Deleted all photos by albumId {}", albumId))
             .doOnError(error -> logger.error("Error deleting photos {} ", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }


  public Mono<Photo> save(Photo photo) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.merge(photo);
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Saved photo {}", result.getTitle()))
             .doOnError(error -> logger.error("Error saving photo {}", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }
}

