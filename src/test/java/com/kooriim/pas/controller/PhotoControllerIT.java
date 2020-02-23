package com.kooriim.pas.controller;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.repository.PhotoRepository;
import com.kooriim.pas.service.PhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
//@DirtiesContext
public class PhotoControllerIT {
  private static final Logger logger = LoggerFactory.getLogger(PhotoControllerIT.class);

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private PhotoService photoService;

  @Autowired
  private PhotoRepository photoRepository;

  @AfterEach
  public void afterAll() {
    logger.info("clearing test env");
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0;").executeUpdate();
    entityManager.createNativeQuery("truncate table photo;").executeUpdate();
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
  }

  @Test
  public void testCreatePhoto() throws IOException {
    final var photo = createPhoto();
    assertNotNull(photo.getId());
  }

  @Test
  public void testGetPhotos() {
    createPhoto();
    final var photos = restTemplate.getForObject("/photos", Photo[].class);
    assertEquals(1, photos.length);
  }

  @Test
  public void testGetPhotoById() {
    final var photo = createPhoto();
    final var photoResult = restTemplate.getForObject(uriWithPathVariable(photo.getId()), Photo.class);
    assertEquals(photo.getId(), photoResult.getId());
  }

  @Test
  public void testGetPhotoByIdWithSrc() {
    final var photo = createPhoto();
    final var photoResult = restTemplate
                              .getForObject(uriWithPathVariableAndQueryParam(photo.getId(),
                                "srcImage",
                                "true"),
                                Photo.class);
    assertNotNull(photoResult.getBase64SrcPhoto());
  }

  @Test
  public void testGetPhotoById404() {
    final var photos = restTemplate
                         .exchange(uriWithPathVariable(123),
                           HttpMethod.GET,
                           httpEntity(),
                           new ParameterizedTypeReference<List<Photo>>() {
                           });
    assertEquals(404, photos.getStatusCodeValue());
  }

  @Test
  public void testGetPhotosWithThumbnail() {
    createPhoto();
    final var photos = restTemplate
                         .exchange(uriWithQueryParam("thumbnail", "true"),
                           HttpMethod.GET,
                           httpEntity(),
                           new ParameterizedTypeReference<List<Photo>>() {
                           });
    assertNotNull(photos.getBody().get(0).getBase64ThumbnailPhoto());
  }

  @Test
  public void testGetPhotsoWithSrc() {
    createPhoto();
    final var photos = restTemplate
                         .exchange(uriWithQueryParam("srcImage", "true"),
                           HttpMethod.GET,
                           httpEntity(),
                           new ParameterizedTypeReference<List<Photo>>() {
                           });
    assertNotNull(photos.getBody().get(0).getBase64SrcPhoto());
  }


  @Test
  public void testDeletePhoto() {
    final var photo = createPhoto();
    assertEquals(1, photoRepository.findAll().size());
    restTemplate.delete("/photos/" + photo.getId());
    assertEquals(0, photoRepository.findAll().size());
  }

  @Test
  public void testDeletePhotos() {
    final var photo = createPhoto();
    assertEquals(1, photoRepository.findAll().size());
    final var photoIdsAsString = Arrays.asList(photo.getId())
                                   .toString()
                                   .replaceAll("\\[", "")
                                   .replaceAll("]", "");
    restTemplate
      .exchange(uriWithQueryParam("photoIds", photoIdsAsString),
        HttpMethod.DELETE,
        httpEntity(),
        Void.class);
    assertEquals(0, photoRepository.findAll().size());
  }

  private Photo createPhoto() {
    final var headers = new HttpHeaders();
    final var form = new LinkedMultiValueMap<>();
    form.add("file", new ClassPathResource("test.jpg"));
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    final var requestEntity = new HttpEntity<>(form, headers);

    return restTemplate.exchange("/photos", HttpMethod.POST, requestEntity, Photo.class).getBody();
  }

  private String uriWithQueryParam(String param, Object paramValue) {
    return UriComponentsBuilder
             .fromPath("/photos")
             .queryParam(param, paramValue)
             .build()
             .toUriString();
  }

  private String uriWithPathVariable(Integer pathId) {
    return UriComponentsBuilder
             .fromPath("/photos/" + pathId)
             .build()
             .toUriString();
  }

  private String uriWithPathVariableAndQueryParam(Integer pathId, String param, String paramValue) {
    return UriComponentsBuilder
             .fromPath("/photos/" + pathId)
             .queryParam(param, paramValue)
             .build()
             .toUriString();
  }

  private HttpEntity httpEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(headers);
  }
}
