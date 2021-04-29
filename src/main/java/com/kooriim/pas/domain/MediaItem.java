package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.Metamodel;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.springframework.http.codec.multipart.FilePart;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "media_item")
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamicUpdate
@SelectBeforeUpdate
public class MediaItem implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Transient
  private MediaItemMetaData mediaItemMetaData;
  @Size(max = 255)
  private String title;
  @Size(max = 255)
  private String compressedImageFilePath;
  @Size(max = 255)
  private String originalImageFilePath;
  @Size(max = 255)
  private String thumbnailFilePath;
  @Size(max = 255)
  private String videoFilePath;
  @Size(max = 255)
  private String description;
  @Transient
  private String base64CompressedImage;
  @Transient
  private String base64OriginalImage;
  @Transient
  private String base64ThumbnailImage;
  private String googleId;
  private String contentType;
  private String mediaType;
  @Column(name = "is_public", nullable = false, columnDefinition = "BIT", length = 1)
  private Boolean isPublic;
  private Date created;
  private Date updated;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public MediaItemMetaData getMediaItemMetaData() {
    return mediaItemMetaData;
  }

  public void setMediaItemMetaData(MediaItemMetaData mediaItemMetaData) {
    this.mediaItemMetaData = mediaItemMetaData;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCompressedImageFilePath() {
    return compressedImageFilePath;
  }

  public void setCompressedImageFilePath(String compressedImageFilePath) {
    this.compressedImageFilePath = compressedImageFilePath;
  }

  public String getOriginalImageFilePath() {
    return originalImageFilePath;
  }

  public void setOriginalImageFilePath(String originalImageFilePath) {
    this.originalImageFilePath = originalImageFilePath;
  }

  public String getThumbnailFilePath() {
    return thumbnailFilePath;
  }

  public void setThumbnailFilePath(String thumbnailFilePath) {
    this.thumbnailFilePath = thumbnailFilePath;
  }

  public String getVideoFilePath() {
    return videoFilePath;
  }

  public void setVideoFilePath(String videoFilePath) {
    this.videoFilePath = videoFilePath;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBase64CompressedImage() {
    return base64CompressedImage;
  }

  public void setBase64CompressedImage(String base64CompressedImage) {
    this.base64CompressedImage = base64CompressedImage;
  }

  public String getBase64OriginalImage() {
    return base64OriginalImage;
  }

  public void setBase64OriginalImage(String base64OriginalImage) {
    this.base64OriginalImage = base64OriginalImage;
  }

  public String getBase64ThumbnailImage() {
    return base64ThumbnailImage;
  }

  public void setBase64ThumbnailImage(String base64ThumbnailImage) {
    this.base64ThumbnailImage = base64ThumbnailImage;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  public Boolean getPublic() {
    return isPublic;
  }

  public void setPublic(Boolean aPublic) {
    isPublic = aPublic;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public static MediaItem newInstancePhoto(String fileName, String compressedImageFilePath, String originalImageFilePath, String thumbnailFilePath, String contentType, String googleId, String mediaType, com.google.photos.types.proto.MediaItem mediaItem) {
    final var photo = new MediaItem();
    photo.setContentType(contentType);
    photo.setCompressedImageFilePath(compressedImageFilePath);
    photo.setOriginalImageFilePath(originalImageFilePath);
    photo.setThumbnailFilePath(thumbnailFilePath);
    photo.setTitle(fileName);
    photo.setGoogleId(googleId);
    photo.setMediaType(mediaType);
    if (mediaItem != null && mediaItem.getMediaMetadata() != null) {
      final var mediaItemMetaData = new MediaItemMetaData(mediaItem.getMediaMetadata());
      photo.setMediaItemMetaData(mediaItemMetaData);
    }
    return photo;
  }

  public static MediaItem newInstanceVideo(String fileName, String thumbnailFilePath, String videoFilePath, String contentType, String googleId, String mediaType, com.google.photos.types.proto.MediaItem mediaItem) {
    final var video = new MediaItem();
    video.setContentType(contentType);
    video.setThumbnailFilePath(thumbnailFilePath);
    video.setVideoFilePath(videoFilePath);
    video.setTitle(fileName);
    video.setGoogleId(googleId);
    video.setMediaType(mediaType);
    if (mediaItem != null && mediaItem.getMediaMetadata() != null) {
      final var mediaItemMetaData = new MediaItemMetaData(mediaItem.getMediaMetadata());
      video.setMediaItemMetaData(mediaItemMetaData);
    }
    return video;
  }


}
