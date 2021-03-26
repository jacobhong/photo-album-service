package com.kooriim.pas.controller;

import com.kooriim.pas.service.MediaItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

//@RestController
//@RequestMapping("person")
@Service
public class PublicGalleryController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private MediaItemService mediaItemService;

  @RequestMapping(value = "")
  public Mono<ServerResponse> getPhotos(ServerRequest serverRequest) {
    ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .map(Authentication::getName).doOnNext(p -> logger.info(p)).subscribeOn(Schedulers.boundedElastic()).subscribe();
    return ServerResponse.ok().body(Mono.just("hey"), String.class);
  }

}
