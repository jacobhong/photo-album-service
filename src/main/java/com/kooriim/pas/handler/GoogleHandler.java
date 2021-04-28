package com.kooriim.pas.handler;

import com.kooriim.pas.service.GoogleService;
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
    // TODO figure out how to do differently
    var accesstoken = serverRequest
      .headers()
      .header("Authorization")
      .get(0)
      .split(" ")[1];
//    var refresh = serverRequest
//                        .headers()
//                        .header("refresh")
//                        .get(0);
//    jwtDecoder =  NimbusJwtDecoder.withSecretKey(new SecretKeySpec("gI02EpNeYIiQKWiH1ywGQZl-TSbP7trHez-OdV1OVciXigL3z1vyKWmvjUAR74M4TiNg_mU7h6QHHWjnSu9EdQ".getBytes(), "HS256")).macAlgorithm(MacAlgorithm.HS256).build();

    jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    var jwt = this.jwtDecoder.decode(accesstoken);
    googleService.syncGooglePhotos(jwt).subscribe();

    return ServerResponse.ok().build();

  }

}
