package com.kooriim.pas.service;

import com.kooriim.pas.domain.MediaItem;
import com.kooriim.pas.domain.MediaItemMetaData;
import com.kooriim.pas.domain.error.ConflictException;
import com.kooriim.pas.repository.MediaItemMetaDataRepository;
import com.kooriim.pas.repository.MediaItemRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MediaItemService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String S3_BUCKET_BASE_URL = "https://kooriim-images.s3-us-west-1.amazonaws.com/";

  @Value("${s3.bucketName}")
  private String S3_BUCKET_NAME;

  @Value("${image-directory}")
  private String imgDir;

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

//  public Mono<byte[]> getVideoByTitle(String title) {
//    return getVideoByTitleS3(title);
//  }

  public Mono<byte[]> getVideoByTitleS3(String title) {
    return Mono.fromFuture(() -> {
      logger.info("FETCHING s3 VIDEO BY title " + title);
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
                                    .doOnError(e -> logger.error(e.getMessage()))
                                    .doOnNext(x -> logger.info("finished creating media item {}", x.getTitle()))
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

               return mediaItemRepository.deleteByMediaItemIds(ids).then();
             }).doOnNext(result -> logger.info("Deleted photos from s3"))
             .doOnError(error -> logger.error("Error deleting photos {}, {}", error.getMessage(), ids))
             .then();
  }

  public Mono<Void> patchPhotos(List<MediaItem> mediaItems) {
    return Flux.fromIterable(mediaItems)
             .flatMap(this::saveMediaItem)
             .then();
  }

  public Mono<MediaItem> setBase64Thumbnail(MediaItem mediaItem) {
    return Mono.fromFuture(() -> {
      logger.info("inside setting base64 thumbnail mediaItem {}", mediaItem.getTitle());
      final var key = mediaItem.getMediaType().toLowerCase().equals("photo") ?
                        "thumbnail." + mediaItem.getTitle() :
                        mediaItem.getThumbnailFilePath()
                          .substring(mediaItem.getThumbnailFilePath()
                                       .lastIndexOf("/") + 1);
      logger.info("fetching s3 mediaItem by key {}", key);
      return awsS3Client.getObject(GetObjectRequest
                                     .builder()
                                     .key(key)
                                     .bucket(S3_BUCKET_NAME)
                                     .build(), AsyncResponseTransformer.toBytes());
    }).flatMap(response -> {
      mediaItem.setBase64ThumbnailImage(generateBase64Image(mediaItem, response.asByteArray()));
      return Mono.just(mediaItem);
    }).doOnNext(result -> logger.info("fetched thumbnail image from s3 {}", mediaItem.getThumbnailFilePath()))
             .doOnError(error -> logger.error("Error setting base64 thumbnail {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<MediaItem> setBase64CompressedImage(MediaItem mediaItem) {
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
                 doOnNext(result -> logger.info("fetched compressed image from s3 {}", result.getCompressedImageFilePath()))
             .doOnError(error -> logger.error("Error setting setBase64CompressedImage {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<MediaItem> setBase64OriginalImage(MediaItem mediaItem) {
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
                 doOnNext(result -> logger.info("fetched original image from s3 {}", result.getOriginalImageFilePath()))
             .doOnError(error -> logger.error("Error setting setBase64OriginalImage {} for photoId {}", error.getMessage(), mediaItem.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.boundedElastic());
  }


  public Mono<MediaItem> processGoogleMediaItem(Jwt accessToken, com.google.photos.types.proto.MediaItem mediaItem) {
//    return getUserGoogleId().flatMap(googleId -> {
    logger.info("uploading mediaItem {}", mediaItem.getFilename());

    final var file = new File("/tmp/google_photos/" + mediaItem.getFilename());
    logger.info("processing google media item {}", mediaItem.getFilename());
    final var contentType = getContentType(mediaItem.getFilename().toLowerCase());
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
//    }).flatMap(this::saveMediaItem)
//             .doOnError(error -> logger.error("error saving mediaItem {} {}", mediaItem.getFilename(), error.getMessage()))
//             .doOnNext(x -> logger.info("successfully processed google media item {}", mediaItem.getFilename()));
  }

  private Flux<MediaItem> getMediaItems(Map<String, String> params, Pageable pageable, String name) {

    if (params.containsKey("albumId")) {
      return mediaItemRepository
               .getMediaItemsByAlbumId(Integer.valueOf(params.get("albumId")), pageable)
               .flatMap(mediaItem -> setBase64Photo(params, mediaItem))
               .doOnNext(mediaItem -> logger.info("getMediaItems by albumId: {}", mediaItem.getTitle()));
    } else {
      return mediaItemRepository
               .getMediaItemsByGoogleId(name, pageable)
               .flatMap(mediaItem -> setBase64Photo(params, mediaItem))
               .doOnNext(mediaItem -> logger.info("getMediaItems by googleId: {}", mediaItem.getTitle()));
    }
  }

  private Mono<MediaItem> setBase64Photo(Map<String, String> params, MediaItem mediaItem) {
    if (params.containsKey("thumbnail") && params.get("thumbnail").equalsIgnoreCase("true")) {
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

  private Mono<MediaItem> setMetaData(MediaItem mediaItem) {
    return mediaItemMetaDataRepository.findByMediaItemId(mediaItem.getId())
             .flatMap(metaData -> {
               mediaItem.setMediaItemMetaData(metaData);
               return Mono.just(mediaItem);
             }).doOnError(error -> logger.error("error finding metadata for mediaItem {} {}", mediaItem.getTitle(), error.getMessage()))
             .doOnNext(x -> logger.info("got metaData for mediaItem {}", mediaItem.getTitle()))
             .switchIfEmpty(Mono.just(mediaItem));
  }


  private Mono<MediaItem> processMediaItem(String googleId, FilePart file) {
    final var contentType = getContentType(file.filename().toLowerCase());
    final var mediaType = getMediaType(contentType);
    var tmpDir = new File(imgDir);
    if (!tmpDir.exists()) {
      tmpDir.mkdir();
    }
    if (mediaType.equalsIgnoreCase("photo")) {
      return pushCompressedPhotoToS3(googleId, file, mediaType, contentType);
    } else {
      return pushVideoToS3(googleId, file, contentType);
    }
  }

  public String getMediaType(String contentType) {
    return contentType.toLowerCase().endsWith("jpeg") ||
             contentType.toLowerCase().endsWith("jpg") ||
             contentType.toLowerCase().endsWith("png") ? "photo" : "video";
  }

  private Mono<MediaItem> pushVideoToS3(String googleId, FilePart file, String contentType) {
    return Mono.fromCallable(() -> {
      final var filePath = imgDir + "/" + file.filename();
      final var tempFile = new File(filePath);
      file.transferTo(tempFile);

      final var thumbnailPath = ThumbnailGenerator.randomGrabberFFmpegImage(filePath, 2);
      final var tempThumbnailFile = new File(thumbnailPath);
      final byte[] compressedThumbnailResult = compressPhoto("png", ImageIO.read(tempThumbnailFile), 360f, 270f);

      var thumbnailKey = thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);
      thumbnailKey = thumbnailKey.substring(0, thumbnailKey.lastIndexOf(".")) + ".thumbnail.png";

      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var videoSize = Long.valueOf(tempFile.length()).intValue() / 1024;//kb

      logger.info("pushing video to s3 {}", file.filename());
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(file.filename())
                              .build(), AsyncRequestBody.fromFile(tempFile)).whenComplete((response, err) -> tempFile.delete());
      logger.info("pushing thumbnail to s3 {}", file.filename());
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key("thumbnail." + thumbnailKey)
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

  private Mono<MediaItem> pushGoogleVideoToS3(com.google.photos.types.proto.MediaItem mediaItem, String googleId, String fileName, String contentType) {
    return Mono.fromCallable(() -> {
      logger.info("creating google video {}", fileName);
      final var filePath = "/tmp/google_photos/" + fileName;
      final var tempFile = new File(filePath);
      final var thumbnailPath = ThumbnailGenerator.randomGrabberFFmpegImage(filePath, 2);
      final var tempThumbnailFile = new File(thumbnailPath);

      var thumbnailKey = "thumbnail." + thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);

      byte[] compressedThumbnailResult = compressPhoto("png", ImageIO.read(tempThumbnailFile), 360f, 270f);

      final var thumbnailSize = compressedThumbnailResult.length / 1024;//kb
      final var videoSize = Long.valueOf(tempFile.length()).intValue() / 1024;//kb

      logger.info("pushing google video to s3 {}", fileName);
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(fileName)
                              .build(), AsyncRequestBody.fromFile(tempFile)).whenComplete((response, err) -> tempFile.delete());

      logger.info("pushing google video thumbnail to s3 {}", fileName);
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey) // check key hnanme
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult)).whenComplete((response, err) -> tempThumbnailFile.delete());
      return MediaItem.newInstanceVideo(fileName,
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
             .doOnNext(x -> logger.info("successfully created google video {}", fileName));
  }

  private Mono<MediaItem> pushCompressedPhotoToS3(String name, FilePart file, String mediaType, String contentType) {
    return Mono.fromCallable(() -> {
      var tempFile = File.createTempFile(file.filename(), contentType, new File(imgDir + "/"));
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
      logger.info("pushing compressed  photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(compressedKey)
                              .build(), AsyncRequestBody.fromBytes(compressedImageResult));

      logger.info("pushing thumbnail  photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey)
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult));

      logger.info("pushing google  photo to s3");
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

  private Mono<MediaItem> pushCompressedGooglePhotoToS3(com.google.photos.types.proto.MediaItem mediaItem, String googleId, File file, String mediaType, String contentType) {
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
      logger.info("pushing compressed google photo to s3 {}", fileName);
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(compressedKey)
                              .build(), AsyncRequestBody.fromBytes(compressedImageResult)).get();
      compressedImageResult = null;

      logger.info("pushing thumbnail google photo to s3 {}", fileName);
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(thumbnailKey)
                              .build(), AsyncRequestBody.fromBytes(compressedThumbnailResult)).get();
      compressedThumbnailResult = null;
      logger.info("pushing original google photo to s3 {}", fileName);
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
      return MediaItem.newInstancePhoto(file.getName(),
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
             .doOnNext(x -> logger.info("successfully created google photo {}", file.getName()));
  }

  private byte[] compressPhoto(String contentType, BufferedImage image, float aspectWidth, float aspectHeight) throws
    IOException {
    final var widthRatio = aspectWidth / image.getWidth();
    final var heightRatio = aspectHeight / image.getHeight();
    final var ratio = Math.min(widthRatio, heightRatio);
    final var width = image.getWidth() * ratio;
    final var height = image.getHeight() * ratio;
    image = Thumbnails.of(image)
              .size(Math.round(width), Math.round(height))
              .asBufferedImage();
    var byteOutputStream = new ByteArrayOutputStream();
    ImageIO.setUseCache(false);
    var writers = ImageIO.getImageWritersByFormatName(contentType);
    var writer = writers.next();
    var ios = ImageIO.createImageOutputStream(byteOutputStream);
    writer.setOutput(ios);
    final var param = writer.getDefaultWriteParam();

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.75f);  // Change the quality value you prefer
    writer.write(null, new IIOImage(image, null, null), param);
    final var compressedImageResult = byteOutputStream.toByteArray();
    writer.dispose();
    byteOutputStream.close();
    ios.close();
    writer.dispose();
    return compressedImageResult;
  }

  private String generateBase64Image(MediaItem mediaItem, byte[] bytes) {
    final var contentType = mediaItem.getMediaType().equals("photo") ? mediaItem.getContentType() : "png";
    return "data:image/"
             + contentType
             + ";base64,"
             + StringUtils.newStringUtf8(Base64.getEncoder().encode(bytes));
  }

  public Mono<String> getUserGoogleId() {
    return ReactiveSecurityContextHolder
             .getContext()
             .doOnError(error -> logger.error("error authorizing user context {}", error))
             .publishOn(Schedulers.boundedElastic())
             .map(SecurityContext::getAuthentication)
             .doOnError(error -> logger.error("error authorizing user auth {}", error))
             .map(Authentication::getName)
             .doOnError(error -> logger.error("error authorizing user name {}", error))
             .doOnNext(name -> logger.info("getting photo for googleId {}", name));
  }

  // TODO refactor to enum
  private String getContentType(String fileName) {
    if (fileName.toLowerCase().endsWith("png")) {
      return "png";
    } else if (fileName.toLowerCase().endsWith("jpg") || fileName.toLowerCase().endsWith("jpeg")) {
      return "jpg";
    } else if (fileName.toLowerCase().endsWith("mp4")) {
      return "mp4";
    } else if (fileName.toLowerCase().endsWith("mov")) {
      return "mov";
    } else if (fileName.toLowerCase().endsWith("wmv")) {
      return "wmv";
    } else if (fileName.toLowerCase().endsWith("avi")) {
      return "avi";
    } else if (fileName.toLowerCase().endsWith("3pg")) {
      return "3pg";
    } else if (fileName.toLowerCase().endsWith("mkv")) {
      return "mkv";
    } else if (fileName.toLowerCase().endsWith("gif")) {
      return "gif";
    }
    return "jpg";
  }

  public Mono<MediaItem> saveMediaItem(MediaItem mediaItem) {
    return mediaItemRepository.save(mediaItem)
             .doOnNext(m -> {
               if (mediaItem.getMediaItemMetaData() != null) {
                 logger.info("Saving metadata for mediaItem {}", mediaItem.getTitle());
                 mediaItem.getMediaItemMetaData().setMediaItemId(m.getId());
                 saveMediaItemMetaData(mediaItem.getMediaItemMetaData()).subscribe();
               }
             }).doOnError(error -> logger.error("error saving mediaItem {} {}", mediaItem.getTitle(), error.getMessage()))
             .doOnNext(x -> logger.info("successfully saved mediaItem {}", mediaItem.getTitle()));
  }

  private Mono<MediaItemMetaData> saveMediaItemMetaData(MediaItemMetaData mediaItemMetaData) {
    return mediaItemMetaDataRepository.save(mediaItemMetaData)
             .subscribeOn(Schedulers.elastic())
             .retryBackoff(20, Duration.ofMinutes(2))
             .doOnError(error -> logger.error("error saving metaData for mediaItem {} {}", mediaItemMetaData.getId(), error.getMessage()))
             .doOnNext(x -> logger.info("successfully saved metaData {}", x.getId()));
  }
}
