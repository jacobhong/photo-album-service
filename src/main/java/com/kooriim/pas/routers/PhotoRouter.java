package com.kooriim.pas.routers;

import com.kooriim.pas.controller.PublicGalleryController;
import com.kooriim.pas.handler.AlbumHandler;
import com.kooriim.pas.handler.PhotoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class PhotoRouter {
  private static final RequestPredicate ACCEPTS_JSON = accept(APPLICATION_JSON).or(accept(APPLICATION_STREAM_JSON));
  private static final RequestPredicate CONTENT_TYPE_JSON =
    contentType(APPLICATION_JSON).or(contentType(APPLICATION_STREAM_JSON));

  @Bean
  public RouterFunction<ServerResponse> route(PhotoHandler photoHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(GET("/photos/{id}").and(ACCEPTS_JSON), photoHandler::getPhotoById)
        .andRoute(GET("/photos").and(ACCEPTS_JSON), photoHandler::getPhotos)
        .andRoute(PATCH("/photos").and(CONTENT_TYPE_JSON), photoHandler::patchPhotos)
        .andRoute(DELETE("/photos/{id}").and(ACCEPTS_JSON), photoHandler::deletePhoto)
        .andRoute(DELETE("/photos").and(CONTENT_TYPE_JSON), photoHandler::deletePhotos)
        .andRoute(POST("/photos").and(ACCEPTS_JSON).and(RequestPredicates.contentType(MediaType.MULTIPART_FORM_DATA)), photoHandler::create));

//          .andRoute(POST("/orders").and(CONTENT_TYPE_JSON), orderHandler::createOrderHandler)
//          .andRoute(PATCH("/orders/{id}").and(CONTENT_TYPE_JSON), orderHandler::patchOrder)
//          .andRoute(PATCH("/orders").and(CONTENT_TYPE_JSON), orderHandler::patchOrders)
//          .andRoute(DELETE("/orders/{id}"), orderHandler::deleteOrder)).filter(this::checkCommonRequiredHeaders);
  }

  @Bean
  public RouterFunction<ServerResponse> route2(PublicGalleryController publicGalleryController) {
    return RouterFunctions.route(GET("/person/").and(ACCEPTS_JSON), publicGalleryController::getPhotos);
//          .andRoute(POST("/orders").and(CONTENT_TYPE_JSON), orderHandler::createOrderHandler)
//          .andRoute(PATCH("/orders/{id}").and(CONTENT_TYPE_JSON), orderHandler::patchOrder)
//          .andRoute(PATCH("/orders").and(CONTENT_TYPE_JSON), orderHandler::patchOrders)
//          .andRoute(DELETE("/orders/{id}"), orderHandler::deleteOrder)).filter(this::checkCommonRequiredHeaders);
  }

  @Bean
  public RouterFunction<ServerResponse> route3(AlbumHandler albumHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(GET("/albums").and(ACCEPTS_JSON), albumHandler::getAlbums)
        .andRoute(POST("/albums").and(CONTENT_TYPE_JSON), albumHandler::create)
        .andRoute(PATCH("/albums/{id}").and(CONTENT_TYPE_JSON), albumHandler::addPhotosToAlbum)
          .andRoute(DELETE("/albums/{id}").and(ACCEPTS_JSON), albumHandler::delete));
  }

//  @Bean
//  public RouterFunction<ServerResponse> routes(PersonHandler personHandler, LocationHandler locationHandler) {
//    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
//             .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
//             .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
//             .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
//             .andRoute(DELETE("/people/{id}"), personHandler::delete)
//             .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry)
//             .andRoute(GET("/locations/{id}").and(accept(APPLICATION_JSON)), locationHandler::get);
//  }
}

