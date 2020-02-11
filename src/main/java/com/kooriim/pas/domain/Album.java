package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "album")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Album implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Transient
  private Set<Integer> photoIds = new HashSet<>();
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

  public Set<Integer> getPhotoIds() {
    return photoIds;
  }

  public void setPhotoIds(Set<Integer> photoIds) {
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
    return id == album.id &&
             Objects.equals(photoIds, album.photoIds) &&
             Objects.equals(title, album.title) &&
             Objects.equals(description, album.description) &&
             Objects.equals(created, album.created) &&
             Objects.equals(updated, album.updated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, photoIds, title, description, created, updated);
  }
}
