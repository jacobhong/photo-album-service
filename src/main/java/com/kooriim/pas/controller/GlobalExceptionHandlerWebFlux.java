//package com.kooriim.pas.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.io.IOException;
//
//@Configuration
//@Component
////@Order(-2)
//public class GlobalExceptionHandlerWebFlux implements ErrorWebExceptionHandler {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  private ObjectMapper objectMapper;
//
//  public GlobalExceptionHandlerWebFlux(ObjectMapper objectMapper) {
//    this.objectMapper = objectMapper;
//  }
//
//  @Override
//  public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
//
//    DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
//    serverWebExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//    serverWebExchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
//    DataBuffer dataBuffer = bufferFactory.wrap(throwable.getMessage().getBytes());
////    logger.error("Error: {}", throwable.getCause());
//    return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
//  }
//
//  public class HttpError {
//
//    private String message;
//
//    HttpError(String message) {
//      this.message = message;
//    }
//
//    public String getMessage() {
//      return message;
//    }
//  }
//
//}
