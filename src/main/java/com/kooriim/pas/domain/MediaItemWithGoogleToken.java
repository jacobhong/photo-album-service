package com.kooriim.pas.domain;

import com.google.photos.types.proto.MediaItem;

public class MediaItemWithGoogleToken {
  private com.google.photos.types.proto.MediaItem mediaItem;
  private GoogleTokenResponse googleTokenResponse;

  public MediaItemWithGoogleToken(MediaItem mediaItem, GoogleTokenResponse googleTokenResponse) {
    this.mediaItem = mediaItem;
    this.googleTokenResponse = googleTokenResponse;
  }

  public MediaItem getMediaItem() {
    return mediaItem;
  }

  public void setMediaItem(MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }

  public GoogleTokenResponse getGoogleTokenResponse() {
    return googleTokenResponse;
  }

  public void setGoogleTokenResponse(GoogleTokenResponse googleTokenResponse) {
    this.googleTokenResponse = googleTokenResponse;
  }
}
