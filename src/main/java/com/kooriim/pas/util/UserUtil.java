package com.kooriim.pas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

//@Service
public class UserUtil {
  private static final Logger logger = LoggerFactory.getLogger(UserUtil.class);

  public static Mono<String> getUserGoogleId() {
    return ReactiveSecurityContextHolder
             .getContext()
             .doOnError(error -> logger.error("error authorizing user context {}", error))
             .publishOn(Schedulers.boundedElastic())
             .map(SecurityContext::getAuthentication)
             .doOnError(error -> logger.error("error authorizing user auth {}", error))
             .map(Authentication::getName)
             .doOnError(error -> logger.error("error authorizing user name {}", error))
             .doOnNext(name -> logger.info("getting photo for googleId {}", name));
  }
}
