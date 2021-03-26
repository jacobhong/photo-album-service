package com.kooriim.pas.routers;

import com.kooriim.pas.controller.PublicGalleryController;
import com.kooriim.pas.handler.PhotoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.http.MediaType.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;

@Configuration
public class PhotoRouter {
  public static final RequestPredicate ACCEPTS_JSON = accept(APPLICATION_JSON).or(accept(APPLICATION_STREAM_JSON));
  public static final RequestPredicate CONTENT_TYPE_JSON =
    contentType(APPLICATION_JSON).or(contentType(APPLICATION_STREAM_JSON));

  @Bean
  public RouterFunction<ServerResponse> route(PhotoHandler photoHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(GET("/photos/{id}").and(ACCEPTS_JSON), photoHandler::getPhotoById)
        .andRoute(GET("/videos/{title}").and(contentType(APPLICATION_OCTET_STREAM)).and(accept(APPLICATION_OCTET_STREAM)), photoHandler::getVideoByTitle)
        .andRoute(GET("/photos").and(ACCEPTS_JSON), photoHandler::getPhotos)
        .andRoute(PATCH("/photos").and(CONTENT_TYPE_JSON), photoHandler::patchPhotos)
        .andRoute(DELETE("/photos/{id}").and(ACCEPTS_JSON), photoHandler::deletePhoto)
        .andRoute(DELETE("/photos").and(CONTENT_TYPE_JSON), photoHandler::deletePhotos)
        .andRoute(POST("/photos").and(ACCEPTS_JSON).and(RequestPredicates.contentType(MediaType.MULTIPART_FORM_DATA)), photoHandler::create));

  }

  @Bean
  public RouterFunction<ServerResponse> route2(PublicGalleryController publicGalleryController) {
    return RouterFunctions.route(GET("/person/").and(ACCEPTS_JSON), publicGalleryController::getPhotos);
  }

}

