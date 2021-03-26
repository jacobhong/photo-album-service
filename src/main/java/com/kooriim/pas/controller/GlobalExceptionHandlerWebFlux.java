//package com.kooriim.pas.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.autoconfigure.web.ErrorProperties;
//import org.springframework.boot.autoconfigure.web.ResourceProperties;
//import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
//import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
//import org.springframework.boot.web.reactive.error.ErrorAttributes;
//import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.server.*;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//import static org.springframework.web.reactive.function.server.RequestPredicates.all;
//import static org.springframework.web.reactive.function.server.RouterFunctions.route;
//
//@Configuration
//@Component
//@Order(-2)
//public class GlobalExceptionHandlerWebFlux extends DefaultErrorWebExceptionHandler {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//
//  private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);
//
//  private static final Map<HttpStatus.Series, String> SERIES_VIEWS;
//  private final ErrorProperties errorProperties;
//
//
//  static {
//    Map<HttpStatus.Series, String> views = new EnumMap<>(HttpStatus.Series.class);
//    views.put(HttpStatus.Series.CLIENT_ERROR, "4xx");
//    views.put(HttpStatus.Series.SERVER_ERROR, "5xx");
//    SERIES_VIEWS = Collections.unmodifiableMap(views);
//  }
//  /**
//   * Create a new {@code DefaultErrorWebExceptionHandler} instance.
//   *
//   * @param errorAttributes    the error attributes
//   * @param resourceProperties the resources configuration properties
//   * @param errorProperties    the error configuration properties
//   * @param applicationContext the current application context
//   */
//  public GlobalExceptionHandlerWebFlux(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ApplicationContext applicationContext) {
//    super(errorAttributes, resourceProperties, null, applicationContext);
//    this.errorProperties = new ErrorProperties();
//    this.setMessageWriters( Collections.emptyList());
//
//  }
//
//  @Override
//  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
//    return route(acceptsTextHtml(), this::renderErrorView).andRoute(all(), this::renderErrorResponse);
//  }
//
////  private Mono<ServerResponse> renderErrorResponse(
////    ServerRequest request) {
////
////    Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);
////
////    return ServerResponse.status(HttpStatus.BAD_REQUEST)
////             .contentType(MediaType.APPLICATION_JSON)
////             .body(BodyInserters.fromValue(errorPropertiesMap));
////  }
//
//  @Override
//  protected Mono<ServerResponse> renderErrorView(ServerRequest request) {
//    boolean includeStackTrace = isIncludeStackTrace(request, MediaType.TEXT_HTML);
//    Map<String, Object> error = getErrorAttributes(request, includeStackTrace);
//    int errorStatus = getHttpStatus(error);
//    ServerResponse.BodyBuilder responseBody = ServerResponse.status(errorStatus).contentType(TEXT_HTML_UTF8);
//    return Flux.just(getData(errorStatus).toArray(new String[] {}))
//             .flatMap((viewName) -> renderErrorView(viewName, responseBody, error))
//             .switchIfEmpty(this.errorProperties.getWhitelabel().isEnabled()
//                              ? renderDefaultErrorView(responseBody, error) : Mono.error(getError(request)))
//             .next();
//  }
//
////  @Override
////  public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
////
////    DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
////    serverWebExchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
////    serverWebExchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
////    DataBuffer dataBuffer = bufferFactory.wrap(throwable.getMessage().getBytes());
//////    logger.error("Error: {}", throwable.getCause());
////    return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
////  }
////
////  public class HttpError {
////
////    private String message;
////
////    HttpError(String message) {
////      this.message = message;
////    }
////
////    public String getMessage() {
////      return message;
////    }
////  }
//private List<String> getData(int errorStatus) {
//  List<String> data = new ArrayList<>();
//  data.add("error/" + errorStatus);
//  HttpStatus.Series series = HttpStatus.Series.resolve(errorStatus);
//  if (series != null) {
//    data.add("error/" + SERIES_VIEWS.get(series));
//  }
//  data.add("error/error");
//  return data;
//}
//}
