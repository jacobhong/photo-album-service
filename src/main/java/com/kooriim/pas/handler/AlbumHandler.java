package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Album;
import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.AlbumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private AlbumService albumService;

  //  @RequestMapping(value = "", method = RequestMethod.GET)
//  public ResponseEntity<Set<Album>> getAlbums(Pageable pageable) {
//    logger.info("getting all albums");
//    return new ResponseEntity(albumService.getAlbums(pageable), HttpStatus.OK);
//  }
  //  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "application/json")
//  public ResponseEntity<Album> saveOrUpdateAlbum(@RequestBody Album album) {
//    logger.info("creating album {}", album.getTitle());
//    return new ResponseEntity(albumService.saveOrUpdateAlbum(album), HttpStatus.OK);
//  }
//o
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
//
}
