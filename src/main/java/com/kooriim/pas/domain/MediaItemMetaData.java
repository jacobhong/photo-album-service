package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.photos.types.proto.MediaMetadata;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

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
  private float exposureTime;
  private long width;
  private long height;
  private double fps;

  private String contrast;
  private float digitalZoomRatio;
  private float exposureCompensation;
  private String exposureMode;
  private String exposureProgram;
  private String lensModel;
  private String meteringMode;
  private String saturation;
  private String sceneCaptureType;
  private String sharpness;
  private String whiteBalance;
  private LocalDate createdDate;

  public static MediaItemMetaData fromGoogleMetaData(MediaMetadata mediaMetadata) {
    var mediaItemMetaData = new MediaItemMetaData();
    if (mediaMetadata.getPhoto() != null) {
      mediaItemMetaData.setCameraMake(mediaMetadata.getPhoto().getCameraMake());
      mediaItemMetaData.setCameraModel(mediaMetadata.getPhoto().getCameraModel());
      mediaItemMetaData.setFocalLength(mediaMetadata.getPhoto().getFocalLength());
      mediaItemMetaData.setApertureFNumber(mediaMetadata.getPhoto().getApertureFNumber());
      mediaItemMetaData.setIsoEquivalent(mediaMetadata.getPhoto().getIsoEquivalent());
      mediaItemMetaData.setExposureTime(mediaMetadata.getPhoto().getExposureTime().getSeconds());
    }
    if (mediaMetadata.getVideo() != null) {
      mediaItemMetaData.setFps(mediaMetadata.getVideo().getFps());
    }

    mediaItemMetaData.setCreatedDate(LocalDate
                                       .ofInstant(Instant
                                                    .ofEpochSecond(mediaMetadata.getCreationTime().getSeconds()),
                                         ZoneId.of("UTC")));
    mediaItemMetaData.setWidth(mediaMetadata.getWidth());
    mediaItemMetaData.setHeight(mediaMetadata.getHeight());
    return mediaItemMetaData;
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

  public float getExposureTime() {
    return exposureTime;
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

  public void setExposureTime(float exposureTime) {
    this.exposureTime = exposureTime;
  }

  public String getContrast() {
    return contrast;
  }

  public void setContrast(String contrast) {
    this.contrast = contrast;
  }

  public float getDigitalZoomRatio() {
    return digitalZoomRatio;
  }

  public void setDigitalZoomRatio(float digitalZoomRatio) {
    this.digitalZoomRatio = digitalZoomRatio;
  }

  public float getExposureCompensation() {
    return exposureCompensation;
  }

  public void setExposureCompensation(float exposureCompensation) {
    this.exposureCompensation = exposureCompensation;
  }

  public String getExposureMode() {
    return exposureMode;
  }

  public void setExposureMode(String exposureMode) {
    this.exposureMode = exposureMode;
  }

  public String getExposureProgram() {
    return exposureProgram;
  }

  public void setExposureProgram(String exposureProgram) {
    this.exposureProgram = exposureProgram;
  }

  public String getLensModel() {
    return lensModel;
  }

  public void setLensModel(String lensModel) {
    this.lensModel = lensModel;
  }

  public String getMeteringMode() {
    return meteringMode;
  }

  public void setMeteringMode(String meteringMode) {
    this.meteringMode = meteringMode;
  }

  public String getSaturation() {
    return saturation;
  }

  public void setSaturation(String saturation) {
    this.saturation = saturation;
  }

  public String getSceneCaptureType() {
    return sceneCaptureType;
  }

  public void setSceneCaptureType(String sceneCaptureType) {
    this.sceneCaptureType = sceneCaptureType;
  }

  public String getSharpness() {
    return sharpness;
  }

  public void setSharpness(String sharpness) {
    this.sharpness = sharpness;
  }

  public String getWhiteBalance() {
    return whiteBalance;
  }

  public void setWhiteBalance(String whiteBalance) {
    this.whiteBalance = whiteBalance;
  }

  public LocalDate getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDate createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaItemMetaData that = (MediaItemMetaData) o;
    return Float.compare(that.focalLength, focalLength) == 0 &&
             Float.compare(that.apertureFNumber, apertureFNumber) == 0 &&
             isoEquivalent == that.isoEquivalent &&
             Float.compare(that.exposureTime, exposureTime) == 0 &&
             width == that.width &&
             height == that.height &&
             Double.compare(that.fps, fps) == 0 &&
             Float.compare(that.digitalZoomRatio, digitalZoomRatio) == 0 &&
             Float.compare(that.exposureCompensation, exposureCompensation) == 0 &&
             Objects.equals(id, that.id) &&
             Objects.equals(mediaItemId, that.mediaItemId) &&
             Objects.equals(cameraMake, that.cameraMake) &&
             Objects.equals(cameraModel, that.cameraModel) &&
             Objects.equals(contrast, that.contrast) &&
             Objects.equals(exposureMode, that.exposureMode) &&
             Objects.equals(exposureProgram, that.exposureProgram) &&
             Objects.equals(lensModel, that.lensModel) &&
             Objects.equals(meteringMode, that.meteringMode) &&
             Objects.equals(saturation, that.saturation) &&
             Objects.equals(sceneCaptureType, that.sceneCaptureType) &&
             Objects.equals(sharpness, that.sharpness) &&
             Objects.equals(whiteBalance, that.whiteBalance) &&
             Objects.equals(createdDate, that.createdDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, mediaItemId, cameraMake, focalLength, apertureFNumber, cameraModel, isoEquivalent, exposureTime, width, height, fps, contrast, digitalZoomRatio, exposureCompensation, exposureMode, exposureProgram, lensModel, meteringMode, saturation, sceneCaptureType, sharpness, whiteBalance, createdDate);
  }
}
