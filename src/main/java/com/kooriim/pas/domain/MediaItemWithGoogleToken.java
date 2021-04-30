package com.kooriim.pas.domain;

import com.google.photos.types.proto.MediaItem;

public class MediaItemWithGoogleToken {
  private com.google.photos.types.proto.MediaItem mediaItem;
  private String refreshToken;

  public MediaItemWithGoogleToken(){}
  public MediaItemWithGoogleToken(MediaItem mediaItem, String refreshToken) {
    this.mediaItem = mediaItem;
    this.refreshToken = refreshToken;
  }

  public MediaItem getMediaItem() {
    return mediaItem;
  }

  public void setMediaItem(MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
