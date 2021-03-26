package com.kooriim.pas.repository;

import com.kooriim.pas.domain.Album;
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

@Repository
public class AlbumRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  public Mono<Album> save(Album album) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.merge(album);
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Saved album {}", result.getId()))
             .doOnError(error -> logger.error("Failed to save album {}", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Integer> savePhotoAlbum(Integer albumId, Integer photoId) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("INSERT INTO media_item_album(album_id, media_item_id) VALUES(:albumId, :photoId)")
                           .setParameter("albumId", albumId)
                           .setParameter("photoId", photoId)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Saved to media_item_album photoId {} albumId {}", photoId, albumId))
             .doOnError(error -> logger.error("Failed to media_item_album photoId {} albumId {} error {}",
               photoId,
               albumId,
               error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Flux<Album> findByGoogleId(String googleId, Pageable pageable) {
    return Flux.defer(() -> Flux.fromIterable(entityManager
                                                .createNativeQuery("SELECT * FROM album where google_id = :googleId", Album.class)
                                                .setParameter("googleId", googleId)
                                                .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                                .setMaxResults(pageable.getPageSize()).getResultList()))
             .doOnNext(result -> logger.info("Found album by googleId {}", ((Album)result).getId()))
             .doOnError(error -> logger.error("Failed to save album {}", ((Throwable)error).getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Album> getAlbumById(Integer id) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM album where id = :id", Album.class)
                                     .setParameter("id", id)
                                     .getSingleResult())
             .cast(Album.class)
             .doOnNext(result -> logger.info("Found album by albumId {}", result.getId()))
             .doOnError(error -> logger.error("Failed to get album {}", error.getMessage()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteById(Integer id) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.createNativeQuery("DELETE from album WHERE id = :id")
                           .setParameter("id", id)
                           .executeUpdate();
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Deleted album by Id {}", id.toString()))
             .doOnError(error -> logger.error("Error deleting by album id {}", error.getMessage()))
             .then()
             .subscribeOn(Schedulers.boundedElastic());
  }


}
