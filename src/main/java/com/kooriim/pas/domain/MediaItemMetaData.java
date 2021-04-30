package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.photos.types.proto.MediaMetadata;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

@Entity
@Table(name = "media_item_meta_data")
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamicUpdate
@SelectBeforeUpdate
public class MediaItemMetaData {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private Integer mediaItemId;
  private String cameraMake;
  private float focalLength;
  @Column(name = "aperture_f_number")
  private float apertureFNumber;
  private String cameraModel;
  private int isoEquivalent;
  private long exposureTime;
  private long width;
  private long height;
  private double fps;

  public MediaItemMetaData (){}

  public MediaItemMetaData(MediaMetadata mediaMetadata) {
    if (mediaMetadata.getPhoto() != null) {
      cameraMake = mediaMetadata.getPhoto().getCameraMake();
      cameraModel = mediaMetadata.getPhoto().getCameraModel();
      focalLength = mediaMetadata.getPhoto().getFocalLength();
      apertureFNumber = mediaMetadata.getPhoto().getApertureFNumber();
      isoEquivalent = mediaMetadata.getPhoto().getIsoEquivalent();
      exposureTime = mediaMetadata.getPhoto().getExposureTime().getSeconds();
    }
    if (mediaMetadata.getVideo() != null) {
      fps = mediaMetadata.getVideo().getFps();
    }
    width = mediaMetadata.getWidth();
    height = mediaMetadata.getHeight();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getMediaItemId() {
    return mediaItemId;
  }

  public void setMediaItemId(Integer mediaItemId) {
    this.mediaItemId = mediaItemId;
  }

  public double getFps() {
    return fps;
  }

  public void setFps(double fps) {
    this.fps = fps;
  }

  public String getCameraMake() {
    return cameraMake;
  }

  public void setCameraMake(String cameraMake) {
    this.cameraMake = cameraMake;
  }

  public String getCameraModel() {
    return cameraModel;
  }

  public void setCameraModel(String cameraModel) {
    this.cameraModel = cameraModel;
  }

  public float getFocalLength() {
    return focalLength;
  }

  public void setFocalLength(float focalLength) {
    this.focalLength = focalLength;
  }

  public float getApertureFNumber() {
    return apertureFNumber;
  }

  public void setApertureFNumber(float apertureFNumber) {
    this.apertureFNumber = apertureFNumber;
  }

  public int getIsoEquivalent() {
    return isoEquivalent;
  }

  public void setIsoEquivalent(int isoEquivalent) {
    this.isoEquivalent = isoEquivalent;
  }

  public long getExposureTime() {
    return exposureTime;
  }

  public void setExposureTime(long exposureTime) {
    this.exposureTime = exposureTime;
  }

  public long getWidth() {
    return width;
  }

  public void setWidth(long width) {
    this.width = width;
  }

  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }
}
