package com.kooriim.pas.repository;

import com.kooriim.pas.domain.MediaItem;
import com.kooriim.pas.domain.MediaItemMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

@Repository
public class MediaItemMetaDataRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  public Mono<MediaItemMetaData> save(MediaItemMetaData mediaItemMetaData) {
    return Mono.fromCallable(() -> {
      var em = entityManagerFactory.createEntityManager();
      var trans = em.getTransaction();
      trans.begin();
      final var result = em.merge(mediaItemMetaData);
      trans.commit();
      em.close();
      return result;
    }).doOnNext(result -> logger.info("Saved mediaItemMetaData {}", result.getId()))
             .doOnError(error -> logger.error("Error saving mediaItemMetaData {}", error.getMessage()))
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<MediaItemMetaData> findByMediaItemId(Integer id) {
    return Mono.fromCallable(() -> entityManager.createNativeQuery("SELECT * FROM media_item_meta_data where media_item_id = :id", MediaItemMetaData.class)
                                     .setParameter("id", id)
                                     .getSingleResult())
             .cast(MediaItemMetaData.class)
             .doOnNext(mediaItemMetaData -> logger.info("Got mediaItemMetaData {}", mediaItemMetaData.getId()))
             .onErrorResume(e -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }
}
