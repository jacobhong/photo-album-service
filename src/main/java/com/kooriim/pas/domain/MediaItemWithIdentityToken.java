package com.kooriim.pas.domain;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.MediaItem;

public class MediaItemWithIdentityToken {
  private com.google.photos.types.proto.MediaItem mediaItem;
  private GoogleTokenExchangeResponse googleTokenExchangeResponse;
  private PhotosLibraryClient photosLibraryClient;

  public MediaItemWithIdentityToken(MediaItem mediaItem, GoogleTokenExchangeResponse googleTokenExchangeResponse, PhotosLibraryClient photosLibraryClient) {
    this.mediaItem = mediaItem;
    this.googleTokenExchangeResponse = googleTokenExchangeResponse;
    this.photosLibraryClient = photosLibraryClient;
  }

  public MediaItemWithIdentityToken(){}

  public MediaItem getMediaItem() {
    return mediaItem;
  }

  public void setMediaItem(MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }

  public GoogleTokenExchangeResponse getGoogleTokenExchangeResponse() {
    return googleTokenExchangeResponse;
  }

  public void setGoogleTokenExchangeResponse(GoogleTokenExchangeResponse googleTokenExchangeResponse) {
    this.googleTokenExchangeResponse = googleTokenExchangeResponse;
  }

  public PhotosLibraryClient getPhotosLibraryClient() {
    return photosLibraryClient;
  }

  public void setPhotosLibraryClient(PhotosLibraryClient photosLibraryClient) {
    this.photosLibraryClient = photosLibraryClient;
  }
}
