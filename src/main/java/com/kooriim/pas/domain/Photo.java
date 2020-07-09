package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
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

  private String title;
  @Column(name = "file_path")
  private String filePath;
  @Column(name = "thumbnail_file_path")
  private String thumbnailFilePath;
  private String description;
  @Transient
  private String base64SrcPhoto;
  @Transient
  private String base64ThumbnailPhoto;
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

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
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

  public String getBase64SrcPhoto() {
    return base64SrcPhoto;
  }

  public void setBase64SrcPhoto(String base64SrcPhoto) {
    this.base64SrcPhoto = base64SrcPhoto;
  }

  public String getBase64ThumbnailPhoto() {
    return base64ThumbnailPhoto;
  }

  public void setBase64ThumbnailPhoto(String base64ThumbnailPhoto) {
    this.base64ThumbnailPhoto = base64ThumbnailPhoto;
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

  public static Photo newInstance(FilePart file, String filePath, String thumbnailFilePath, String contentType, String googleId) {
    final var photo = new Photo();
    photo.setContentType(contentType);
    photo.setFilePath(filePath);
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
    return id == photo.id &&
             Objects.equals(title, photo.title) &&
             Objects.equals(filePath, photo.filePath) &&
             Objects.equals(thumbnailFilePath, photo.thumbnailFilePath) &&
             Objects.equals(description, photo.description) &&
             Objects.equals(base64SrcPhoto, photo.base64SrcPhoto) &&
             Objects.equals(base64ThumbnailPhoto, photo.base64ThumbnailPhoto) &&
             Objects.equals(contentType, photo.contentType) &&
             Objects.equals(created, photo.created) &&
             Objects.equals(updated, photo.updated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, filePath, thumbnailFilePath, description, base64SrcPhoto, base64ThumbnailPhoto, contentType, created, updated);
  }

  @Override
  public String toString() {
    return "Photo{" +
             "id=" + id +
             ", title='" + title + '\'' +
             ", filePath='" + filePath + '\'' +
             ", thumbnailFilePath='" + thumbnailFilePath + '\'' +
             ", description='" + description + '\'' +
             ", base64SrcPhoto='" + base64SrcPhoto + '\'' +
             ", base64ThumbnailPhoto='" + base64ThumbnailPhoto + '\'' +
             ", googleId='" + googleId + '\'' +
             ", contentType='" + contentType + '\'' +
             ", created=" + created +
             ", updated=" + updated +
             '}';
  }
}
