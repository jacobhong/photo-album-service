package com.kooriim.pas.service;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.repository.PhotoRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.StringUtils;
import org.reactivestreams.Publisher;
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
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhotoService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String S3_BUCKET_BASE_URL = "https://kooriim-images.s3-us-west-1.amazonaws.com/";
  @Value("${s3.bucketName}")
  private String S3_BUCKET_NAME;

  @Value("${image-directory}")
  private String imgDir;

  @Autowired
  private PhotoRepository photoRepository;

//  @Autowired
//  private AlbumRepository albumRepository;

  @Autowired
  @Qualifier("awsS3Client")
  private S3Client awsS3Client;


  public Mono<Photo> getPhotoById(Integer photoId, Boolean setSrcImage) {
    return photoRepository.getPhotoById(photoId)
             .flatMap(photo -> {
               if (setSrcImage == true) {
                 return setBase64SrcPhoto(photo);
               }
               return Mono.just(photo);
             });
  }

  public Flux<Photo> getPhotosByQueryParams(MultiValueMap<String, String> params, Pageable pageable) {
    return getUserGoogleId()
             .flatMapMany(name -> getPhotosSetBase64(params, pageable, name));
  }

  public Mono<Photo> savePhoto(FilePart file) {
    return getUserGoogleId()
             .flatMap(compressAndSaveImage(file));
  }

  public Mono<Void> deletePhotos(List<Integer> ids) {
    return photoRepository.getPhotosByIds(ids)
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
               deletePhotos.addAll(deleteThumbnails);
               awsS3Client.deleteObjects(DeleteObjectsRequest
                                           .builder()
                                           .delete(Delete.builder().objects(deletePhotos).build())
                                           .bucket(S3_BUCKET_NAME)
                                           .build());

               return photoRepository.deleteByPhotoIds(ids).then();
             }).doOnNext(result -> logger.info("Deleted photos from s3"))
             .doOnError(error -> logger.error("Error deleting photos {}, {}", error.getMessage(), ids))
             .then();
  }

  public Mono<Photo> setBase64Thumbnail(Photo photo) {
    return Mono.fromCallable(() -> {
      final byte[] bytes;
      var s3Object = awsS3Client.getObject(GetObjectRequest
                                             .builder()
                                             .key("thumbnail." + photo
                                                                   .getTitle())
                                             .bucket(S3_BUCKET_NAME)
                                             .build());
      bytes = s3Object.readAllBytes();
      s3Object.close();
      photo.setBase64ThumbnailPhoto(generateBase64Image(photo, bytes));
      return photo;
    }).doOnNext(result -> logger.info("fetched image from s3 {}", result.getThumbnailFilePath()))
             .doOnError(error -> logger.error("Error setting base64 thumbnail {} for photoId {}", error.getMessage(), photo.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<Photo> setBase64SrcPhoto(Photo photo) {
    return Mono.fromCallable(() -> {
      final byte[] bytes;
      var s3Object = awsS3Client.getObject(GetObjectRequest
                                             .builder()
                                             .key(photo
                                                    .getTitle())
                                             .bucket(S3_BUCKET_NAME)
                                             .build());
      bytes = s3Object.readAllBytes();
      s3Object.close();
      photo.setBase64SrcPhoto(generateBase64Image(photo, bytes));
      return photo;
    }).doOnNext(result -> logger.info("fetched image from s3 {}", result.getFilePath()))
             .doOnError(error -> logger.error("Error setting setBase64SrcPhoto {} for photoId {}", error.getMessage(), photo.getId()))
             .onErrorResume(p -> Mono.empty())
             .subscribeOn(Schedulers.elastic());
  }

  public Mono<Void> patchPhotos(List<Photo> photos) {
    return Flux.fromIterable(photos)
             .flatMap(p -> photoRepository.save(p))
             .then();
  }

  private Publisher<? extends Photo> getPhotosSetBase64(MultiValueMap<String, String> params, Pageable pageable, String name) {
    if (params.containsKey("albumId")) {
      return photoRepository
               .getPhotosByAlbumId(Integer.valueOf(params.toSingleValueMap().get("albumId")), pageable)
               .flatMap(photo -> setBase64Photo(params.toSingleValueMap(), photo))
               .doOnNext(photos -> logger.info("getPhotosSetBase64 by albumId: {}", photos.getTitle()));
    } else {
      return photoRepository
               .getPhotosByGoogleId(name, pageable)
               .flatMap(photo -> setBase64Photo(params.toSingleValueMap(), photo))
               .doOnNext(photos -> logger.info("getPhotosSetBase64 by googleId: {}", photos.getTitle()));
    }
  }

  private Mono<Photo> setBase64Photo(Map<String, String> params, Photo photo) {
    if (params.containsKey("thumbnail") && params.get("thumbnail").equalsIgnoreCase("true")) {
      return setBase64Thumbnail(photo);
    }
    if (params.containsKey("srcImage") && params.get("srcImage").equalsIgnoreCase("true")) {
      return setBase64SrcPhoto(photo);
    }
    return Mono.just(photo);
  }

  private Function<String, Mono<? extends Photo>> compressAndSaveImage(FilePart file) {
    return name -> {
      final var contentType = file.filename()
                                .endsWith((".png")) ? "png" : "jpg";
      final var fileName = file.filename();
      final var thumbnailPath = fileName.substring(0, fileName.lastIndexOf(".")) + ".thumbnail." + contentType;
      return compressImageS3Push(file, contentType)
               .flatMap(image -> photoRepository
                                   .save(Photo.newInstance(file, S3_BUCKET_BASE_URL + fileName, S3_BUCKET_BASE_URL + thumbnailPath, contentType, name)));
    };
  }

  private byte[] compressPhoto(String contentType, BufferedImage image, float aspectWidth, float aspectHeight) throws IOException {
    final var widthRatio = aspectWidth / image.getWidth();
    final var heightRatio = aspectHeight / image.getHeight();
    final var ratio = Math.min(widthRatio, heightRatio);
    final var width = image.getWidth() * ratio;
    final var height = image.getHeight() * ratio;
    image = Thumbnails.of(image)
              .size(Math.round(width), Math.round(height))
              .asBufferedImage();
    final var byteOutputStream = new ByteArrayOutputStream();
    ImageIO.setUseCache(false);
    final var writers = ImageIO.getImageWritersByFormatName(contentType);
    final var writer = writers.next();
    final var ios = ImageIO.createImageOutputStream(byteOutputStream);
    writer.setOutput(ios);
    final var param = writer.getDefaultWriteParam();

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.6f);  // Change the quality value you prefer
    writer.write(null, new IIOImage(image, null, null), param);
    final var compressedImageResult = byteOutputStream.toByteArray();
    writer.dispose();
    byteOutputStream.close();
    ios.close();
    writer.dispose();
    return compressedImageResult;
  }

  private String generateBase64Image(Photo photo, byte[] bytes) {
    return "data:image/"
             + photo.getContentType()
             + ";base64,"
             + StringUtils.newStringUtf8(Base64.getEncoder().encode(bytes));
  }

  private Mono<byte[]> compressImageS3Push(FilePart file, String contentType) {
    return Mono.fromCallable(() -> {
      final var tmpDir = new File(imgDir);
      if (!tmpDir.exists()) {
        tmpDir.mkdir();
      }
      var tempFile = File.createTempFile(file.filename(), contentType, new File(imgDir + "/"));
      file.transferTo(tempFile);
      var image = ImageIO.read(tempFile);
      final byte[] compressedImageResult = compressPhoto(contentType, image, 1920f, 1080f);
      final byte[] compressedThumbnailResult = compressPhoto(contentType, image, 360f, 270f);
      logger.info("pushing thumbnail to s3");
      final var thumbnailPath = file.filename().substring(0, file.filename().lastIndexOf(".")) + ".thumbnail." + contentType;
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key(file.filename())
                              .build(), RequestBody.fromBytes(compressedImageResult));

      logger.info("pushing photo to s3");
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket(S3_BUCKET_NAME)
                              .key("thumbnail." + file.filename())
                              .build(), RequestBody.fromBytes(compressedThumbnailResult));
      tempFile.delete();
      return compressedImageResult;
    }).doOnSuccess(result -> logger.info("Compressed and pushed image to s3"))
             .doOnError(error -> logger.error("Failed compressImageS3Push {}", error.getMessage()))
             .subscribeOn(Schedulers.elastic());
  }

  private Mono<String> getUserGoogleId() {
    return ReactiveSecurityContextHolder
             .getContext()
             .doOnError(error -> logger.error("error authorizing user context {}", error))
             .publishOn(Schedulers.elastic())
             .map(SecurityContext::getAuthentication)
             .doOnError(error -> logger.error("error authorizing user auth {}", error))
             .map(Authentication::getName)
             .doOnError(error -> logger.error("error authorizing user name {}", error))
             .doOnNext(name -> logger.info("getting photo for googleId {}", name));
  }
}
