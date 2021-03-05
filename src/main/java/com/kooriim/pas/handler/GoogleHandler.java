package com.kooriim.pas.handler;

import com.kooriim.pas.service.GoogleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class GoogleHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private GoogleService googleService;

  public Mono<ServerResponse> syncGooglePhotos(final ServerRequest serverRequest) {
    logger.info("syncing google photos {}", serverRequest.queryParams());
//    final var page = serverRequest.queryParam("page").get();
//    final var size = serverRequest.queryParam("size").get();
//    if (StringUtils.isEmpty(page) || StringUtils.isEmpty(size)) {
//      return ServerResponse.badRequest().bodyValue("Must send page and size parameter");
//    }
//    final var queryParams = serverRequest.queryParams();
//    final var pageable = PageRequest.of(Integer.valueOf(page), Integer.valueOf(size));
//    return photoService.getPhotos(queryParams.toSingleValueMap(), pageable)
//             .collectList()
//             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
    return googleService.syncGooglePhotos()
             .flatMap(a -> Mono.empty());
  }
}
