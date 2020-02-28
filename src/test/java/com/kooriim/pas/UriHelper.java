package com.kooriim.pas;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class UriHelper {

  public static String uri(String path) {
    return UriComponentsBuilder
             .fromPath(path)
             .build()
             .toUriString();
  }

  public static String uriWithQueryParam(String path, String param, Object paramValue) {
    return UriComponentsBuilder
             .fromPath(path)
             .queryParam(param, paramValue)
             .build()
             .toUriString();
  }

  public static String uriWithPathVariable(String path, Integer pathId) {
    return UriComponentsBuilder
             .fromPath(path + pathId)
             .build()
             .toUriString();
  }

  public static String uriWithPathVariableAndQueryParam(String path, Integer pathId, String param, String paramValue) {
    return UriComponentsBuilder
             .fromPath(path + pathId)
             .queryParam(param, paramValue)
             .build()
             .toUriString();
  }

  public static HttpEntity httpEntity() {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(headers);
  }

  public static HttpEntity httpEntityWithBody(Object body) {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }


}
