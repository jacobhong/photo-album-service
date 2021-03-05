package com.kooriim.pas.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.types.proto.MediaItem;
import com.kooriim.pas.domain.IdentityToken;
import org.apache.commons.lang3.StringUtils;
import org.h2.store.fs.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;

@Service
public class GoogleService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private WebClient webClient;

  public Mono<Void> syncGooglePhotos() {
    return getUserAccessToken()
             .flatMap(accessToken -> syncGooglePhotos(accessToken))
             .doOnNext(x -> logger.info("finishing sync google photos"))
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<Void> syncGooglePhotos(String accessToken) {
    logger.info("SYNCING GOOGLE PHOTOS {}", accessToken);
    // refresh tokens?
    return webClient.get()
             .uri("/auth/realms/kooriim-fe/broker/google/token")
             .accept(MediaType.APPLICATION_JSON)
             .header("Authorization", "Bearer " + accessToken)
             .retrieve()
             .bodyToMono(IdentityToken.class)
             .flatMap(identityToken -> getGooglePhotos(identityToken));
  }

  private Mono<Void> getGooglePhotos(IdentityToken identityToken) {
    logger.info("identity token: " + identityToken);
    try {
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
      final var photosLibraryClient = PhotosLibraryClient.initialize(settings);
      final var listMediaItemsPagedResponse = photosLibraryClient.listMediaItems();
      logger.info("response {}", listMediaItemsPagedResponse);
      final var list = new ArrayList<MediaItem>();
      listMediaItemsPagedResponse.getPage().getValues().forEach(v -> list.add(v));
      var nextPageToken = listMediaItemsPagedResponse.getNextPageToken();
      var nextPage = listMediaItemsPagedResponse.getPage().getNextPage();
      var index = 0; // TODO batch get here instead
//      while (StringUtils.isNotEmpty(nextPageToken)) {
//        try {
//          logger.info("getting next page " + nextPageToken);
      nextPage.getValues()
        .forEach(v -> list.add(v));
//          nextPageToken = nextPage.getNextPageToken();
//          nextPage = nextPage.getNextPage();
//          index++;
//        } catch (Exception e) {
//          logger.error("Error getting photos with pagination at index {}, {}", index, e);
//          // store index and try later
//        }
//      }
      try {
        list.forEach(v -> {
          var contentType = getContentType(v.getMimeType());
          if (contentType.contains("mp4") || contentType.contains("mov")) {
            downloadVideo(v, contentType);
          }
        });
      } catch (Exception e) {
        logger.error("error creating file {}", e);
      }
    } catch (IOException e) {
      logger.error("Error creating google credentials {}", e.getMessage());
    }
    return Mono.empty();
  }

  private void downloadImage(MediaItem mediaItem, String contentType) {
    var width = mediaItem.getMediaMetadata().getWidth();
    var height = mediaItem.getMediaMetadata().getHeight();
    BufferedImage image = null;
    try {
      logger.info("Downloading image {}", mediaItem.getFilename());
      image = ImageIO.read(new URL(mediaItem.getBaseUrl() + "=w" + width + "-h" + height));
      File outputfile = new File(mediaItem.getFilename());
      ImageIO.write(image, contentType, outputfile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void downloadVideo(MediaItem mediaItem, String contentType) {
    logger.info("Downloading video {}", mediaItem.getFilename());
    try {
      ReadableByteChannel readableByteChannel = null;
      readableByteChannel = Channels.newChannel(new URL(mediaItem.getBaseUrl() + "=dv").openStream());
      FileOutputStream fileOutputStream = new FileOutputStream("/tmp/" + mediaItem.getFilename());
      fileOutputStream.getChannel()
        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getContentType(String mimeType) {
    if (mimeType.toLowerCase().contains("png")) {
      return "png";
    } else if (mimeType.toLowerCase().contains("jpg")) {
      return "jpg";
    } else if (mimeType.toLowerCase().contains("mp4")) {
      return "mp4";
    } else if (mimeType.toLowerCase().contains("mov")) {
      return "mov";
    }
    return "jpg";
  }

  private Mono<String> getUserAccessToken() {
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
