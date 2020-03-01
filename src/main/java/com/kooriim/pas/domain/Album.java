package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "album")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Album implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Transient
  private List<Photo> previewPhotos = new ArrayList<>(2);
  @Transient
  private List<Integer> photoIds = new ArrayList<>();
  private String title;
  private String description;
  @Column(name = "google_id")
  private String googleId;
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

  public List<Photo> getPreviewPhotos() {
    return previewPhotos;
  }

  public void setPreviewPhotos(List<Photo> previewPhotos) {
    this.previewPhotos = previewPhotos;
  }

  public List<Integer> getPhotoIds() {
    return photoIds;
  }

  public void setPhotoIds(List<Integer> photoIds) {
    this.photoIds = photoIds;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Album album = (Album) o;
    return Objects.equals(id, album.id) &&
             Objects.equals(title, album.title) &&
             Objects.equals(description, album.description) &&
             Objects.equals(googleId, album.googleId) &&
             Objects.equals(created, album.created) &&
             Objects.equals(updated, album.updated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, description, googleId, created, updated);
  }

  @Override
  public String toString() {
    return "Album{" +
             "id=" + id +
             ", previewPhotos=" + previewPhotos +
             ", title='" + title + '\'' +
             ", description='" + description + '\'' +
             ", googleId='" + googleId + '\'' +
             ", created=" + created +
             ", updated=" + updated +
             '}';
  }
}
