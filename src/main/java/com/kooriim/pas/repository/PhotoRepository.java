package com.kooriim.pas.repository;

import com.kooriim.pas.domain.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PhotoRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PersistenceContext
  private EntityManager entityManager;

  public Mono<List<Photo>> getPhotosByAlbumId(Integer id, Pageable pageable) {
    return Mono
             .fromCallable(() -> entityManager
                                   .createNativeQuery("SELECT * FROM photo p INNER JOIN photo_album pa ON p.id=pa.photo_id WHERE pa.album_id=?", Photo.class)
                                   .setParameter("id", id)
                                   .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                   .setMaxResults(pageable.getPageSize()).getResultList()).map(photos -> (List<Photo>) photos)
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<Photo> getPhotoById(Integer id) {
    logger.info("getting photo");
    return Mono.fromCallable(() -> (Photo) entityManager.createNativeQuery("SELECT * FROM photo where id = :id", Photo.class)
                                             .setParameter("id", id)
                                             .getSingleResult())
             .subscribeOn(Schedulers.elastic())
             .onErrorResume(e -> Mono.empty())
             .doOnNext(p -> logger.info("got pppp"))
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<List<Photo>> getPhotosByGoogleId(String googleId, Pageable pageable) {
    logger.info("getting photo");
    return Mono.fromCallable(() -> entityManager
                                     .createNativeQuery("SELECT * FROM photo WHERE google_id=:google_id", Photo.class)
                                     .setParameter("google_id", googleId)
                                     .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                                     .setMaxResults(pageable.getPageSize()).getResultList()).map(photos -> (List<Photo>) photos)
             .doOnNext(photos -> logger.info("fetched photos: {}", photos))
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<List<Photo>> getByIds(List<Integer> ids) {
    logger.info("getting photo");
    return Mono.fromCallable(() -> entityManager
                                     .createNativeQuery("SELECT * FROM photo WHERE id in (:ids)", Photo.class)
                                     .setParameter("id", ids)
                                     .getResultList())
             .map(photo -> (List<Photo>) photo)
             .subscribeOn(Schedulers.elastic());
  }

  public void deleteByIds(List<Integer> ids) {
    logger.info("deleting by ids");
    Mono.fromCallable(() -> entityManager
                              .createNativeQuery("DELETE p, a FROM album a JOIN photo p ON p.id = a.id WHERE p.id in (:ids)")
                              .setParameter("id", ids)
                              .executeUpdate())
      .subscribeOn(Schedulers.elastic());
  }

  @Transactional
  public Mono<Photo> save(Photo photo) {
    logger.info("deleting by ids");
    entityManager.persist(photo);
    return Mono.fromCallable(() -> entityManager.merge(photo))
             .map(p -> p)
             .subscribeOn(Schedulers.elastic());
  }
}

