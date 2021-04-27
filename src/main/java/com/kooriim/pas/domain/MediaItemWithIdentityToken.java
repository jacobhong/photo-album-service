package com.kooriim.pas.domain;

public class MediaItemWithIdentityToken {
  private com.google.photos.types.proto.MediaItem mediaItem;
  private GoogleTokenExchangeResponse googleTokenExchangeResponse;

  public MediaItemWithIdentityToken(com.google.photos.types.proto.MediaItem mediaItem, GoogleTokenExchangeResponse googleTokenExchangeResponse) {
    this.mediaItem = mediaItem;
    this.googleTokenExchangeResponse = googleTokenExchangeResponse;
  }
  public MediaItemWithIdentityToken(){}

  public com.google.photos.types.proto.MediaItem getMediaItem() {
    return mediaItem;
  }

  public void setMediaItem(com.google.photos.types.proto.MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }

  public GoogleTokenExchangeResponse getGoogleTokenExchangeResponse() {
    return googleTokenExchangeResponse;
  }

  public void setGoogleTokenExchangeResponse(GoogleTokenExchangeResponse googleTokenExchangeResponse) {
    this.googleTokenExchangeResponse = googleTokenExchangeResponse;
  }
}
