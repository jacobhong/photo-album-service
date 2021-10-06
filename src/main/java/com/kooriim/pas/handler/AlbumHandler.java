package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Album;
import com.kooriim.pas.service.AlbumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private AlbumService albumService;

  public Mono<ServerResponse> delete(final ServerRequest serverRequest) {
    final var id = serverRequest.pathVariable("id");

    return albumService.deleteAlbum(Integer.valueOf(id))
             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
  }

  public Mono<ServerResponse> getAlbums(final ServerRequest serverRequest) {
    logger.info("Getting albums");
    final var page = serverRequest.queryParam("page");
    final var size = serverRequest.queryParam("size");
    return albumService.getAlbums(PageRequest.of(0, 10))
             .collectList()
             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
  }

  public Mono<ServerResponse> create(ServerRequest serverRequest) {
    return serverRequest
             .bodyToMono(Album.class)
             .flatMap(album -> albumService
                                 .saveOrUpdateAlbum(album)
                                 .flatMap(a -> ServerResponse.ok().bodyValue(a)));
  }

  public Mono<ServerResponse> addPhotosToAlbum(ServerRequest serverRequest) {
    logger.info("adding photo to albums");
    final var id = serverRequest.pathVariable("id");
    return serverRequest
             .bodyToMono(new ParameterizedTypeReference<List<String>>() {
             })
             .map(p -> p.stream().map(Integer::valueOf)
                         .collect(Collectors.toList()))
             .flatMap(p -> albumService.addPhotosToAlbum(Integer.valueOf(id), p)
                             .flatMap(a -> ServerResponse.ok().build()));
  }

  public Mono<ServerResponse> movePhotosToAlbum(ServerRequest serverRequest) {
    logger.info("moving photos from album to another album");
    final var fromAlbumId = serverRequest.pathVariable("fromAlbumId");
    final var toAlbum = serverRequest.pathVariable("toAlbum");
    return serverRequest
             .bodyToMono(new ParameterizedTypeReference<List<String>>() {
             })
             .map(p -> p.stream().map(Integer::valueOf)
                         .collect(Collectors.toList()))
             .flatMap(p -> albumService.movePhotosToAlbum(Integer.valueOf(fromAlbumId), toAlbum, p)
                             .flatMap(a -> ServerResponse.ok().build()));
  }
//
}
