package com.kooriim.pas.service;

import com.kooriim.pas.domain.MediaItem;
import com.kooriim.pas.domain.MediaItemMetaData;
import com.kooriim.pas.domain.enums.ContentType;
import com.kooriim.pas.domain.error.ConflictException;
import com.kooriim.pas.repository.MediaItemMetaDataRepository;
import com.kooriim.pas.repository.MediaItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kooriim.pas.domain.constant.Constants.S3_BUCKET_BASE_URL;
import static com.kooriim.pas.util.MediaItemUtil.*;
import static com.kooriim.pas.util.UserUtil.getUserGoogleId;

@Service
public class MediaItemService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${s3.bucketName}")
  private String S3_BUCKET_NAME;

  @Value("${image-directory}")
  private String IMAGE_DIR;

  @Autowired
  private MediaItemRepository mediaItemRepository;

  @Autowired
  private MediaItemMetaDataRepository mediaItemMetaDataRepository;

  @Autowired
  @Qualifier("awsS3Client")
  private S3AsyncClient awsS3Client;


  public Mono<MediaItem> getMediaItemById(Integer mediaItemId, Map<String, String> params) {
    return mediaItemRepository.getMediaItemById(mediaItemId)
             .flatMap(mediaItem -> setBase64Photo(params, mediaItem))
             .flatMap(this::setMetaData);
  }

  public Mono<byte[]> getVideoByTitleS3(String title) {
    return Mono.fromFuture(() -> {
      logger.debug("FETCHING s3 VIDEO BY title " + title);
      return awsS3Client.getObject(GetObjectRequest
                                     .builder()
                                     .key(title)
                                     .bucket(S3_BUCKET_NAME)
                                     .build(), AsyncResponseTransformer.toBytes());
    })
             .map(BytesWrapper::asByteArray);
  }

  public Flux<MediaItem> getMediaItems(Map<String, String> params, Pageable pageable) {
    return getUserGoogleId()
             .flatMapMany(name -> getMediaItems(params, pageable, name));
  }

  public Mono<MediaItem> createMediaItem(FilePart file) {
    return getUserGoogleId()
             .flatMap(googleId -> mediaItemRepository.mediaItemExists(googleId, file.filename())
                                    .flatMap(mediaItem -> {
                                      if (mediaItem.getId() == null) {
                                        return processMediaItem(googleId, file);
                                      } else {
                                        throw new ConflictException("Cannot create mediaItem that already exists " + file.filename());
                                      }
                                    })
                                    .flatMap(this::saveMediaItem));
  }

  public Mono<Void> deleteMediaItems(List<Integer> ids) {
    return mediaItemRepository.getMediaItemsByIds(ids)
             .collectList()
             .flatMap(photos -> {
               var deletePhotos = photos
                                    .stream()
                                    .map(photo -> ObjectIdentifier.builder()
                                                    .key(photo.getTitle())
                                                    .build())
                                    .collect(Collectors.toList());
               var deleteThumbnails = photos
                                        .stream()
                                        .map(photo -> ObjectIdentifier.builder()
                                                        .key("thumbnail." + photo.getTitle())
                                                        .build())
                                        .collect(Collectors.toList());
               var deleteOriginals = photos
                                       .stream()
                                       .map(photo -> ObjectIdentifier.builder()
                                                       .key("original." + photo.getTitle())
                                                       .build())
                                       .collect(Collectors.toList());
               deletePhotos.addAll(deleteThumbnails);
               deletePhotos.addAll(deleteOriginals);
               awsS3Client.deleteObjects(DeleteObjectsRequest
                                           .builder()
                                           .delete(Delete.builder().objects(deletePhotos).build())
                                           .bucket(S3_BUCKET_NAME)
                                           .build());
/**
 *
 * TODO delete metadata
 */
               return mediaItemMetaDataRepository
                        .deleteMetaDataByMediaItemIds(ids)
                        .flatMap(x -> mediaItemRepository.deleteByMediaItemIds(ids));
             }).then();
  }

  public Mono<Void> patchMediaItems(List<MediaItem> mediaItems) {
    return Flux.fromIterable(mediaItems)
             .flatMap(this::saveMediaItem)
             .then();
  }

  /**
   * TODO save originalDate on mediaItem if metadata exists
   *
   * @param mediaItem
   * @return
   */
  public Mono<MediaItem> saveMediaItem(MediaItem mediaItem) {
    return mediaItemRepository.save(mediaItem)
             .doOnNext(m -> {
               if (mediaItem.getMediaItemMetaData() != null) {
                 logger.debug("Saving metadata for mediaItem {}", mediaItem.getTitle());
                 mediaItem.getMediaItemMetaData().setMediaItemId(m.getId());
                 mediaItemMetaDataRepository.save(mediaItem.getMediaItemMetaData()).subscribe();
               }
             });
  }

  public Mono<MediaItem> setMetaData(MediaItem mediaItem) {
    return mediaItemMetaDataRepository.findByMediaItemId(mediaItem.getId())
             .flatMap(metaData -> {
               mediaItem.setMediaItemMetaData(metaData);
               return Mono.just(mediaItem);
             })
             .switchIfEmpty(Mono.just(mediaItem));
  }

  public Mono<MediaItemMetaData> createMetaData(MediaItemMetaData mediaItemMetaData) {
    return mediaItemRepository.updateMediaItemOriginalDate(mediaItemMetaData.getMediaItemId(), mediaItemMetaData.getCreatedDate())
             .flatMap(result -> mediaItemMetaDataRepository.save(mediaItemMetaData));
  }

  protected Mono<MediaItem> processMediaItem(String googleId, FilePart file) {
    final var contentType = ContentType.fromString(file.filename()).getValue();
    final var mediaType = getMediaType(contentType);
    var tmpDir = new File(IMAGE_DIR);
    if (!tmpDir.exists()) {
      tmpDir.mkdir();
    }
    if (mediaType.equalsIgnoreCase("photo")) {
      return pushCompressedPhotoToS3(googleId, file, mediaType, contentType);
    } else {
      return pushVideoToS3(googleId, file, contentType);
    }
  }


  protected Flux<MediaItem> getMediaItems(Map<String, String> params, Pageable pageable, String name) {
    if (params.containsKey("albumId")) {
      return mediaItemRepository
               .getMediaItemsByAlbumId(Integer.valueOf(params.get("albumId")), pageable)
               .flatMap(mediaItem -> setBase64Photo(params, mediaItem));
    } else {
      return mediaItemRepository
               .getMediaItemsByGoogleId(name, pageable)
               .flatMap(mediaItem -> setBase64Photo(params, mediaItem));
    }
  }

  protected Mono<MediaItem> setBase64Photo(Map<String, String> params, MediaItem mediaItem) {
    if (mediaItem.getMediaType().equalsIgnoreCase("video") || params.containsKey("thumbnail") && params.get("thumbnail").equalsIgnoreCase("true")) {
      return setBase64Thumbnail(mediaItem);
    }
    if (params.containsKey("compressedImage") && params.get("compressedImage").equalsIgnoreCase("true")) {
      return setBase64CompressedImage(mediaItem);
    }
    if (params.containsKey("originalImage") && params.get("originalImage").equalsIgnoreCase("true")) {
      return setBase64OriginalImage(mediaItem);
    }
    return Mono.just(mediaItem);
  }

  protected Mono<MediaItem> setBase64Thumbnail(MediaItem mediaItem) {
    return Mono.fromFuture(() -> {
      final var key = mediaItem.getMediaType().toLowerCase().equals("photo") ?
                        "thumbnail." + mediaItem.getTitle() :
                        mediaItem.getThumbnailFilePath()
                          .substring(mediaItem.getThumbnailFilePath()
                                       .lastIndexOf("/") + 1);
      return awsS3Client.getObject(GetObjectRequest
                                     .builder()
                                     .key(key)
                                     .bucket(S3_BUCKET_NAME)
                                     .build(), AsyncResponseTransformer.toBytes());
    }).flatMap(response -> {
      mediaItem.setBase64ThumbnailImage(generateBase64Image(mediaItem, response.asByteArray()));
      return Mono.just(mediaItem);
    }).doOnNext(result -> logger.debug("fetched thumbnail image from s3 {}", mediaItem.getThumbnailFilePath()))
             .doOnError(error -> logger.error("Error setting base64 thumbnail {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  protected Mono<MediaItem> setBase64CompressedImage(MediaItem mediaItem) {
    return Mono.fromFuture(() -> awsS3Client.getObject(GetObjectRequest
                                                         .builder()
                                                         .key("compressed." + mediaItem
                                                                                .getTitle())
                                                         .bucket(S3_BUCKET_NAME)
                                                         .build(), AsyncResponseTransformer.toBytes()))
             .flatMap(response -> {
               mediaItem.setBase64CompressedImage(generateBase64Image(mediaItem, response.asByteArray()));
               return Mono.just(mediaItem);
             }).
                 doOnNext(result -> logger.debug("fetched compressed image from s3 {}", result.getCompressedImageFilePath()))
             .doOnError(error -> logger.error("Error setting setBase64CompressedImage {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  protected Mono<MediaItem> setBase64OriginalImage(MediaItem mediaItem) {
    return Mono.fromFuture(() -> awsS3Client.getObject(GetObjectRequest
                                                         .builder()
                                                         .key("original." + mediaItem
                                                                              .getTitle())
                                                         .bucket(S3_BUCKET_NAME)
                                                         .build(), AsyncResponseTransformer.toBytes()))
             .flatMap(response -> {
               mediaItem.setBase64OriginalImage(generateBase64Image(mediaItem, response.asByteArray()));
               return Mono.just(mediaItem);
             }).
                 doOnNext(result -> logger.debug("fetched original image from s3 {}", result.getOriginalImageFilePath()))
             .doOnError(error -> logger.error("Error setting setBase64OriginalImage {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }


  private Mono<MediaItem> pushVideoToS3(String googleId, FilePart file, String contentType) {
    return Mono.fromCallable(() -> {
      final var filePath = IMAGE_DIR + "/" + file.filename();
      final var tempFile = new File(filePath);
      file.transferTo(tempFile);

      final var thumbnailPath = ThumbnailGenerator.randomGrabberFFmpegImage(filePath, 2);
      final var tempThumbnailFile = new File(thumbnailPath);
      final byte[] compressedThumbnailResult = compressPhoto("png", ImageIO.read(tempThumbnailFile), 360f, 270f);

      var thumbnailKey = thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);
      thumbnailKey = "thumbnail." + thumbnailKey.substring(0, thumbnailKey.lastIndexOf(".")) + ".png";

      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var videoSize = Long.valueOf(tempFile.length()).intValue() / 1024;//kb

      logger.debug("pushing video to s3 {}", file.filename());
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(file.filename())
                              .build(), AsyncRequestBody.fromFile(tempFile)).whenComplete((response, err) -> tempFile.delete());
      logger.debug("pushing thumbnail to s3 {}", file.filename());
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey)
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult)).whenComplete((response, err) -> tempThumbnailFile.delete());
      return MediaItem.newInstanceVideo(file.filename(),
        S3_BUCKET_BASE_URL + thumbnailKey,
        S3_BUCKET_BASE_URL + file.filename(),
        contentType,
        googleId,
        "video",
        thumbnailSize,
        videoSize,
        null);
    }).doOnError(error -> logger.error("error creating  video {} {}", file.filename(), error.getMessage()))
             .doOnNext(x -> logger.info("successfully created  video {}", file.filename()));
  }


  private Mono<MediaItem> pushCompressedPhotoToS3(String name, FilePart file, String mediaType, String contentType) {
    return Mono.fromCallable(() -> {
      var tempFile = File.createTempFile(file.filename(), contentType, new File(IMAGE_DIR + "/"));
      file.transferTo(tempFile);
      var image = ImageIO.read(tempFile);
      byte[] compressedImageResult = compressPhoto(contentType, image, 1920f, 1080f);
      byte[] compressedThumbnailResult = compressPhoto(contentType, image, 360f, 270f);
      final var fileName = file.filename();
      final var thumbnailKey = "thumbnail." + fileName;
      final var compressedKey = "compressed." + fileName;
      final var originalKey = "original." + fileName;
      final var compressedSize = compressedImageResult.length / 1024;//kb
      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var originalSize = Long.valueOf(tempFile.length()).intValue() / 1024;//kb
      logger.debug("pushing compressed  photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(compressedKey)
                              .build(), AsyncRequestBody.fromBytes(compressedImageResult));

      logger.debug("pushing thumbnail  photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey)
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult));

      logger.debug("pushing google  photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(originalKey)
                              .build(), AsyncRequestBody.fromFile(tempFile)).whenComplete((response, err) -> tempFile.delete());
      return MediaItem.newInstancePhoto(file.filename(),
        S3_BUCKET_BASE_URL + compressedKey,
        S3_BUCKET_BASE_URL + originalKey,
        S3_BUCKET_BASE_URL + thumbnailKey,
        contentType,
        name,
        mediaType,
        thumbnailSize,
        compressedSize,
        originalSize,
        null);

    }).doOnError(error -> logger.error("error creating photo {} {}", file.filename(), error.getMessage()))
             .doOnNext(x -> logger.info("successfully created photo {}", file.filename()));
  }


}
