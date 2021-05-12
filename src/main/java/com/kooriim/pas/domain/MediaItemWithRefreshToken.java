package com.kooriim.pas.domain;

import com.google.photos.types.proto.MediaItem;


public class MediaItemWithRefreshToken {
  private com.google.photos.types.proto.MediaItem mediaItem;
  private String refreshToken;

  public MediaItemWithRefreshToken(){}
  public MediaItemWithRefreshToken(MediaItem mediaItem, String refreshToken) {
    this.mediaItem = mediaItem;
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
