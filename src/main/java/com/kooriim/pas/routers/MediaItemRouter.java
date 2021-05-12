package com.kooriim.pas.routers;

import com.kooriim.pas.controller.PublicGalleryController;
import com.kooriim.pas.handler.MediaItemHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.http.MediaType.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;

@Configuration
public class MediaItemRouter {
  public static final RequestPredicate ACCEPTS_JSON = accept(APPLICATION_JSON).or(accept(APPLICATION_STREAM_JSON));
  public static final RequestPredicate CONTENT_TYPE_JSON =
    contentType(APPLICATION_JSON).or(contentType(APPLICATION_STREAM_JSON));

  @Bean
  public RouterFunction<ServerResponse> route(MediaItemHandler mediaItemHandlerHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(GET("/photos/{id}").and(ACCEPTS_JSON), mediaItemHandlerHandler::getMediaItemById)
        .andRoute(GET("/videos/{title}").and(contentType(APPLICATION_OCTET_STREAM)).and(accept(APPLICATION_OCTET_STREAM)), mediaItemHandlerHandler::getVideoByTitle)
        .andRoute(GET("/photos").and(ACCEPTS_JSON), mediaItemHandlerHandler::getMediaItems)
        .andRoute(PATCH("/photos").and(CONTENT_TYPE_JSON), mediaItemHandlerHandler::patchMediaItems)
        .andRoute(POST("/photos/{id}/metadata").and(ACCEPTS_JSON).and(CONTENT_TYPE_JSON), mediaItemHandlerHandler::createMetaData)
        .andRoute(DELETE("/photos/{id}").and(ACCEPTS_JSON), mediaItemHandlerHandler::deleteMediaItem)
        .andRoute(DELETE("/photos").and(CONTENT_TYPE_JSON), mediaItemHandlerHandler::deleteMediaItems)
        .andRoute(POST("/photos").and(ACCEPTS_JSON).and(RequestPredicates.contentType(MediaType.MULTIPART_FORM_DATA)), mediaItemHandlerHandler::create));

  }

  @Bean
  public RouterFunction<ServerResponse> route2(PublicGalleryController publicGalleryController) {
    return RouterFunctions.route(GET("/person/").and(ACCEPTS_JSON), publicGalleryController::getPhotos);
  }

}

