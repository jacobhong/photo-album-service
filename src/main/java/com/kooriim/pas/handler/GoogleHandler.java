package com.kooriim.pas.handler;

import com.kooriim.pas.service.GoogleService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class GoogleHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private NimbusJwtDecoder jwtDecoder;

  @Autowired
  private GoogleService googleService;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  public Mono<ServerResponse> syncGooglePhotos(final ServerRequest serverRequest) {
    logger.info("syncing google photos {}", serverRequest.queryParams());
    Instant startDate = null;
    Instant endDate = null;
    if (serverRequest.queryParam("startDate").isPresent()) {
      startDate = Instant.ofEpochMilli(Long.valueOf(serverRequest.queryParam("startDate").get()));

    }
    if (serverRequest.queryParam("endDate").isPresent()) {
      endDate = Instant.ofEpochMilli(Long.valueOf(serverRequest.queryParam("endDate").get()));
    }
    var accesstoken = serverRequest
                        .headers()
                        .header("Authorization")
                        .get(0)
                        .split(" ")[1];
//    jwtDecoder =  NimbusJwtDecoder.withSecretKey(new SecretKeySpec("gI02EpNeYIiQKWiH1ywGQZl-TSbP7trHez-OdV1OVciXigL3z1vyKWmvjUAR74M4TiNg_mU7h6QHHWjnSu9EdQ".getBytes(), "HS256")).macAlgorithm(MacAlgorithm.HS256).build();

    jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    var jwt = this.jwtDecoder.decode(accesstoken);
    googleService.syncGooglePhotos(jwt, Map.of("startDate", startDate, "endDate", endDate)).subscribe();

    return ServerResponse.ok().build();

  }

}
