package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.PhotoService;
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
             .collectList()
             .flatMap(photos -> ServerResponse.ok().bodyValue(photos));
  }

  public Mono<ServerResponse> getPhotoById(ServerRequest serverRequest) {
    logger.info("getting photo by id: {}", serverRequest.pathVariable("id"));
    return photoService
             .getPhotoById(Integer.valueOf(serverRequest.pathVariable("id")), Boolean.TRUE)
             .flatMap(p -> ServerResponse.ok().bodyValue(p))
             .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> deletePhoto(ServerRequest serverRequest) {
    final var id = Integer.valueOf(serverRequest.pathVariable("id"));
    logger.info("deleting photo id: {}", id);
    return photoService.deletePhotos(Arrays.asList(id)).flatMap(v -> ServerResponse.ok().build());
  }

  public Mono<ServerResponse> deletePhotos(ServerRequest serverRequest) {
    final var ids = (String) serverRequest.queryParam("photoIds").get();
    logger.info("deleting photo id: {}", ids);
    return photoService.deletePhotos(Arrays.asList(ids.split(","))
                                       .stream()
                                       .map(Integer::valueOf)
                                       .collect(Collectors.toList())).flatMap(v -> ServerResponse.ok().build());
  }

  public Mono<ServerResponse> patchPhotos(ServerRequest serverRequest) {
    final var photos = serverRequest.bodyToMono(new ParameterizedTypeReference<List<Photo>>() {
    });
    logger.info("patching photos: {}", photos);
    return photoService.patchPhotos(photos).flatMap(v -> ServerResponse.ok().build());
  }

  public Mono<ServerResponse> create(ServerRequest serverRequest) {
    logger.info("creating photo");
    return serverRequest
             .multipartData()
             .map(data -> data.toSingleValueMap().get("file")).cast(FilePart.class)
             .flatMap(file -> photoService.savePhoto(file)
                                .flatMap(photo -> ServerResponse.ok().bodyValue(photo)));
  }
}
