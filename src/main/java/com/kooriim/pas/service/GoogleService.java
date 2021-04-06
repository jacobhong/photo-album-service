package com.kooriim.pas.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.types.proto.MediaItem;
import com.kooriim.pas.domain.IdentityToken;
import com.kooriim.pas.repository.MediaItemRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * alpha testing
 * TODO add sync by date
 * possibly add kafka
 */
@Service
public class GoogleService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private MediaItemService mediaItemService;
  @Autowired
  private WebClient webClient;
  @Autowired
  private MediaItemRepository mediaItemRepository;

  public Flux<com.kooriim.pas.domain.MediaItem> syncGooglePhotos() {
    return getUserAccessToken()
             .flatMap(accessToken -> getGoogleIdentityToken(accessToken))
             .flatMap(identityToken -> initializeS3PhotosLibraryClient(identityToken))
             .flatMapMany(photosLibraryClient -> getGooglePhotos(photosLibraryClient))
             .delayUntil(d -> Mono.delay(Duration.ofSeconds(5)))
             .flatMap(mediaItem -> downloadGoogleMediaItem(mediaItem))
             .flatMap(mediaItem -> uploadMediaItem(mediaItem))
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<IdentityToken> getGoogleIdentityToken(String accessToken) {
    logger.info("getting google identity token");
    // refresh tokens?
    return webClient.get()
             .uri("/auth/realms/kooriim-fe/broker/google/token")
             .accept(MediaType.APPLICATION_JSON)
             .header("Authorization", "Bearer " + accessToken)
             .retrieve()
             .bodyToMono(IdentityToken.class)
             .doOnError(error -> logger.error("Error getting google identity token {}", error.getMessage()))
             .doOnNext(x -> logger.info("got google identity token"));
  }

  private Flux<MediaItem> getGooglePhotos(PhotosLibraryClient photosLibraryClient) {
    return Flux.defer(() -> {
      logger.info("getting google photos.. may take awhile");

      final var listMediaItemsPagedResponse = photosLibraryClient.listMediaItems(ListMediaItemsRequest
                                                                                   .newBuilder()
                                                                                   .setPageSize(100)
                                                                                   .build());
      logger.info("response {}", listMediaItemsPagedResponse);
      final var list = new ArrayList<MediaItem>();
      listMediaItemsPagedResponse.getPage()
        .getValues()
        .forEach(mediaItem -> list.add(mediaItem));
      var nextPageToken = listMediaItemsPagedResponse.getNextPageToken();
      var nextPage = listMediaItemsPagedResponse.getPage().getNextPage();
      var index = 0; // TODO batch get here instead
      while (StringUtils.isNotEmpty(nextPageToken)) {
        logger.info("getting next page");
        nextPage.getValues()
          .forEach(mediaItem -> list.add(mediaItem));
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

  private Mono<PhotosLibraryClient> initializeS3PhotosLibraryClient(IdentityToken identityToken) {
    return Mono.fromCallable(() -> {
      logger.info("initializing photos library client");
      final var settings = PhotosLibrarySettings
                             .newBuilder()
                             .setCredentialsProvider(FixedCredentialsProvider
                                                       .create(UserCredentials
                                                                 .create(
                                                                   new AccessToken(
                                                                     identityToken.accessToken,
                                                                     Date.from(Instant.now().plusSeconds(Long.valueOf(identityToken.expiresIn)
                                                                     )))
                                                                 )
                                                       )
                             )
                             .build();
      return PhotosLibraryClient.initialize(settings);
    }).doOnError(e -> logger.error("error initializing photo client {}", e.getMessage()))
             .doOnNext(x -> logger.info("successfully initialized photos library client"));
  }

  private Mono<MediaItem> downloadGoogleMediaItem(MediaItem mediaItem) {
//    logger.info("checking if media item already exists{}", mediaItem.getFilename());
    return mediaItemService.getUserGoogleId()
             .flatMap(googleId -> mediaItemRepository.mediaItemExists(googleId, mediaItem.getFilename())
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
                                          return downloadImage(mediaItem, contentType);
                                        } else {
//                                          logger.info("medaItem does not exist, downloading {}", mediaItem.getFilename());
                                          return downloadVideo(mediaItem);
                                        }
                                      }
//                                      logger.info("SKIPPING DUPLICATE MEDIAITEM {}", mediaItem.getFilename());
//                                      throw new ConflictException("skipping google mediaItem because duplicate exists " + mediaItem.getFilename());
                                      return Mono.empty();
                                    }));

  }

  private Mono<MediaItem> downloadImage(MediaItem mediaItem, String contentType) {
    return Mono.fromCallable(() -> {
      logger.info("Downloading image {}", mediaItem.getFilename());
      var width = mediaItem.getMediaMetadata().getWidth();
      var height = mediaItem.getMediaMetadata().getHeight();
      BufferedImage image = null;
      image = ImageIO.read(new URL(mediaItem.getBaseUrl() + "=w" + width + "-h" + height));
      File outputfile = new File("/tmp/google_photos/" + mediaItem.getFilename());
      ImageIO.write(image, contentType, outputfile);
      return mediaItem;
    }).doOnError(error -> logger.error("error downloading image from google {} {}", mediaItem.getFilename(), error.getMessage()))
             .retryBackoff(20, Duration.ofMinutes(1))
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
      return mediaItem;
    }).doOnError(error -> logger.error("error downloading video from google {} {}", mediaItem.getFilename(), error.getMessage()))
             .retryBackoff(20, Duration.ofMinutes(1))
             .doOnNext(x -> logger.info("downloaded video from google {}", mediaItem.getFilename()));
  }

  private Mono<com.kooriim.pas.domain.MediaItem> uploadMediaItem(MediaItem mediaItem) {
    if (mediaItem.getId() == null) {
      return Mono.empty();
    }
    return mediaItemService.processGoogleMediaItem(mediaItem);
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
}
