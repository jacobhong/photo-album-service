package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PhotoHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private PhotoService photoService;

  public Mono<ServerResponse> getPhotos(final ServerRequest serverRequest) {
    logger.info("getting all photos with queryParams: {}", serverRequest.queryParams());
    final var page = serverRequest.queryParam("page");
    final var size = serverRequest.queryParam("size");
    final var queryParams = serverRequest.queryParams();
    return photoService.getPhotosByQueryParams(queryParams, PageRequest.of(Integer.valueOf(page.get()), Integer.valueOf(size.get())))
             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
  }

  public Mono<ServerResponse> getPhotoById(ServerRequest serverRequest) {
    logger.info("getting photo by id: {}", serverRequest.pathVariable("id"));
    serverRequest.principal().map(Principal::getName).flatMap(p -> {
      logger.info("hifewhfiehwfiewhfiew " + p);
      return Mono.just(p);
    }).subscribe();
    return photoService
             .getPhotoById(Integer.valueOf(serverRequest.pathVariable("id")), Boolean.TRUE)
             .flatMap(p -> ServerResponse.ok().bodyValue(p))
             .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> deletePhoto(ServerRequest serverRequest) {
    final var id = Integer.valueOf(serverRequest.pathVariable("id"));
    logger.info("deleting photo id: {}", id);
    photoService.deletePhotos(new ArrayList<>() {{
      add(id);
    }});
    return ServerResponse.ok().build();
  }

  //
  public Mono<ServerResponse> deletePhotos(@RequestParam("photoIds") List<Integer> ids) {
    logger.info("deleting photo id: {}", ids);
    photoService.deletePhotos(ids);
    return ServerResponse.ok().build();
  }
//
  @RequestMapping(value = "", method = RequestMethod.PATCH)
  public ResponseEntity<Void> patchPhotos(@RequestBody List<Photo> photos) {
    logger.info("patching photos: {}", photos);
    photoService.patchPhotos(photos);
    return new ResponseEntity(HttpStatus.OK);
  }
//
  public Mono<ServerResponse> create(ServerRequest serverRequest) {
    return serverRequest
             .multipartData()
             .map(data -> data.toSingleValueMap().get("file")).cast(FilePart.class)
             .flatMap(file -> photoService.savePhoto(file)
                                .flatMap(photo -> ServerResponse.ok().bodyValue(photo)));
  }
}
