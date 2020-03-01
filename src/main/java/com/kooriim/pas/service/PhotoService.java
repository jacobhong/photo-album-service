package com.kooriim.pas.service;

import com.kooriim.pas.repository.AlbumRepository;
import com.kooriim.pas.repository.PhotoRepository;
import com.kooriim.pas.domain.Photo;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PhotoService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${image-directory}")
  private String imgDir;

  @Autowired
  private PhotoRepository photoRepository;

  @Autowired
  private AlbumRepository albumRepository;


  public Optional<Photo> getPhotoById(Integer photoId, Boolean setSrcImage) {
    final var photo = photoRepository.findById(photoId);
    if (setSrcImage == true) {
      photo.ifPresent(p -> setBase64SrcPhoto(p));
      logger.info("found photoId {}", photo.get().getId());
    }
    return photo;
  }

  public List<Photo> getPhotosByQueryParams(Map<String, String> params) {
    logger.info("getting photo for googleId {}", SecurityContextHolder
                                                   .getContext()
                                                   .getAuthentication()
                                                   .getName());
    final var response = new ArrayList<Photo>();
    final var photos = new ArrayList<Photo>();

    if (params.containsKey("albumId")) {
      photoRepository
        .getPhotosByAlbumId(Integer.valueOf(params.get("albumId")))
        .ifPresent(photo -> photos.addAll(photo));
    } else {
      photos.addAll(photoRepository.getPhotosByGoogleId(SecurityContextHolder
                                                          .getContext()
                                                          .getAuthentication()
                                                          .getName()));
    }

    photos.forEach(p -> {
      if (params.containsKey("thumbnail") && params.get("thumbnail").equalsIgnoreCase("true")) {
        setBase64Thumbnail(p);
      }
      if (params.containsKey("srcImage") && params.get("srcImage").equalsIgnoreCase("true")) {
        setBase64SrcPhoto(p);
      }
      response.add(p);
    });

    return response;
  }

  @Transactional
  public Photo savePhoto(MultipartFile file) throws IOException {
    logger.info("saving photo for googleId {}", SecurityContextHolder
                                                  .getContext()
                                                  .getAuthentication()
                                                  .getName());
    final var fileName = file
                           .getOriginalFilename()
                           .substring(0, file.getOriginalFilename().lastIndexOf(".")) + ".jpg";
    final var filePath = imgDir + "/" + fileName;
    final var thumbnailFilePath = imgDir + "/" + "thumbnail." + fileName;
    final var contentType = "jpg";
    final var compressedImage = compressAndSavePhoto(file, filePath);
    saveThumbnail(compressedImage);
    return photoRepository.save(Photo.newInstance(file, filePath, thumbnailFilePath, contentType, SecurityContextHolder
                                                                                                    .getContext()
                                                                                                    .getAuthentication()
                                                                                                    .getName()));
  }

  @Transactional
  public void deletePhotos(List<Integer> ids) {
    try {
      final var photos = photoRepository.findAllById(ids);
      photoRepository.deletePhotosFromAlbum(ids);
      this.photoRepository.deleteAll(photos);
      for (Photo photo : photos) {
        Files.delete(Path.of(photo.getFilePath()));
        Files.delete(Path.of(photo.getThumbnailFilePath()));
      }
    } catch (IOException e) {
      logger.error("Failed deleting photo ids: [{}], stackTrace: {}", ids, e.getMessage());
    }
  }

  public void setBase64Thumbnail(Photo photo) {
    final byte[] bytes;
    try {
      bytes = Files.readAllBytes(Paths.get(photo.getThumbnailFilePath()));
      photo.setBase64ThumbnailPhoto(generateBase64Image(photo, bytes));
    } catch (IOException e) {
      logger.error("Failed to set base64Thumbnail with title: {}", photo.getTitle(), e.getMessage());
    }
  }

  public void setBase64SrcPhoto(Photo photo) {
    final byte[] bytes;
    try {
      bytes = Files.readAllBytes(Paths.get(photo.getFilePath()));
      photo.setBase64SrcPhoto(generateBase64Image(photo, bytes));
    } catch (IOException e) {
      logger.error("Failed to set base64SrcPhoto with title: {}", photo.getTitle(), e.getMessage());
    }
  }

  private String generateBase64Image(Photo photo, byte[] bytes) {
    return "data:image/"
             + photo.getContentType()
             + ";base64,"
             + StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false));
  }

  private File compressAndSavePhoto(MultipartFile file, String filePath) {
    logger.info("Saving photo {} to imgDir {}", file.getOriginalFilename(), filePath);
    try {
      final var image = ImageIO.read(file.getInputStream());
      final var compressedImageFile = new File(filePath);
      final var os = new FileOutputStream(compressedImageFile);
      final var writers = ImageIO.getImageWritersByFormatName("jpg");
      final var writer = writers.next();

      final var ios = ImageIO.createImageOutputStream(os);
      writer.setOutput(ios);

      final var param = writer.getDefaultWriteParam();

      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(0.75f);  // Change the quality value you prefer
      writer.write(null, new IIOImage(image, null, null), param);

      os.close();
      ios.close();
      writer.dispose();
      return compressedImageFile;
    } catch (Exception e) {
      logger.error("Failed to compressAndSavePhoto file {} with error: ", file.getOriginalFilename(), e);
      throw new RuntimeException(e);
    }
  }

  private void saveThumbnail(File file) throws IOException {
    Thumbnails.of(file)
      .size(360, 270)
      .outputFormat("jpg")
      .toFiles(new File("/opt/images"), Rename.PREFIX_DOT_THUMBNAIL);
  }
}
