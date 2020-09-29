package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.springframework.http.codec.multipart.FilePart;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "photo")
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamicUpdate
@SelectBeforeUpdate
public class Photo implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Size(max=50)
  private String title;
  @Column(name = "compressed_image_file_path")
  private String compressedImageFilePath;
  @Column(name = "original_image_file_path")
  private String originalImageFilePath;
  @Column(name = "thumbnail_file_path")
  private String thumbnailFilePath;
  @Size(max=50)
  private String description;
  @Transient
  private String base64CompressedImage;
  @Transient
  private String base64OriginalImage;
  @Transient
  private String base64ThumbnailImage;
  @Column(name = "google_id")
  private String googleId;
  private String contentType;
  @Column(name ="is_public", nullable = false, columnDefinition = "BIT", length = 1)
  private Boolean isPublic;
  private Date created;
  private Date updated;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public String getThumbnailFilePath() {
    return thumbnailFilePath;
  }

  public void setThumbnailFilePath(String thumbnailFilePath) {
    this.thumbnailFilePath = thumbnailFilePath;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public String getBase64CompressedImage() {
    return base64CompressedImage;
  }

  public void setBase64CompressedImage(String base64CompressedImage) {
    this.base64CompressedImage = base64CompressedImage;
  }

  public String getBase64ThumbnailImage() {
    return base64ThumbnailImage;
  }

  public void setBase64ThumbnailImage(String base64ThumbnailImage) {
    this.base64ThumbnailImage = base64ThumbnailImage;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public String getOriginalImageFilePath() {
    return originalImageFilePath;
  }

  public void setOriginalImageFilePath(String originalImageFilePath) {
    this.originalImageFilePath = originalImageFilePath;
  }

  public String getBase64OriginalImage() {
    return base64OriginalImage;
  }

  public void setBase64OriginalImage(String base64OriginalImage) {
    this.base64OriginalImage = base64OriginalImage;
  }
  public static Photo newInstance(FilePart file, String compressedImageFilePath, String originalImageFilePath, String thumbnailFilePath, String contentType, String googleId) {
    final var photo = new Photo();
    photo.setContentType(contentType);
    photo.setCompressedImageFilePath(compressedImageFilePath);
    photo.setOriginalImageFilePath(originalImageFilePath);
    photo.setThumbnailFilePath(thumbnailFilePath);
    photo.setTitle(file.filename());
    photo.setGoogleId(googleId);
    return photo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Photo photo = (Photo) o;
    return Objects.equals(id, photo.id) &&
             Objects.equals(title, photo.title) &&
             Objects.equals(compressedImageFilePath, photo.compressedImageFilePath) &&
             Objects.equals(originalImageFilePath, photo.originalImageFilePath) &&
             Objects.equals(thumbnailFilePath, photo.thumbnailFilePath) &&
             Objects.equals(description, photo.description) &&
             Objects.equals(base64CompressedImage, photo.base64CompressedImage) &&
             Objects.equals(base64ThumbnailImage, photo.base64ThumbnailImage) &&
             Objects.equals(googleId, photo.googleId) &&
             Objects.equals(contentType, photo.contentType) &&
             Objects.equals(isPublic, photo.isPublic) &&
             Objects.equals(created, photo.created) &&
             Objects.equals(updated, photo.updated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, compressedImageFilePath, originalImageFilePath, thumbnailFilePath, description, base64CompressedImage, base64ThumbnailImage, googleId, contentType, isPublic, created, updated);
  }

  @Override
  public String toString() {
    return "Photo{" +
             "id=" + id +
             ", title='" + title + '\'' +
             ", compressedImageFilePath='" + compressedImageFilePath + '\'' +
             ", originalImageFilePath='" + originalImageFilePath + '\'' +
             ", thumbnailFilePath='" + thumbnailFilePath + '\'' +
             ", description='" + description + '\'' +
             ", base64CompressedImage='" + base64CompressedImage + '\'' +
             ", base64ThumbnailImage='" + base64ThumbnailImage + '\'' +
             ", googleId='" + googleId + '\'' +
             ", contentType='" + contentType + '\'' +
             ", isPublic=" + isPublic +
             ", created=" + created +
             ", updated=" + updated +
             '}';
  }
}
