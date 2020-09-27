package com.kooriim.pas.controller;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.repository.PhotoRepository;
import com.kooriim.pas.service.PhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.main.web-application-type=reactive",
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
public class PhotoControllerIT {
  private static final Logger logger = LoggerFactory.getLogger(PhotoControllerIT.class);
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private PhotoService photoService;

  @Autowired
  private PhotoRepository photoRepository;

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  public void before() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofMinutes(5)).build();
  }

  @AfterEach
  public void afterAll() {
    logger.info("clearing test env");
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0;").executeUpdate();
    entityManager.createNativeQuery("truncate table photo;").executeUpdate();
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testCreatePhoto() throws IOException, InterruptedException {
    final var photo = createPhoto().getResponseBody();
    assertNotNull(photo.getId());
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testGetPhotos() throws IOException {
    createPhoto().getResponseBody();
    webTestClient.get().uri("/photo-album-service/photos?page=0&size=10")
      .exchange()
      .expectStatus()
      .is2xxSuccessful()
      .expectBodyList(Photo.class).consumeWith(p -> assertEquals(1, p.getResponseBody().size()));
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testGetPhotoById() {
    final var photo = createPhoto();
    webTestClient.get().uri("/photo-album-service/photos/{id}?compressedImage=true", photo.getResponseBody().getId())
      .exchange()
      .expectStatus()
      .is2xxSuccessful()
      .expectBody(Photo.class)
      .consumeWith(p -> assertNotNull(p.getResponseBody().getBase64CompressedImage()));
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testGetPhotoById404() {
    webTestClient.get().uri("/photo-album-service/photos/123?compressedImage=true")
      .exchange()
      .expectStatus()
      .isNotFound();
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testGetPhotosWithThumbnail() {
    final var photo = createPhoto();
    webTestClient.get().uri("/photo-album-service/photos?thumbnail=true&page=0&size=10")
      .exchange()
      .expectStatus()
      .is2xxSuccessful()
      .expectBodyList(Photo.class)
      .consumeWith(p -> assertNotNull(p.getResponseBody().get(0).getBase64ThumbnailImage()));
  }

  @Test
  @WithMockUser("anonymousUser")
  public void testGetPhotsoWithSrc() {
    final var photo = createPhoto();
    webTestClient.get().uri("/photo-album-service/photos?compressedImage=true&page=0&size=10")
      .exchange()
      .expectStatus()
      .is2xxSuccessful()
      .expectBodyList(Photo.class)
      .consumeWith(p -> assertNotNull(p.getResponseBody().get(0).getBase64CompressedImage()));
  }


  //cant delete with join with h2
//  @Ignore
  @WithMockUser("anonymousUser")
  public void testDeletePhoto() {
    final var photo = createPhoto();
    webTestClient.delete().uri("/photo-album-service/photos/{id}", photo.getResponseBody().getId())
      .exchange()
      .expectStatus()
      .is2xxSuccessful();
    webTestClient.get().uri("/photo-album-service/photos/{id}", photo.getResponseBody().getId())
      .exchange()
      .expectStatus()
      .isNotFound();
  }

//  @Test
//  public void testDeletePhotos() {
//    final var photo = createPhoto();
//    assertEquals(1, photoRepository.findAll().size());
//    final var photoIdsAsString = Arrays.asList(photo.getId())
//                                   .toString()
//                                   .replaceAll("\\[", "")
//                                   .replaceAll("]", "");
//    restTemplate
//      .exchange(UriHelper.uriWithQueryParam("/photos/", "photoIds", photoIdsAsString),
//        HttpMethod.DELETE,
//        UriHelper.httpEntity(),
//        Void.class);
//    assertEquals(0, photoRepository.findAll().size());
//  }

//  /**
//   * Set isPublic to true
//   */
  @Test
  @WithMockUser("anonymousUser")
  public void testPatchPhotos() {
    final var photo = createPhoto();
    photo.getResponseBody().setDescription("updated");
    webTestClient.patch().uri("/photo-album-service/photos", photo.getResponseBody().getId())
      .body(BodyInserters.fromValue(List.of(photo.getResponseBody())))
      .exchange()
      .expectStatus()
      .is2xxSuccessful().expectBody().returnResult();
    webTestClient.get().uri("/photo-album-service/photos/{id}?compressedImage=true", photo.getResponseBody().getId())
      .exchange()
      .expectStatus()
      .is2xxSuccessful()
      .expectBody(Photo.class).consumeWith(p -> assertEquals("updated", p.getResponseBody().getDescription()));
  }

  private EntityExchangeResult<Photo> createPhoto() {
    final var builder = new MultipartBodyBuilder();
    builder.part("file", new ClassPathResource("test.jpg"))
      .filename("test.jpg")
      .contentType(MediaType.IMAGE_JPEG);
    return webTestClient.post()
             .uri("/photo-album-service/photos")
             .contentType(MediaType.MULTIPART_FORM_DATA)
             .body(BodyInserters.fromMultipartData(builder.build()))
             .exchange()
             .expectStatus()
             .is2xxSuccessful()
             .expectBody(Photo.class).returnResult();
  }


}
