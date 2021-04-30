package com.kooriim.pas.handler;

import com.kooriim.pas.repository.UserRepository;
import com.kooriim.pas.service.GoogleService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class UserHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private GoogleService googleService;

  public Mono<ServerResponse> refreshToken(ServerRequest serverRequest) {
    logger.info("creating refreshtoken with request {}", serverRequest.headers());
    return getUserAccessToken().flatMap(jwt -> {
      var user = userRepository.findByGoogleId(jwt.getClaimAsString("sub"));
      return googleService.getGoogleAccessToken(jwt).flatMap(x -> {
        if (StringUtils.isNotEmpty(x.getRefreshToken())) {
          user.get().setRefreshToken(x.getRefreshToken());
          logger.info("setting refresh token on user");
          userRepository.save(user.get());
          return ServerResponse.ok().build();
        }
        return Mono.empty();
      });
    });
  }

  public Mono<Jwt> getUserAccessToken() {
    return ReactiveSecurityContextHolder
             .getContext()
             .doOnError(error -> logger.error("error authorizing user context {}", error))
             .publishOn(Schedulers.boundedElastic())
             .map(SecurityContext::getAuthentication)
             .cast(JwtAuthenticationToken.class)
             .map(JwtAuthenticationToken::getToken)
             .doOnNext(x -> logger.info("Got User AccessToken"));
  }
}
