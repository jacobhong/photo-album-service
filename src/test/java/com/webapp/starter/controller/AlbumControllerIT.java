package com.webapp.starter.controller;

import com.webapp.starter.domain.Album;
import com.webapp.starter.domain.Photo;
import com.webapp.starter.repository.AlbumRepository;
import com.webapp.starter.repository.PhotoRepository;
import com.webapp.starter.service.AlbumService;
import com.webapp.starter.service.PhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
public class AlbumControllerIT {
  private static final Logger logger = LoggerFactory.getLogger(AlbumControllerIT.class);

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private PhotoService photoService;

  @Autowired
  private PhotoRepository photoRepository;

  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private AlbumService albumService;

  @AfterEach
  public void afterAll() {
    logger.info("clearing test env");
    entityManager.createNativeQuery("truncate album; truncate photo; truncate photo_album;").executeUpdate();
  }

  @Test
  public void testCreateAlbumWithPhotos() {
    insertPhotos();
    final var album = restTemplate.exchange(uri(), HttpMethod.POST, httpEntityWithAlbum(), Album.class);
    assertEquals(2, photoRepository.getPhotosByAlbumId(album.getBody().getId()).get().size());
  }

  private String uri() {
    return UriComponentsBuilder
             .fromPath("/albums")
             .build()
             .toUriString();
  }

  private HttpEntity httpEntityWithAlbum() {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(createAlbum(), headers);
  }

  private Album createAlbum() {
    final var album = new Album();
    album.setDescription("test");
    album.setTitle("test");
    album.setPhotoIds(Set.of(1, 2));
    return album;
  }

  private void insertPhotos() {
    final var photo = new Photo();
    photo.setTitle("1");
    photo.setContentType("jpg");
    photo.setFilePath("test");
    photo.setThumbnailFilePath("test");
    final var photo2 = new Photo();
    photo2.setTitle("2");
    photo2.setContentType("jpg");
    photo2.setFilePath("test");
    photo2.setThumbnailFilePath("test");
    photoRepository.save(photo);
    photoRepository.save(photo2);
  }

}
