package com.kooriim.pas.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.types.proto.MediaItem;
import com.kooriim.pas.domain.GoogleTokenExchangeRequest;
import com.kooriim.pas.domain.GoogleTokenExchangeResponse;
import com.kooriim.pas.domain.MediaItemWithIdentityToken;
import com.kooriim.pas.repository.MediaItemRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.parameters.P;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

/**
 * alpha testing
 * TODO add sync by date
 * possibly add kafka
 */
@Service
public class GoogleService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;
  @Value("${google.clientId}")
  private String googleClientId;
  @Value("${google.clientSecret}")
  private String googleSecret;
  @Autowired
  private MediaItemService mediaItemService;
  @Autowired
  private WebClient webClient;
  @Autowired
  @Qualifier("googleClient")
  private WebClient googleClient;
  @Autowired
  private MediaItemRepository mediaItemRepository;

  /**
   * Download all MediaItems from google, upload to s3 and save record to database. Need add delay here
   * because google api limits how often we call them
   *
   * @param accessToken
   * @return
   */
  public Flux<com.kooriim.pas.domain.MediaItem> syncGooglePhotos(Jwt accessToken) {
//    return getUserAccessToken()
    return getGoogleIdentityToken(accessToken)
             .flatMap(this::initializeS3PhotosLibraryClient)
             .flatMapMany(map -> getGooglePhotos(map))
             .delayUntil(d -> Mono.delay(Duration.ofSeconds(6)))
             .flatMap(mediaItem -> downloadGoogleMediaItem(accessToken, mediaItem))
             .flatMap(mediaItem -> uploadMediaItem(accessToken, mediaItem))
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<GoogleTokenExchangeResponse> getGoogleIdentityToken(Jwt accessToken) {
    logger.info("getting google identity token");
    // refresh tokens?
    return webClient.get()
             .uri("/auth/realms/kooriim-fe/broker/google/token?access_type=offline")
             .accept(MediaType.APPLICATION_JSON)
             .header("Authorization", "Bearer " + accessToken.getTokenValue())
             .retrieve()
             .bodyToMono(GoogleTokenExchangeResponse.class)
             .doOnError(error -> logger.error("Error getting google identity token {}", error.getMessage()))
             .doOnNext(token -> logger.info("got token {}", token))
             .flatMap(identityToken -> refreshExchangeGoogle(identityToken));
//             .flatMap(identityToken -> refreshExchangeGoogle(identityToken));
  }

//  public Mono<GoogleTokenExchangeResponse> tokenExchangeGoogle(GoogleTokenExchangeResponse accessToken) {
//    logger.info("exchanging google identity token");
//    // refresh tokens?
//    return webClient.post()
//             .uri("/auth/realms/kooriim-fe/protocol/openid-connect/token")
//             .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
//             .body(BodyInserters
//                     .fromFormData("client_id", "kooriim-fe")
//                     .with("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
//                     .with("requested_token_type", "urn:ietf:params:oauth:token-type:refresh_token")
//                     .with("subject_token", accessToken.getAccessToken())
//                     .with("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
//                     .with("subject_issuer", "google")
//                     .with("requested_issuer", "google"))
////             .accept(MediaType.APPLICATION_JSON)
//             .retrieve()
//             .bodyToMono(GoogleTokenExchangeResponse.class)
//             .doOnError(error -> logger.error("Error getting tokenExchangeGoogle {}", error))
//             .doOnNext(token -> logger.info("Got tokenExchangeGoogle exchange from google {}", token));
//  }

  public Mono<GoogleTokenExchangeResponse> refreshExchangeGoogle(GoogleTokenExchangeResponse accessToken) {
    logger.info("exchanging google identity token");
    // refresh tokens?
    return googleClient.post()
             .uri("/token", uriBuilder ->
                              uriBuilder.queryParam("client_id", googleClientId)
                                .queryParam("client_secret", googleSecret)
                                .queryParam("grant_type", "refresh_token")
                                .queryParam("refresh_token", accessToken.getRefreshToken())
                                .build())
             .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
             .retrieve()
             .bodyToMono(GoogleTokenExchangeResponse.class)
             .doOnError(error -> logger.error("Error getting refreshExchangeGoogle {}", error))
             .doOnNext(token -> logger.info("Got refreshExchangeGoogle exchange from google {}", token));
  }

  // TODO sync by date range?
  private Flux<MediaItemWithIdentityToken> getGooglePhotos(Map<String, Object> map) {
    return Flux.defer(() -> {
      logger.info("getting google photos.. may take awhile");
      var photosLibraryClient = (PhotosLibraryClient) map.get("client");
      var identityToken = (GoogleTokenExchangeResponse) map.get("googleTokenExchangeResponse");
      final var listMediaItemsPagedResponse = photosLibraryClient.listMediaItems(ListMediaItemsRequest
                                                                                   .newBuilder()
                                                                                   .setPageSize(100)
                                                                                   .build());
      logger.info("response {}", listMediaItemsPagedResponse);
      final var list = new ArrayList<MediaItemWithIdentityToken>();
      listMediaItemsPagedResponse.getPage()
        .getValues()
        .forEach(mediaItem -> list.add(new MediaItemWithIdentityToken(mediaItem, identityToken)));

      var nextPageToken = listMediaItemsPagedResponse.getNextPageToken();
      var nextPage = listMediaItemsPagedResponse.getPage().getNextPage();
      var index = 0; // TODO batch get here instead
      while (StringUtils.isNotEmpty(nextPageToken)) {
        logger.info("getting next page");
        nextPage.getValues()
          .forEach(mediaItem -> list.add(new MediaItemWithIdentityToken(mediaItem, identityToken)));
        nextPageToken = nextPage.getNextPageToken();
        nextPage = nextPage.getNextPage();
        index++;
        // store index  or date of last itemand try later
      }
      logger.info("returning google list size {}", list.size());
      return Flux.fromIterable(list);
    }).doOnError((e) -> logger.error("error getting google photos {}", e.getMessage()))
//             .doOnNext(x -> logger.info("finished getting photos from google"))
             .subscribeOn(Schedulers.elastic());
  }

  private Mono<Map<String, Object>> initializeS3PhotosLibraryClient(GoogleTokenExchangeResponse googleTokenExchangeResponse) {
    return Mono.fromCallable(() -> {
      logger.info("initializing photos library client");
      // use regular accesstoken here and let it expire and then refresh for new url
//      var cred = UserCredentials.newBuilder()
//                   .setClientId("36443888483-nltubgbdq78spoj2ebvaibem942f0r07.apps.googleusercontent.com")
//                   .setClientSecret("BbOZZ21fg5-fH4t1NSoapwjF")
//                   .setRefreshToken(googleTokenExchangeResponse.getRefreshToken())
////                   .setTokenServerUri(URI.create(jwkSetUri))
////                   .setHttpTransportFactory(new DefaultHttpTransportFactory())
//                   .build();
//      cred.refresh();
//      ;

      final var settings = PhotosLibrarySettings
                             .newBuilder()
                             .setCredentialsProvider(FixedCredentialsProvider
                                                       .create(GoogleCredentials.create(new AccessToken(googleTokenExchangeResponse.getAccessToken(),
                                                         Date.from(Instant.now().plusSeconds(Long.valueOf(googleTokenExchangeResponse.getExpiresIn())))))))
                             .build();
      final var client = PhotosLibraryClient.initialize(settings);
      return Map.of("client", client, "googleTokenExchangeResponse", googleTokenExchangeResponse);
    }).doOnError(e -> logger.error("error initializing photo client {}", e))
             .doOnNext(x -> logger.info("successfully initialized photos library client"));
  }

  private Mono<MediaItem> downloadGoogleMediaItem(Jwt accessToken, MediaItemWithIdentityToken mediaItemWithIdentityToken) {
//    logger.info("checking if media item already exists{}", mediaItem.getFilename());
//    return mediaItemService.getUserGoogleId()
//             .flatMap(googleId ->
    ;
    var mediaItem = mediaItemWithIdentityToken.getMediaItem();
    return mediaItemRepository.mediaItemExists(accessToken.getClaimAsString("sub"), mediaItem.getFilename())
             .flatMap(exists -> {
               if (exists.getId() == null) {
                 var contentType = getContentType(mediaItem.getMimeType());
                 var mediaType = mediaItemService.getMediaType(mediaItem.getMimeType());
//                                        if (mediaType.equalsIgnoreCase("photo")) return Mono.empty();
                 final var googleTempDir = new File("/tmp/google_photos");
                 if (!googleTempDir.exists()) {
                   googleTempDir.mkdirs();
                 }
                 if (mediaType.equalsIgnoreCase("photo")) {
                   return downloadImage(mediaItemWithIdentityToken, contentType);
                 } else {
//                                          logger.info("medaItem does not exist, downloading {}", mediaItem.getFilename());
                   return downloadVideo(mediaItem);
                 }
               }
               return Mono.empty();
//                                      logger.info("SKIPPING DUPLICATE MEDIAITEM {}", mediaItem.getFilename());
//                                      throw new ConflictException("skipping google mediaItem because duplicate exists " + mediaItem.getFilename());
             });


  }

  private Mono<MediaItem> downloadImage(MediaItemWithIdentityToken mediaItemWithIdentityToken, String contentType) {
    logger.info("Downloading image {}", mediaItemWithIdentityToken.getMediaItem().getFilename());
    var mediaItem = mediaItemWithIdentityToken.getMediaItem();
    return getMediaItem(mediaItem, contentType)
             .onErrorResume(error -> {
               logger.error("ON ERROR RESUME");
               logger.error("ERROR IS OF UNAUTHENTICATED TYPE {}", error);
               var identityToken = mediaItemWithIdentityToken.getGoogleTokenExchangeResponse();
               logger.info("got identity token {}", identityToken.getAccessToken());
//                 var cred = UserCredentials.newBuilder()
//                              .setRefreshToken(identityToken.getRefreshToken())
//                              .setTokenServerUri(URI.create(jwkSetUri))
//                              .setHttpTransportFactory(new DefaultHttpTransportFactory())
//                              .build();
//                 cred.refresh();
               return initializeS3PhotosLibraryClient(identityToken)
                        .flatMap(x -> {
                          try {
                            logger.info("refreshing token {}", identityToken);

                            var photoClient = (PhotosLibraryClient)x.get("client");
//                            final var settings = PhotosLibrarySettings
//                                                   .newBuilder()
//                                                   .setCredentialsProvider(FixedCredentialsProvider
//                                                                             .create(UserCredentials.create(new AccessToken(x.getAccessToken(),
//                                                                               Date.from(Instant.now().plusSeconds(Long.valueOf(x.getExpiresIn())))))))
//                                                   .build();
//                            var photoClient = PhotosLibraryClient.initialize(settings);
                            var result = photoClient.getMediaItem(mediaItem.getId());
                            return getMediaItem(result, contentType);
                          } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("failed refetch item {} {}", mediaItem.getFilename(), error);
                          }
                          return Mono.empty();
                        });
//               logger.info("GOT IDENTITY TOKEN {}", identityToken.getAccessToken());
//               return Mono.empty();
             })
             .doOnError(error -> logger.error("error downloading image from google {} {}", mediaItem.getFilename(), error))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.info("downloaded image from google {}", mediaItem.getFilename()));
  }

  private Mono<MediaItem> getMediaItem(MediaItem mediaItem, String contentType) {
    return Mono.fromCallable(() -> {
      var width = mediaItem.getMediaMetadata().getWidth();
      var height = mediaItem.getMediaMetadata().getHeight();
      BufferedImage image = null;
      image = ImageIO.read(new URL(mediaItem.getBaseUrl() + "=w" + width + "-h" + height));
      File outputfile = new File("/tmp/google_photos/" + mediaItem.getFilename());
      ImageIO.write(image, contentType, outputfile);
      image = null;
      return mediaItem;
    })//.doOnError(error -> logger.error("error downloading image from google {} {}", mediaItem.getFilename(), error))
             //     .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.info("downloaded image from google {}", mediaItem.getFilename()));
  }

  private Mono<MediaItem> downloadVideo(MediaItem mediaItem) {
    return Mono.fromCallable(() -> {
      logger.info("Downloading video {}", mediaItem.getFilename());
      ReadableByteChannel readableByteChannel = null;
      readableByteChannel = Channels.newChannel(new URL(mediaItem.getBaseUrl() + "=dv").openStream());
      FileOutputStream fileOutputStream = new FileOutputStream("/tmp/google_photos/" + mediaItem.getFilename());
      fileOutputStream.getChannel()
        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
      return mediaItem;
    }).doOnError(error -> logger.error("error downloading video from google {} {}", mediaItem.getFilename(), error))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.info("downloaded video from google {}", mediaItem.getFilename()));
  }

  private Mono<com.kooriim.pas.domain.MediaItem> uploadMediaItem(Jwt accessToken, MediaItem mediaItem) {
    if (mediaItem.getId() == null) {
      return Mono.empty();
    }
    return mediaItemService.processGoogleMediaItem(accessToken, mediaItem)
             .flatMap(mediaItemService::saveMediaItem)
             .doOnError(error -> logger.error("error saving mediaItem {} {}", mediaItem.getFilename(), error.getMessage()))
             .doOnNext(x -> logger.info("successfully processed google media item {}", mediaItem.getFilename()));
  }

  private String getContentType(String mimeType) {
    if (mimeType.toLowerCase().endsWith("png")) {
      return "png";
    } else if (mimeType.toLowerCase().endsWith("jpg") || mimeType.toLowerCase().endsWith("jpeg")) {
      return "jpg";
    } else if (mimeType.toLowerCase().endsWith("mp4")) {
      return "mp4";
    } else if (mimeType.toLowerCase().endsWith("mov")) {
      return "mov";
    } else if (mimeType.toLowerCase().endsWith("wmv")) {
      return "wmv";
    } else if (mimeType.toLowerCase().endsWith("avi")) {
      return "avi";
    } else if (mimeType.toLowerCase().endsWith("3pg")) {
      return "3pg";
    } else if (mimeType.toLowerCase().endsWith("mkv")) {
      return "mkv";
    }
    return "jpg";
  }

  public Mono<String> getUserAccessToken() {
    return ReactiveSecurityContextHolder
             .getContext()
             .doOnError(error -> logger.error("error authorizing user context {}", error))
             .publishOn(Schedulers.boundedElastic())
             .map(SecurityContext::getAuthentication)
             .cast(JwtAuthenticationToken.class)
             .map(JwtAuthenticationToken::getToken)
             .map(Jwt::getTokenValue)
             .doOnNext(x -> logger.info("Got User AccessToken"));
  }

  class DefaultHttpTransportFactory implements HttpTransportFactory {
    final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public HttpTransport create() {
      return HTTP_TRANSPORT;
    }
  }
}
