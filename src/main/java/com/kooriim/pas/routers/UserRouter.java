package com.kooriim.pas.routers;

import com.kooriim.pas.handler.GoogleHandler;
import com.kooriim.pas.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;

@Configuration
public class UserRouter {
  public static final RequestPredicate ACCEPTS_JSON = accept(APPLICATION_JSON).or(accept(APPLICATION_STREAM_JSON));
  public static final RequestPredicate CONTENT_TYPE_JSON =
    contentType(APPLICATION_JSON).or(contentType(APPLICATION_STREAM_JSON));

  @Bean
  public RouterFunction<ServerResponse> route5(UserHandler userHandler) {
    return nest(path("/photo-album-service"),
      RouterFunctions.route(POST("/refreshToken").and(ACCEPTS_JSON), userHandler::refreshToken));

  }
}