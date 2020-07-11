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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
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
import java.util.stream.Collectors;

@Service
public class PhotoService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String S3_BUCKET = "https://kooriim-images.s3-us-west-1.amazonaws.com/";
  @Value("${image-directory}")
  private String imgDir;

  @Autowired
  private PhotoRepository photoRepository;

//  @Autowired
//  private AlbumRepository albumRepository;

  @Autowired()
  @Qualifier("awsS3Client")
  private S3Client awsS3Client;


  public Mono<Photo> getPhotoById(Integer photoId, Boolean setSrcImage) {
    logger.info("getting photo for googleId {}", ReactiveSecurityContextHolder
                                                   .getContext()
                                                   .map(SecurityContext::getAuthentication)
                                                   .map(Authentication::getName));
    return photoRepository.getPhotoById(photoId).map(photo -> {
      if (setSrcImage == true) {
        setBase64SrcPhoto(photo);
        logger.info("found photoId {}", photo.getId());
      }
      return photo;
    }).doOnNext(p -> logger.info("got p"));
  }

  public Mono<List<Photo>> getPhotosByQueryParams(MultiValueMap<String, String> params, Pageable pageable) {
    return ReactiveSecurityContextHolder
             .getContext()
             .publishOn(Schedulers.elastic())
             .map(SecurityContext::getAuthentication)
             .map(Authentication::getName)
             .doOnNext(name -> logger.info("getting photo for googleId {}", name))
             .flatMap(name -> {
               if (params.containsKey("albumId")) {
                 logger.info("Fetching photos by albumID");
                 return photoRepository
                          .getPhotosByAlbumId(Integer.valueOf(params.toSingleValueMap().get("albumId")), pageable)
                          .map(photos -> {
                            photos.forEach(photo -> setBase64Photo(params.toSingleValueMap(), photo));
                            return photos;
                          }).doOnNext(photos -> logger.info("fetched photos: {}", photos));
               } else {
                 logger.info("Fetching photos by googleID");
                 return photoRepository
                          .getPhotosByGoogleId(name, pageable)
                          .map(photos -> {
                            photos.forEach(photo -> setBase64Photo(params.toSingleValueMap(), photo));
                            return photos;
                          }).doOnNext(photos -> logger.info("fetched photos: {}", photos));
               }
             });

  }

  private Publisher<? extends Photo> setBase64Photo(Map<String, String> params, Photo photo) {
    if (params.containsKey("thumbnail") && params.get("thumbnail").equalsIgnoreCase("true")) {
      setBase64Thumbnail(photo);
    }
    if (params.containsKey("srcImage") && params.get("srcImage").equalsIgnoreCase("true")) {
      setBase64SrcPhoto(photo);
    }
    return Flux.just(photo);
  }

  public Mono<Photo> savePhoto(FilePart file) {
    return ReactiveSecurityContextHolder
             .getContext()
             .publishOn(Schedulers.elastic())
             .map(SecurityContext::getAuthentication)
             .map(Authentication::getName)
             .doOnNext(name -> logger.info("getting photo for googleId {}", name))
             .flatMap(name -> {
               final var contentType = file.filename().toLowerCase().endsWith((".png")) ? "png" : "jpg";
               final var fileName = file.filename().toLowerCase();
               final var thumbnailPath = fileName.substring(0, fileName.lastIndexOf(".") - 1) + ".thumbnail." + contentType;
               compressImageS3Push(file, contentType);
               return photoRepository.save(Photo.newInstance(file, fileName, thumbnailPath, contentType, name));
             });

  }

  public void deletePhotos(List<Integer> ids) {
    photoRepository.getByIds(ids)
      .doOnNext(photos -> photoRepository.deleteByIds(ids))
      .doOnNext(photos -> {
        var deletePhotos = photos
                             .stream()
                             .map(photo -> ObjectIdentifier.builder().key(photo.getFilePath()).build()).collect(Collectors.toList());
        var deleteThumbnails = photos
                                 .stream()
                                 .map(photo -> ObjectIdentifier.builder().key(photo.getThumbnailFilePath()).build()).collect(Collectors.toList());
        deletePhotos.addAll(deleteThumbnails);
        photos.forEach(photo -> awsS3Client.deleteObjects(DeleteObjectsRequest
                                                            .builder()
                                                            .delete(Delete.builder().objects(deletePhotos).build())
                                                            .build()));
      });
  }

  public void setBase64Thumbnail(Photo photo) {
    final byte[] bytes;
    try {
      logger.info("fetching image from s3 {}", photo.getThumbnailFilePath());
      bytes = awsS3Client.getObjectAsBytes(GetObjectRequest.builder().key(photo.getThumbnailFilePath().toLowerCase()).bucket("kooriim-images").build()).asByteArray();
      photo.setBase64SrcPhoto(generateBase64Image(photo, bytes));
    } catch (Exception e) {
      logger.error("Failed to set base64SrcPhoto with title: {}, error: {}", photo.getTitle(), e.getMessage());
    }
  }

  public void setBase64SrcPhoto(Photo photo) {
    final byte[] bytes;
    try {
      logger.info("fetching image from s3 {}", photo.getFilePath());
      bytes = awsS3Client.getObjectAsBytes(GetObjectRequest.builder().key(photo.getFilePath().toLowerCase()).bucket("kooriim-images").build()).asByteArray();
      photo.setBase64SrcPhoto(generateBase64Image(photo, bytes));
    } catch (Exception e) {
      logger.error("Failed to set base64SrcPhoto with title: {}, error: {}", photo.getTitle(), e.getMessage());
    }
  }

  private String generateBase64Image(Photo photo, byte[] bytes) {
    return "data:image/"
             + photo.getContentType()
             + ";base64,"
             + StringUtils.newStringUtf8(Base64.getEncoder().encode(bytes));
  }

  private void compressImageS3Push(FilePart file, String contentType) {
    try {
      var temp = File.createTempFile(file.filename(), contentType, new File(imgDir + "/"));
      file.transferTo(temp);
      var image = ImageIO.read(temp);
      final byte[] compressedImageResult = compressPhoto(contentType, image, 1920f, 1080f);
      final byte[] compressedThumbnailResult = compressPhoto(contentType, image, 360f, 270f);
      logger.info("pushing thumbnail to s3");
      final var thumbnailPath = file.filename().toLowerCase().substring(0, file.filename().lastIndexOf(".") - 1) + ".thumbnail." + contentType;
      awsS3Client.putObject(PutObjectRequest
                              .builder()
                              .bucket("kooriim-images")
                              .key(file.filename().toLowerCase())
                              .build(), RequestBody.fromBytes(compressedImageResult));

          logger.info("pushing photo to s3");
          awsS3Client.putObject(PutObjectRequest
                                  .builder()
                                  .bucket("kooriim-images")
                                  .key(thumbnailPath)
                                  .build(), RequestBody.fromBytes(compressedThumbnailResult));
      temp.delete();
    } catch (Exception e) {
    }
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

  public void patchPhotos(List<Photo> photos) {
    photos.forEach(photo -> photoRepository.save(photo));
  }
}
