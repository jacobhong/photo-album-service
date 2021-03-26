package com.kooriim.pas.routers;

import com.kooriim.pas.controller.PublicGalleryController;
import com.kooriim.pas.handler.GoogleHandler;
import com.kooriim.pas.handler.PhotoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;

@Configuration
public class GoogleRouter {
  public static final RequestPredicate ACCEPTS_JSON = accept(APPLICATION_JSON).or(accept(APPLICATION_STREAM_JSON));
  public static final RequestPredicate CONTENT_TYPE_JSON =
    contentType(APPLICATION_JSON).or(contentType(APPLICATION_STREAM_JSON));

  @Bean
  public RouterFunction<ServerResponse> route4(GoogleHandler googleHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(POST("/google-sync").and(ACCEPTS_JSON), googleHandler::syncGooglePhotos));

  }
}
