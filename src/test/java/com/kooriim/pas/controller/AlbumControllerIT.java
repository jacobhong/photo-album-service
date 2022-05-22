//package com.kooriim.pas.controller;
//
//import com.kooriim.pas.domain.Album;
//import com.kooriim.pas.domain.MediaItem;
//import com.kooriim.pas.repository.AlbumRepository;
//import com.kooriim.pas.repository.MediaItemRepository;
//import com.kooriim.pas.repository.UserRepository;
//import com.kooriim.pas.service.AlbumService;
//import com.kooriim.pas.service.MediaItemService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.http.MediaType;
//import org.springframework.http.client.MultipartBodyBuilder;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.context.SecurityContextImpl;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.reactive.server.EntityExchangeResult;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.BodyInserters;
//
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Arrays;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
//  private WebTestClient webTestClient;
//
//  @Autowired
//  private MediaItemService mediaItemService;
//
//  @Autowired
//  private MediaItemRepository mediaItemRepository;
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
//  @BeforeEach
//  public void before() {
//    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0;").executeUpdate();
//    entityManager.createNativeQuery("truncate table media_item_album;").executeUpdate();
//    entityManager.createNativeQuery("truncate table media_item;").executeUpdate();
//    entityManager.createNativeQuery("truncate table album;").executeUpdate();
//    entityManager.createNativeQuery("ALTER TABLE media_item ALTER id RESTART with 1;").executeUpdate();
//    entityManager.createNativeQuery("ALTER TABLE album ALTER id RESTART with 1;").executeUpdate();
//    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
//    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofMinutes(5)).build();
//
//  }
//
//  /**
//   * Default spring security user is anonymousUser
//   */
//  @Test
//  @WithMockUser("anonymousUser")
//  public void testGetAlbumsByGoogleId() {
//    createAlbum();
//    webTestClient.get().uri("/photo-album-service/albums?page=0&size=10")
//      .exchange()
//      .expectStatus()
//      .is2xxSuccessful()
//      .expectBodyList(Album.class).consumeWith(a -> assertEquals(1, a.getResponseBody().size()));
//  }
//
//  @Test
//  @WithMockUser("anonymousUser")
//  public void addPhotosToExistingAlbum() throws InterruptedException {
//    final var photo = createPhoto().getResponseBody();
//    final var album = createAlbum().getResponseBody();
//    final var aaaaa = webTestClient.patch().uri("/photo-album-service/albums/{id}", album.getId())
//                        .body(BodyInserters.fromValue(Arrays.asList(photo.getId())))
//                        .exchange()
//                        .expectStatus()
//                        .is2xxSuccessful()
//                        .expectBody(Void.class)
//                        .returnResult();
//    Thread.sleep(1000);
//    final var result = webTestClient.get().uri("/photo-album-service/albums?page=0&size=10")
//                         .exchange()
//                         .expectStatus()
//                         .is2xxSuccessful()
//                         .expectBodyList(Album.class)
//                         .returnResult();
//    assertEquals(1, result.getResponseBody().size());
//    assertEquals(1, result.getResponseBody().get(0).getPreviewMediaItems().size());
//  }
////
////  @Test
////  public void deleteAlbum() {
////    final var album = restTemplate.exchange(UriHelper.uri("/albums"), HttpMethod.POST, UriHelper.httpEntityWithBody(createAlbum()), Album.class);
////    restTemplate.exchange(UriHelper.uri("/albums/" + album.getBody().getId()), HttpMethod.PATCH, UriHelper.httpEntityWithBody(Arrays.asList("1")), Void.class);
////    restTemplate.exchange(UriHelper.uri("/albums/" + album.getBody().getId()), HttpMethod.DELETE, UriHelper.httpEntity(), Void.class);
////
////    final var photos = restTemplate
////                         .exchange(UriHelper.uriWithQueryParam("/photos", "albumId", album.getBody().getId()),
////                           HttpMethod.GET,
////                           UriHelper.httpEntity(),
////                           new ParameterizedTypeReference<List<MediaItem>>() {
////                           });
////
////    assertEquals(0, photos.getBody().size());
////  }
//
//
//  private EntityExchangeResult<Album> createAlbum() {
//    final var album = new Album();
//    album.setDescription("test");
//    album.setTitle("test");
//    return webTestClient.post().uri("/photo-album-service/albums")
//             .body(BodyInserters.fromValue(album))
//             .exchange()
//             .expectStatus()
//             .is2xxSuccessful()
//             .expectBody(Album.class)
//             .returnResult();
//  }
//
//
//  private Album createAlbumWithPhoto() {
//    final var album = new Album();
//    album.setDescription("test");
//    album.setTitle("test");
//    return album;
//  }
//
//  private EntityExchangeResult<MediaItem> createPhoto() {
//    final var builder = new MultipartBodyBuilder();
//    builder.part("file", new ClassPathResource("test.jpg"))
//      .filename("test.jpg")
//      .contentType(MediaType.IMAGE_JPEG);
//    return webTestClient.post()
//             .uri("/photo-album-service/photos")
//             .contentType(MediaType.MULTIPART_FORM_DATA)
//             .body(BodyInserters.fromMultipartData(builder.build()))
//             .exchange()
//             .expectStatus()
//             .is2xxSuccessful()
//             .expectBody(MediaItem.class).returnResult();
//  }
//
//
//  private void securityContextHolder() {
//    final var jwt = new Jwt("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJjeUpmelJnckNwNTBlLWN4WUozOU5iX0FSU0lvRkN0MC1ubzhDVXJNblcwIn0.eyJqdGkiOiIxNDhmMTllYS02NzQyLTQxNWYtYWUyMi00ZGQ3OTIwMThkZTkiLCJleHAiOjE1ODE3NDExMjcsIm5iZiI6MCwiaWF0IjoxNTgxNzQwODI3LCJpc3MiOiJodHRwOi8vMTkyLjE2OC4xLjIwNi54aXAuaW86ODA4MS9hdXRoL3JlYWxtcy9rb29yaWltLWZlIiwiYXVkIjpbImJyb2tlciIsImFjY291bnQiXSwic3ViIjoiMWU1NWNmMzktNzViYi00MGU3LWFmOGMtZWUxNmI5NmE2MTc4IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoia29vcmlpbS1mZSIsIm5vbmNlIjoiMWM4MGRlNTctZDNlMC00MmFlLTg2NzItNjA0YjJhNWJiYzZkIiwiYXV0aF90aW1lIjoxNTgxNzM4ODE2LCJzZXNzaW9uX3N0YXRlIjoiYTlkYTBiOWItZDY3OS00MzY3LWJiYzQtY2YyMjBmYjVkZDM5IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTkyLjE2OC4xLjIwNi54aXAuaW86NDIwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJrb29yaWltLWZlIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJicm9rZXIiOnsicm9sZXMiOlsicmVhZC10b2tlbiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCIsImdyYW50cyI6WyJvZmZsaW5lX2FjY2VzcyIsImtvb3JpaW0tZmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJqYWNvYiBob25nIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiamFjb2IuaG9uZ0BnbWFpbC5jb20iLCJnaXZlbl9uYW1lIjoiamFjb2IiLCJmYW1pbHlfbmFtZSI6ImhvbmciLCJlbWFpbCI6ImphY29iLmhvbmdAZ21haWwuY29tIn0.SIOpHnj1_Zugm09FRZAhsn6XSrrnB9zINnQ5KS6H4SWqkTs7xRmXZb8H95LxM2pVe144R-rH-QoA8aen9hgAoAWZnLF1FZwyLo8PkVKRjWFUFrGea0iRYP_iovKjCgp5JvK4siybFrel6lrs742oj8hyK7kzwSFn6AJj23oZq33LNnYaeNHW9djvT3_dcsWK1zkWUfRgYp-m6dVuSOMFc3-_0kndh3_5aFOrDbAnnDEcKXS__U8pntWXp8vgMlZ1BKIgS4bFqFfHw8Rn1nPM2xwLyDRaatAoZhGPYQIUsssdtiKdmy2M8rPq9P00EGrcKOObaF706p8uVnb51PaYww",
//      Instant.from(Instant.now()),
//      Instant.from(Instant.now()),//1e55cf39-75bb-40e7-af8c-ee16b96a6178
//      Map.of("grants", "grants"),
//      Map.of("claims", Map.of("sub", "1")));
//    final var authentication = new JwtAuthenticationToken(jwt);
//    final var securityContext = new SecurityContextImpl(authentication);
//    SecurityContextHolder.setContext(securityContext);
//  }
//}
