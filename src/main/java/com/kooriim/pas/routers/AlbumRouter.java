package com.kooriim.pas.routers;

import com.kooriim.pas.handler.AlbumHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.kooriim.pas.routers.MediaItemRouter.ACCEPTS_JSON;
import static com.kooriim.pas.routers.MediaItemRouter.CONTENT_TYPE_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
@Configuration

public class AlbumRouter {
  @Bean
  public RouterFunction<ServerResponse> route3(AlbumHandler albumHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(GET("/albums").and(ACCEPTS_JSON), albumHandler::getAlbums)
        .andRoute(POST("/albums").and(CONTENT_TYPE_JSON), albumHandler::create)
        .andRoute(PATCH("/albums/{id}").and(CONTENT_TYPE_JSON), albumHandler::addPhotosToAlbum)
        .andRoute(DELETE("/albums/{id}").and(ACCEPTS_JSON), albumHandler::delete));
  }
}
