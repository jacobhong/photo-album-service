//package com.kooriim.pas.controller;
//
//import com.kooriim.pas.domain.Album;
//import com.kooriim.pas.domain.GoogleOidUser;
//import com.kooriim.pas.domain.Photo;
//import com.kooriim.pas.repository.AlbumRepository;
//import com.kooriim.pas.repository.PhotoRepository;
//import com.kooriim.pas.repository.UserRepository;
//import com.kooriim.pas.service.AlbumService;
//import com.kooriim.pas.service.PhotoService;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.context.SecurityContextImpl;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import javax.transaction.Transactional;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ExtendWith(SpringExtension.class)
//@ActiveProfiles("test")
//@Transactional
//public class AlbumControllerIT {
//  private static final Logger logger = LoggerFactory.getLogger(AlbumControllerIT.class);
//
//  @PersistenceContext
//  private EntityManager entityManager;
//
//  @Autowired
//  private TestRestTemplate restTemplate;
//
//  @Autowired
//  private PhotoService photoService;
//
//  @Autowired
//  private PhotoRepository photoRepository;
//
//  @Autowired
//  private AlbumRepository albumRepository;
//
//  @Autowired
//  private AlbumService albumService;
//
//  @Autowired
//  private UserRepository userRepository;
//
//  @AfterEach
//  public void afterAll() {
//    logger.info("clearing test env");
//    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0; " +
//                                      "truncate photo_album; " +
//                                      "truncate album; " +
//                                      "truncate photo; " +
//                                      "truncate users; " +
//                                      "SET FOREIGN_KEY_CHECKS=1").executeUpdate();
//  }
//
//  @Test
//  public void testCreateAlbumWithPhotos() {
//    insertUser();
//    insertPhotos();
//    final var album = restTemplate.exchange(uri(), HttpMethod.POST, httpEntityWithAlbum(), Album.class);
//    final var albums = photoRepository.getPhotosByAlbumId(album.getBody().getId()).get();
//    assertEquals(2, albums.size());
//    assertEquals("1", albums.get(0).getGoogleId());
//  }
//
//  private String uri() {
//    return UriComponentsBuilder
//             .fromPath("/albums")
//             .build()
//             .toUriString();
//  }
//
//  private HttpEntity httpEntityWithAlbum() {
//    final var headers = new HttpHeaders();
//    headers.setContentType(MediaType.APPLICATION_JSON);
//    return new HttpEntity<>(createAlbum(), headers);
//  }
//
//  private Album createAlbum() {
//    final var album = new Album();
//    album.setDescription("test");
//    album.setTitle("test");
//    album.setPhotoIds(Set.of(1, 2));
//    album.setGoogleId("1");
//    return album;
//  }
//
//  private void insertPhotos() {
//    final var photo = new Photo();
//    photo.setTitle("1");
//    photo.setContentType("jpg");
//    photo.setFilePath("test");
//    photo.setThumbnailFilePath("test");
//    photo.setGoogleId("1");
//    final var photo2 = new Photo();
//    photo2.setTitle("2");
//    photo2.setContentType("jpg");
//    photo2.setFilePath("test");
//    photo2.setThumbnailFilePath("test");
//    photo2.setGoogleId("1");
//    photoRepository.save(photo);
//    photoRepository.save(photo2);
//  }
//
//  private void insertUser() {
//    final var user = new GoogleOidUser();
//    user.setGoogleId("1");
//    user.setEmail("email");
//    user.setName("name");
//    userRepository.save(user);
//
//  }
//
//  private SecurityContextHolder securityContextHolder() {
//    final var securityContextHolder = new SecurityContextHolder();
//    final var OAuth2 = new OAuth2User();
//    final var authentication = new OAuth2AuthenticationToken();
//  }
//}
