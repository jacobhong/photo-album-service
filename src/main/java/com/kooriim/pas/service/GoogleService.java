package com.kooriim.pas.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.paging.AbstractPagedListResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.DateFilter;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.library.v1.proto.SearchMediaItemsRequest;
import com.google.photos.types.proto.DateRange;
import com.google.photos.types.proto.MediaItem;
import com.kooriim.pas.domain.GoogleTokenResponse;
import com.kooriim.pas.domain.MediaItemWithRefreshToken;
import com.kooriim.pas.domain.enums.ContentType;
import com.kooriim.pas.repository.MediaItemRepository;
import com.kooriim.pas.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import static com.kooriim.pas.domain.constant.Constants.S3_BUCKET_BASE_URL;
import static com.kooriim.pas.util.MediaItemUtil.compressPhoto;
import static com.kooriim.pas.util.MediaItemUtil.getMediaType;

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

  @Value("${s3.bucketName}")
  private String S3_BUCKET_NAME;

  @Autowired
  private MediaItemService mediaItemService;

  @Autowired
  private WebClient webClient;

  @Autowired
  @Qualifier("googleClient")
  private WebClient googleClient;

  @Autowired
  private MediaItemRepository mediaItemRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  @Qualifier("awsS3Client")
  private S3AsyncClient awsS3Client;

  /**
   * Download all MediaItems from google, upload to s3 and save record to database. Need add delay here
   * because google api limits how often we call them
   *
   * @return
   */
  public Flux<com.kooriim.pas.domain.MediaItem> syncGooglePhotos(Jwt jwt, Map<String, Instant> dates) {
    var user = userRepository.findByGoogleId(jwt.getClaimAsString("sub"));
    return getGoogleAccessToken(jwt)
             .flatMap(googleTokenResponse -> init(user.get().getRefreshToken(), googleTokenResponse, jwt))
             .flatMapMany(map -> getGooglePhotos(map, dates))
             .delayUntil(d -> Mono.delay(Duration.ofSeconds(1)))
             .flatMap(mediaItem -> downloadGoogleMediaItem(jwt, mediaItem))
             .flatMap(mediaItem -> processGoogleMediaItem(jwt, mediaItem))
             .flatMap(mediaItemService::saveMediaItem);
  }

  public Mono<GoogleTokenResponse> getGoogleAccessToken(Jwt accessToken) {
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

  public Mono<GoogleTokenResponse> refreshExchangeGoogle(String refreshToken) {
    logger.info("exchanging google refresh token for new accesstoken");
    return googleClient.post()
             .uri("/token", uriBuilder ->
                              uriBuilder.queryParam("client_id", googleClientId)
                                .queryParam("client_secret", googleSecret)
                                .queryParam("grant_type", "refresh_token")
                                .queryParam("refresh_token", refreshToken)
                                .build())
             .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
             .retrieve()
             .bodyToMono(GoogleTokenResponse.class)
             .doOnError(error -> logger.error("Error refreshExchangeGoogle {}", error))
             .doOnNext(token -> logger.info("Got new access token from google"));
  }

  private Mono<Map<String, Object>> init(String refreshToken, GoogleTokenResponse googleTokenResponse, Jwt jwt) {
    return Mono.fromCallable(() -> {
      final var settings = PhotosLibrarySettings
                             .newBuilder()
                             .setCredentialsProvider(FixedCredentialsProvider
                                                       .create(UserCredentials.create(new AccessToken(googleTokenResponse.getAccessToken(),
                                                         Date.from(Instant.now().plusSeconds(Long.valueOf(googleTokenResponse.getExpiresIn())))))))
                             .build();
      final var client = PhotosLibraryClient.initialize(settings);
      return Map.of("jwt", jwt, "client", client, "refreshToken", refreshToken);
    }).doOnError(e -> logger.error("error initializing photo client {}", e))
             .doOnNext(x -> logger.info("successfully initialized photos library client"));
  }

  private Flux<MediaItemWithRefreshToken> getGooglePhotos(Map<String, Object> map, Map<String, Instant> dates) {
    return Flux.defer(() -> {
      logger.info("getting google photos.. may take awhile");
      var photosLibraryClient = (PhotosLibraryClient) map.get("client");
      var refreshToken = (String) map.get("refreshToken");
      var jwt = (Jwt) map.get("jwt");
      AbstractPagedListResponse searchRequest = null;
      if (dates != null && dates.containsKey("startDate") && dates.containsKey("endDate")) {
        var startDate = LocalDate.ofInstant(dates.get("startDate"), ZoneId.of("UTC"));
        var endDate = LocalDate.ofInstant(dates.get("endDate"), ZoneId.of("UTC"));
        var dateRange = DateRange.newBuilder()
                          .setStartDate(com.google.type.Date.newBuilder()
                                          .setMonth(startDate.getMonthValue())
                                          .setDay(startDate.getDayOfMonth())
                                          .setYear(startDate.getYear())
                                          .build())
                          .setEndDate(com.google.type.Date.newBuilder()
                                        .setMonth(endDate.getMonthValue())
                                        .setDay(endDate.getDayOfMonth())
                                        .setYear(endDate.getYear())
                                        .build())
                          .build();
        var dateFilter = DateFilter.newBuilder()
                           .addRanges(dateRange)
                           .build();
        var filters = Filters.newBuilder()
                        .setDateFilter(dateFilter)
                        .build();
        searchRequest = photosLibraryClient.searchMediaItems(SearchMediaItemsRequest.newBuilder()
                                                               .setPageSize(100)
                                                               .setFilters(filters)
                                                               .build());
      } else {

        searchRequest = photosLibraryClient.listMediaItems(ListMediaItemsRequest
                                                             .newBuilder()
                                                             .setPageSize(100)
                                                             .build());
      }
      var list = fetchAllPages(refreshToken, searchRequest);
      logger.info("total mediaItems in google {}", list.size());

      photosLibraryClient.close();
      return Flux.fromIterable(list)
               .flatMap(mediaItemDupeCheck(jwt))
               .filter(mediaItemWithRefreshToken -> mediaItemWithRefreshToken != null)
               .collectList()
               .doOnNext(mediaItemWithRefreshTokens -> logger.info("{} new mediaItems", mediaItemWithRefreshTokens.size()))
               .flatMapIterable(mediaItemWithGoogleToken -> mediaItemWithGoogleToken);
    }).doOnError((e) -> logger.error("error getting google photos {}", e.getMessage()))
             .subscribeOn(Schedulers.elastic());
  }

  private ArrayList<MediaItemWithRefreshToken> fetchAllPages(String refreshToken, AbstractPagedListResponse mediaItemsPagedResponse) {
    final var list = new ArrayList<MediaItemWithRefreshToken>();
    mediaItemsPagedResponse.getPage()
      .getValues()
      .forEach(mediaItem -> list.add(new MediaItemWithRefreshToken((MediaItem) mediaItem, refreshToken)));

    var nextPageToken = mediaItemsPagedResponse.getNextPageToken();
    var nextPage = mediaItemsPagedResponse.getPage().getNextPage();
    var index = 0; // TODO batch get here instead
    while (StringUtils.isNotEmpty(nextPageToken)) {
      logger.info("getting next page");
      nextPage.getValues()
        .forEach(mediaItem -> list.add(new MediaItemWithRefreshToken((MediaItem) mediaItem, refreshToken)));
      nextPageToken = nextPage.getNextPageToken();
      nextPage = nextPage.getNextPage();
      index++;
      // store index  or date of last itemand try later
    }
    return list;
  }

  private Function<MediaItemWithRefreshToken, Publisher<? extends MediaItemWithRefreshToken>> mediaItemDupeCheck(Jwt jwt) {
    return mediaItemWithRefreshToken ->
             mediaItemRepository.mediaItemExists(jwt.getClaimAsString("sub"), mediaItemWithRefreshToken
                                                                                .getMediaItem()
                                                                                .getFilename())
               .flatMap(exists -> {
                 if (exists.getId() != null) {
                   return Mono.empty();
                 }
                 return Mono.just(mediaItemWithRefreshToken);
               }).doOnError(err -> logger.error("error doing dupe check {}", err.getMessage()));
  }

  private Mono<MediaItem> downloadGoogleMediaItem(Jwt accessToken, MediaItemWithRefreshToken mediaItemWithRefreshToken) {
    var mediaItem = mediaItemWithRefreshToken.getMediaItem();
    var contentType = ContentType.fromString(mediaItem.getMimeType()).getValue();
    var mediaType = getMediaType(mediaItem.getMimeType());
    final var googleTempDir = new File("/tmp/google_photos");
    if (!googleTempDir.exists()) {
      googleTempDir.mkdirs();
    }
    if (contentType.equalsIgnoreCase("gif")) {
      logger.warn("skipping gif {}", mediaItemWithRefreshToken.getMediaItem().getFilename());
      return Mono.empty();
    }
    if (mediaType.equalsIgnoreCase("photo")) {
      return getMediaItemImage(mediaItemWithRefreshToken, contentType);
    } else {
      return getMediaItemVideo(mediaItemWithRefreshToken);
    }
  }

  private Mono<MediaItem> getMediaItemImage(MediaItemWithRefreshToken mediaItemWithRefreshToken, String contentType) {
    logger.debug("Downloading image {}", mediaItemWithRefreshToken.getMediaItem().getFilename());
    var mediaItem = mediaItemWithRefreshToken.getMediaItem();
    return downloadImage(mediaItem, contentType)
             .onErrorResume(retryMediaItemDownload(mediaItemWithRefreshToken, contentType, mediaItem))
             .retryBackoff(20, Duration.ofMinutes(1));
  }

  private Mono<MediaItem> downloadImage(MediaItem mediaItem, String contentType) {
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
             .doOnNext(x -> logger.debug("downloaded image from google {}", mediaItem.getFilename()));
  }

  private Mono<MediaItem> getMediaItemVideo(MediaItemWithRefreshToken mediaItemWithRefreshToken) {
    logger.debug("Downloading image {}", mediaItemWithRefreshToken.getMediaItem().getFilename());
    var mediaItem = mediaItemWithRefreshToken.getMediaItem();
    return downloadVideo(mediaItem)
             .onErrorResume(retryMediaItemDownload(mediaItemWithRefreshToken, null, mediaItem))
             .retryBackoff(20, Duration.ofMinutes(1));
  }

  private Mono<MediaItem> downloadVideo(MediaItem mediaItem) {
    return Mono.fromCallable(() -> {
      logger.debug("Downloading video {}", mediaItem.getFilename());
      ReadableByteChannel readableByteChannel = null;
      readableByteChannel = Channels.newChannel(new URL(mediaItem.getBaseUrl() + "=dv").openStream());
      FileOutputStream fileOutputStream = new FileOutputStream("/tmp/google_photos/" + mediaItem.getFilename());
      fileOutputStream.getChannel()
        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
      return mediaItem;
    }).doOnError(error -> logger.error("error downloading video from google {} {}", mediaItem.getFilename(), error))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.debug("downloaded video from google {}", mediaItem.getFilename()));
  }

  private Function<Throwable, Mono<? extends MediaItem>> retryMediaItemDownload(MediaItemWithRefreshToken mediaItemWithRefreshToken, String contentType, MediaItem mediaItem) {
    return error -> {
      logger.error("download retrying because {}", error.getMessage());
      var refreshToken = mediaItemWithRefreshToken.getRefreshToken();
      return refreshExchangeGoogle(refreshToken)
               .flatMap(newAccessToken -> {
                 try {
                   final var settings = PhotosLibrarySettings
                                          .newBuilder()
                                          .setCredentialsProvider(FixedCredentialsProvider
                                                                    .create(UserCredentials.create(new AccessToken(newAccessToken.getAccessToken(),
                                                                      Date.from(Instant.now().plusSeconds(Long.valueOf(newAccessToken.getExpiresIn())))))))
                                          .build();
                   var photoClient = PhotosLibraryClient.initialize(settings);
                   var result = photoClient.getMediaItem(mediaItem.getId());
                   photoClient.close();
                   if (StringUtils.isNotEmpty(contentType)) {
                     return downloadImage(result, contentType);
                   } else {
                     return downloadVideo(result);
                   }
                 } catch (Exception e) {
                   logger.error("failed to retry downloading image {} {}", mediaItem.getFilename(), error);
                 }
                 return Mono.empty();
               });
    };
  }

  public Mono<com.kooriim.pas.domain.MediaItem> processGoogleMediaItem(Jwt accessToken, com.google.photos.types.proto.MediaItem mediaItem) {
    logger.debug("uploading mediaItem {}", mediaItem.getFilename());
    final var file = new File("/tmp/google_photos/" + mediaItem.getFilename());
    logger.debug("processing google media item {}", mediaItem.getFilename());
    final var contentType = ContentType.fromString(mediaItem.getFilename()).getValue();
    final var mediaType = getMediaType(mediaItem.getMimeType());
    if (contentType.equalsIgnoreCase("gif")) {
      logger.warn("skipping gif 2 {}", mediaItem.getFilename());
      return Mono.empty();
    }
    if (mediaType.equalsIgnoreCase("photo")) {
      return pushCompressedGooglePhotoToS3(mediaItem, accessToken.getClaimAsString("sub"), file, mediaType, contentType);
    } else {
      return pushGoogleVideoToS3(mediaItem, accessToken.getClaimAsString("sub"), file.getName(), contentType);
    }
  }

  private Mono<com.kooriim.pas.domain.MediaItem> pushGoogleVideoToS3(com.google.photos.types.proto.MediaItem mediaItem, String googleId, String fileName, String contentType) {
    return Mono.fromCallable(() -> {
      final var filePath = "/tmp/google_photos/" + fileName;
      final var tempFile = new File(filePath);
      final var thumbnailPath = ThumbnailGenerator.randomGrabberFFmpegImage(filePath, 2);
      final var tempThumbnailFile = new File(thumbnailPath);

      var thumbnailKey = "thumbnail." + thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);

      byte[] compressedThumbnailResult = compressPhoto("png", ImageIO.read(tempThumbnailFile), 360f, 270f);

      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var videoSize = Long.valueOf(tempFile.length()).intValue() / 1024;//kb

      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(fileName)
                              .build(), AsyncRequestBody.fromFile(tempFile)).whenComplete((response, err) -> tempFile.delete());

      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey) // check key hnanme
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult)).whenComplete((response, err) -> tempThumbnailFile.delete());
      return com.kooriim.pas.domain.MediaItem.newInstanceVideo(fileName,
        S3_BUCKET_BASE_URL + thumbnailKey,
        S3_BUCKET_BASE_URL + fileName,
        contentType,
        googleId,
        "video",
        thumbnailSize,
        videoSize,
        mediaItem);
    }).doOnError(error -> logger.error("error creating google video {} {}", fileName, error.getMessage()))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.debug("successfully created google video {}", fileName));
  }

  protected Mono<com.kooriim.pas.domain.MediaItem> pushCompressedGooglePhotoToS3(com.google.photos.types.proto.MediaItem mediaItem, String googleId, File file, String mediaType, String contentType) {
    return Mono.fromCallable(() -> {
      var image = ImageIO.read(file);
      byte[] compressedImageResult = compressPhoto(contentType, image, 1920f, 1080f);
      byte[] compressedThumbnailResult = compressPhoto(contentType, image, 360f, 270f);
      final var fileName = file.getName();
      final var thumbnailKey = "thumbnail." + fileName;
      final var compressedKey = "compressed." + fileName;
      final var originalKey = "original." + fileName;
      final var compressedSize = compressedImageResult.length / 1024;//kb
      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var originalSize = Long.valueOf(file.length()).intValue() / 1024;//kb
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(compressedKey)
                              .build(), AsyncRequestBody.fromBytes(compressedImageResult)).get();
      compressedImageResult = null;

      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey)
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult)).get();
      compressedThumbnailResult = null;
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(originalKey)
                              .build(), AsyncRequestBody.fromFile(file)).whenComplete((response, err) -> {
        if (err != null) {
          logger.error("failed uploading google photo {}", err);
        }
        file.delete();
      });
      return com.kooriim.pas.domain.MediaItem.newInstancePhoto(file.getName(),
        S3_BUCKET_BASE_URL + compressedKey,
        S3_BUCKET_BASE_URL + originalKey,
        S3_BUCKET_BASE_URL + thumbnailKey,
        contentType,
        googleId,
        mediaType,
        thumbnailSize,
        compressedSize,
        originalSize,
        mediaItem);

    })
             .doOnError(error -> logger.error("error creating google photo {} {}", file.getName(), error))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.debug("successfully created google photo {}", file.getName()));
  }
}
