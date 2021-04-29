package com.kooriim.pas.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Clock;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.DateFilter;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.types.proto.DateRange;
import com.google.photos.types.proto.MediaItem;
import com.kooriim.pas.domain.GoogleTokenResponse;
import com.kooriim.pas.domain.MediaItemWithGoogleToken;
import com.kooriim.pas.repository.MediaItemRepository;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

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
    return getGoogleIdentityToken(accessToken)
             .flatMap(googleTokenResponse -> init(googleTokenResponse, accessToken))
             .flatMapMany(this::getGooglePhotos)
             .delayUntil(d -> Mono.delay(Duration.ofSeconds(6)))
             .flatMap(mediaItem -> downloadGoogleMediaItem(accessToken, mediaItem))
             .flatMap(mediaItem -> mediaItemService.processGoogleMediaItem(accessToken, mediaItem))
             .flatMap(mediaItemService::saveMediaItem)
             .subscribeOn(Schedulers.elastic());
  }

  private Mono<GoogleTokenResponse> getGoogleIdentityToken(Jwt accessToken) {
    logger.info("getting google identity token");
    return webClient.get()
             .uri("/auth/realms/kooriim-fe/broker/google/token?access_type=offline")
             .accept(MediaType.APPLICATION_JSON)
             .header("Authorization", "Bearer " + accessToken.getTokenValue())
             .retrieve()
             .bodyToMono(GoogleTokenResponse.class)
             .doOnError(error -> logger.error("Error getting google identity token {}", error.getMessage()))
             .doOnNext(token -> logger.info("got token {}", token));
  }

//  public Mono<GoogleTokenResponse> tokenExchangeGoogle(GoogleTokenResponse accessToken) {
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
//             .bodyToMono(GoogleTokenResponse.class)
//             .doOnError(error -> logger.error("Error getting tokenExchangeGoogle {}", error))
//             .doOnNext(token -> logger.info("Got tokenExchangeGoogle exchange from google {}", token));
//  }

  public Mono<GoogleTokenResponse> refreshExchangeGoogle(GoogleTokenResponse accessToken) {
    logger.info("exchanging google refresh token for new accesstoken");
    return googleClient.post()
             .uri("/token", uriBuilder ->
                              uriBuilder.queryParam("client_id", googleClientId)
                                .queryParam("client_secret", googleSecret)
                                .queryParam("grant_type", "refresh_token")
                                .queryParam("refresh_token", accessToken.getRefreshToken())
                                .build())
             .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
             .retrieve()
             .bodyToMono(GoogleTokenResponse.class)
             .doOnError(error -> logger.error("Error refreshExchangeGoogle {}", error))
             .doOnNext(token -> logger.info("Got new access token from google {}", token));
  }

  private Mono<Map<String, Object>> init(GoogleTokenResponse googleTokenResponse, Jwt accessToken) {
    return Mono.fromCallable(() -> {
      final var settings = PhotosLibrarySettings
                             .newBuilder()
                             .setCredentialsProvider(FixedCredentialsProvider
                                                       .create(UserCredentials.create(new AccessToken(googleTokenResponse.getAccessToken(),
                                                         Date.from(Instant.now().plusSeconds(Long.valueOf(googleTokenResponse.getExpiresIn())))))))
                             .build();
      final var client = PhotosLibraryClient.initialize(settings);
      return Map.of("jwt", accessToken, "client", client, "googleTokenResponse", googleTokenResponse);
    }).doOnError(e -> logger.error("error initializing photo client {}", e))
             .doOnNext(x -> logger.info("successfully initialized photos library client"));
  }

  // TODO sync by date range?
  private Flux<MediaItemWithGoogleToken> getGooglePhotos(Map<String, Object> map) {
    return Flux.defer(() -> {
      logger.info("getting google photos.. may take awhile");
      var photosLibraryClient = (PhotosLibraryClient) map.get("client");
      var identityToken = (GoogleTokenResponse) map.get("googleTokenResponse");
      var jwt = (Jwt) map.get("jwt");
      final var listMediaItemsPagedResponse = photosLibraryClient.listMediaItems(ListMediaItemsRequest
                                                                                   .newBuilder()
                                                                                   .setPageSize(100)
                                                                                   .build());
      final var list = new ArrayList<MediaItemWithGoogleToken>();
      listMediaItemsPagedResponse.getPage()
        .getValues()
        .forEach(mediaItem -> list.add(new MediaItemWithGoogleToken(mediaItem, identityToken)));

      var nextPageToken = listMediaItemsPagedResponse.getNextPageToken();
      var nextPage = listMediaItemsPagedResponse.getPage().getNextPage();
      var index = 0; // TODO batch get here instead
      while (StringUtils.isNotEmpty(nextPageToken)) {
        logger.info("getting next page");
        nextPage.getValues()
          .forEach(mediaItem -> list.add(new MediaItemWithGoogleToken(mediaItem, identityToken)));
        nextPageToken = nextPage.getNextPageToken();
        nextPage = nextPage.getNextPage();
        index++;
        // store index  or date of last itemand try later
      }
      logger.info("total mediaItems in google {}", list.size());
      photosLibraryClient.close();
      return Flux.fromIterable(list)
               .flatMap(mediaItemDupeCheck(jwt))
               .filter(mediaItemWithGoogleToken -> mediaItemWithGoogleToken != null)
               .collectList()
               .doOnNext(mediaItemWithGoogleToken -> logger.info("{} new mediaItems", mediaItemWithGoogleToken.size()))
               .flatMapIterable(mediaItemWithGoogleToken -> mediaItemWithGoogleToken);
    }).doOnError((e) -> logger.error("error getting google photos {}", e.getMessage()))
             .subscribeOn(Schedulers.elastic());
  }

  private Function<MediaItemWithGoogleToken, Publisher<? extends MediaItemWithGoogleToken>> mediaItemDupeCheck(Jwt jwt) {
    return mediaItemWithGoogleToken ->
             mediaItemRepository.mediaItemExists(jwt.getClaimAsString("sub"), mediaItemWithGoogleToken
                                                                                .getMediaItem()
                                                                                .getFilename())
               .flatMap(exists -> {
                 if (exists.getId() != null) {
                   logger.info("dupe found {}", exists.getTitle());
                   return Mono.empty();
                 }
                 return Mono.just(mediaItemWithGoogleToken);
               }).doOnError(err -> logger.error("error doing dupe check {}", err.getMessage()))
      ;
  }

  private Flux<MediaItemWithGoogleToken> getGooglePhotosByDate(Map<String, Object> map) {
    return Flux.defer(() -> {
      logger.info("getting google photos.. may take awhile");
      var photosLibraryClient = (PhotosLibraryClient) map.get("client");
      var identityToken = (GoogleTokenResponse) map.get("googleTokenExchangeResponse");
// Create a new com.google.type.Date object using a builder
// Note that there are different valid combinations as described above
      var dayFebruary15 = com.google.type.Date.newBuilder()
                            .setDay(15)
                            .setMonth(2)
                            .build();
// Create a new dateFilter. You can also set multiple dates here
      var day2013 = com.google.type.Date.newBuilder()
                      .setYear(2013)
                      .build();
// Create a new com.google.type.Date object for November 2011
      var day2011November = com.google.type.Date.newBuilder()
                              .setMonth(11)
                              .setYear(2011)
                              .build();
// Create a date range for January to March
      var dateRangeJanuaryToMarch = DateRange.newBuilder()
                                      .setStartDate(com.google.type.Date.newBuilder().setMonth(1).build())
                                      .setEndDate(com.google.type.Date.newBuilder().setMonth(3).build())
                                      .build();
// Create a date range for March 24 to May 2
      var dateRangeMarch24toMay2 = DateRange.newBuilder()
                                     .setStartDate(com.google.type.Date.newBuilder().setMonth(3).setDay(24).build())
                                     .setEndDate(com.google.type.Date.newBuilder().setMonth(5).setDay(2).build())
                                     .build();
// Create a new dateFilter with the dates and date ranges
      var dateFilter = DateFilter.newBuilder()
                         .addDates(day2013)
                         .addDates(day2011November)
                         .addRanges(dateRangeJanuaryToMarch)
                         .addRanges(dateRangeMarch24toMay2)
                         .build();
// Create a new Filters object
      var filters = Filters.newBuilder()
                      .setDateFilter(dateFilter)
                      .build();
      final var listMediaItemsPagedResponse = photosLibraryClient.listMediaItems(ListMediaItemsRequest
                                                                                   .newBuilder()

                                                                                   .setPageSize(100)
                                                                                   .build());
      logger.info("response {}", listMediaItemsPagedResponse);
      final var list = new ArrayList<MediaItemWithGoogleToken>();
      listMediaItemsPagedResponse.getPage()
        .getValues()
        .forEach(mediaItem -> list.add(new MediaItemWithGoogleToken(mediaItem, identityToken)));

      var nextPageToken = listMediaItemsPagedResponse.getNextPageToken();
      var nextPage = listMediaItemsPagedResponse.getPage().getNextPage();
      var index = 0; // TODO batch get here instead
      while (StringUtils.isNotEmpty(nextPageToken)) {
        logger.info("getting next page");
        nextPage.getValues()
          .forEach(mediaItem -> list.add(new MediaItemWithGoogleToken(mediaItem, identityToken)));
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


  private Mono<MediaItem> downloadGoogleMediaItem(Jwt accessToken, MediaItemWithGoogleToken mediaItemWithGoogleToken) {
    var mediaItem = mediaItemWithGoogleToken.getMediaItem();
    var contentType = getContentType(mediaItem.getMimeType());
    var mediaType = mediaItemService.getMediaType(mediaItem.getMimeType());
    final var googleTempDir = new File("/tmp/google_photos");
    if (!googleTempDir.exists()) {
      googleTempDir.mkdirs();
    }
    if (mediaType.equalsIgnoreCase("photo")) {
      return downloadImage(mediaItemWithGoogleToken, contentType);
    } else {
      return downloadVideo(mediaItem);
    }
  }

  private Mono<MediaItem> downloadImage(MediaItemWithGoogleToken mediaItemWithGoogleToken, String contentType) {
    logger.info("Downloading image {}", mediaItemWithGoogleToken.getMediaItem().getFilename());
    var mediaItem = mediaItemWithGoogleToken.getMediaItem();
    return getMediaItemImage(mediaItem, contentType)
             .onErrorResume(error -> {
               logger.error("download retrying because {}", error.getMessage());
               var googleTokenResponse = mediaItemWithGoogleToken.getGoogleTokenResponse();
               return refreshExchangeGoogle(googleTokenResponse)
                        .flatMap(newAccessToken -> {
                          try {
                            logger.info("refreshing googleTokenResponse {}", googleTokenResponse);
                            final var settings = PhotosLibrarySettings
                                                   .newBuilder()
                                                   .setCredentialsProvider(FixedCredentialsProvider
                                                                             .create(UserCredentials.create(new AccessToken(newAccessToken.getAccessToken(),
                                                                               Date.from(Instant.now().plusSeconds(Long.valueOf(newAccessToken.getExpiresIn())))))))
                                                   .build();
                            var photoClient = PhotosLibraryClient.initialize(settings);
                            var result = photoClient.getMediaItem(mediaItem.getId());
                            photoClient.close();
                            return getMediaItemImage(result, contentType);
                          } catch (Exception e) {
                            logger.error("failed to retry downloading image {} {}", mediaItem.getFilename(), error);
                          }
                          return Mono.empty();
                        });
             })
             .retryBackoff(20, Duration.ofMinutes(1));
  }

  private Mono<MediaItem> getMediaItemImage(MediaItem mediaItem, String contentType) {
    return Mono.fromCallable(() -> {
      var width = mediaItem.getMediaMetadata().getWidth();
      var height = mediaItem.getMediaMetadata().getHeight();
      BufferedImage image = null;
      image = ImageIO.read(new URL(mediaItem.getBaseUrl() + "=w" + width + "-h" + height));
      File outputfile = new File("/tmp/google_photos/" + mediaItem.getFilename());
      ImageIO.write(image, contentType, outputfile);
      image = null;
      return mediaItem;
    }).doOnError(error -> logger.error("error downloading image from google {} {}", mediaItem.getFilename(), error))
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

//  public Mono<String> getUserAccessToken() {
//    return ReactiveSecurityContextHolder
//             .getContext()
//             .doOnError(error -> logger.error("error authorizing user context {}", error))
//             .publishOn(Schedulers.boundedElastic())
//             .map(SecurityContext::getAuthentication)
//             .cast(JwtAuthenticationToken.class)
//             .map(JwtAuthenticationToken::getToken)
//             .map(Jwt::getTokenValue)
//             .doOnNext(x -> logger.info("Got User AccessToken"));
//  }
//
//  class DefaultHttpTransportFactory implements HttpTransportFactory {
//    final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
//
//    public HttpTransport create() {
//      return HTTP_TRANSPORT;
//    }
//
//  }
//
//  private boolean shouldRefresh(GoogleTokenResponse googleTokenResponse) {
//    Long expiresIn = getExpiresInMilliseconds(googleTokenResponse);
//    return expiresIn <= 60000L * 5L;
//  }
//
//  private Long getExpiresInMilliseconds(GoogleTokenResponse googleTokenResponse) {
//    var date = Date.from(Instant.now().plusSeconds(Long.valueOf(googleTokenResponse.getExpiresIn())));
//    return (date.getTime() - Clock.SYSTEM.currentTimeMillis());
//  }
}
