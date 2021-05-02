package com.kooriim.pas.domain.enums;

import java.util.Arrays;

public enum ContentType {
  PNG("png"),
  JPG("jpg"),
  JPEG("jpeg"),
  MP4("mp4"),
  MOV("mov"),
  WMV("wmv"),
  AVI("avi"),
  THREE_PG("3pg"),
  MKV("mkv"),
  GIF("gif");

  private String value;

  ContentType(String contentType) {
    this.value = contentType;
  }

  public String getValue() {
    return value;
  }

  public static ContentType fromString(String value) {
    if (value.lastIndexOf(".") > 0) {
      value = value.substring(value.lastIndexOf(".") + 1);
    }
    if (value.lastIndexOf("/") > 0 ) {
      value = value.split("/")[1];
    }
    String contentType = value;
    if (contentType.equalsIgnoreCase("jpeg")) return ContentType.JPG;
    return Arrays.stream(values())
             .filter(ct -> ct.value.equalsIgnoreCase(contentType))
             .findFirst()
             .orElseThrow(() -> new IllegalArgumentException("unknown contentType: " + contentType));
  }

  @Override
  public String toString() {
    return this.value;
  }
}
