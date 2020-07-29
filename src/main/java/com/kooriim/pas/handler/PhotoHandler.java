package com.kooriim.pas.handler;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.PhotoService;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
public class PhotoHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private PhotoService photoService;

  public Mono<ServerResponse> getPhotos(final ServerRequest serverRequest) {
    logger.info("getting all photos with queryParams: {}", serverRequest.queryParams());
    final var page = serverRequest.queryParam("page").get();
    final var size = serverRequest.queryParam("size").get();
    if (StringUtils.isEmpty(page) || StringUtils.isEmpty(size)) {
      return ServerResponse.badRequest().bodyValue("Must send page and size parameter");
    }
    final var queryParams = serverRequest.queryParams();
    final var pageable = PageRequest.of(Integer.valueOf(page), Integer.valueOf(size));
    return photoService.getPhotosByQueryParams(queryParams, pageable)
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
    return serverRequest
             .bodyToMono(JSONObject.class)
             .doOnError(error -> logger.error("Errer deleting photos {}", error.getMessage()))
             .doOnNext(ids -> logger.info("deleting photo id: {}", ids))
             .flatMap(jsonObject -> {
               var ids = (List<Integer>) jsonObject.get("ids");
               return photoService.deletePhotos(ids)
                        .flatMap(v -> ServerResponse.ok().build());
             });

  }

  public Mono<ServerResponse> patchPhotos(ServerRequest serverRequest) {
    return serverRequest.bodyToMono(new ParameterizedTypeReference<List<Photo>>() {
    })
             .doOnNext(photos -> logger.info("patching photos: {}", photos))
             .flatMap(photos -> photoService.patchPhotos(photos).flatMap(v -> ServerResponse.ok().build()));

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
