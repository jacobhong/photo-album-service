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
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

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
//    return photoService.getMediaItems(queryParams.toSingleValueMap(), pageable)
//             .collectList()
//             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
//    googleService.syncGooglePhotos().collectList().flatMap(x -> Mono.empty()).subscribe();
//    return ServerResponse.ok().build();
    googleService.syncGooglePhotos(serverRequest
                                     .headers()
                                     .header("Authorization")
                                     .get(0)
                                     .split(" ")[1])
      .subscribe();
    return ServerResponse.ok().build();

  }

}
