package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  public ResponseEntity<Void> deletePhoto(@PathVariable Integer id) {
    logger.info("deleting photo id: {}", id);
    photoService.deletePhotos(new ArrayList<>() {{
      add(id);
    }});
    return new ResponseEntity(HttpStatus.OK);
  }

  //
//  @RequestMapping(value = "", method = RequestMethod.DELETE)
//  public ResponseEntity<Void> deletePhoto(@RequestParam("photoIds") List<Integer> ids) {
//    logger.info("deleting photo id: {}", ids);
//    photoService.deletePhotos(ids);
//    return new ResponseEntity(HttpStatus.OK);
//  }
//
//  @RequestMapping(value = "", method = RequestMethod.PATCH)
//  public ResponseEntity<Void> patchPhotos(@RequestBody List<Photo> photos) {
//    logger.info("patching photos: {}", photos);
//    photoService.patchPhotos(photos);
//    return new ResponseEntity(HttpStatus.OK);
//  }
//
  public Mono<ServerResponse> create(ServerRequest serverRequest) {
    return serverRequest
             .multipartData()
             .map(data -> data.toSingleValueMap().get("file")).cast(FilePart.class)
             .flatMap(file -> photoService.savePhoto(file)
                                .flatMap(photo -> ServerResponse.ok().bodyValue(photo)));
  }
}
