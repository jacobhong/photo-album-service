package com.kooriim.pas.handler;

import com.kooriim.pas.domain.MediaItem;
import com.kooriim.pas.domain.MediaItemMetaData;
import com.kooriim.pas.service.MediaItemService;
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
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

@Service
public class MediaItemHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private MediaItemService mediaItemService;

  public Mono<ServerResponse> getMediaItems(final ServerRequest serverRequest) {
    logger.info("getting all mediaItems with queryParams: {}", serverRequest.queryParams());
    final var page = serverRequest.queryParam("page").get();
    final var size = serverRequest.queryParam("size").get();
    if (StringUtils.isEmpty(page) || StringUtils.isEmpty(size)) {
      return ServerResponse.badRequest().bodyValue("Must send page and size parameter");
    }
    final var queryParams = serverRequest.queryParams();
    final var pageable = PageRequest.of(Integer.valueOf(page), Integer.valueOf(size));
    return mediaItemService.getMediaItems(queryParams.toSingleValueMap(), pageable)
             .collectList()
             .flatMap(mediaItems -> ServerResponse.ok().bodyValue(mediaItems));
  }

  public Mono<ServerResponse> getMediaItemById(ServerRequest serverRequest) {
    logger.info("getting mediaItem by id: {}", serverRequest.pathVariable("id"));
    return mediaItemService
             .getMediaItemById(Integer.valueOf(serverRequest.pathVariable("id")),
               serverRequest.queryParams().toSingleValueMap())
             .flatMap(mediaItem -> ServerResponse.ok().bodyValue(mediaItem))
             .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> getVideoByTitle(ServerRequest serverRequest) {
    logger.info("getting video by id: {}", serverRequest.pathVariable("title"));
    return mediaItemService
             .getVideoByTitleS3(serverRequest.pathVariable("title"))
             .flatMap(video -> ServerResponse.ok().bodyValue(video))
             .switchIfEmpty(ServerResponse.notFound().build());

  }

  public Mono<ServerResponse> deleteMediaItem(ServerRequest serverRequest) {
    final var id = Integer.valueOf(serverRequest.pathVariable("id"));
    logger.info("deleting mediaItem id: {}", id);
    return mediaItemService.deleteMediaItems(Arrays.asList(id)).flatMap(v -> ServerResponse.ok().build());
  }

  public Mono<ServerResponse> deleteMediaItems(ServerRequest serverRequest) {
    return serverRequest
             .bodyToMono(JSONObject.class)
             .doOnError(error -> logger.error("Error deleting mediaItems {}", error.getMessage()))
             .doOnNext(ids -> logger.info("deleting mediaItems id: {}", ids))
             .flatMap(jsonObject -> {
               var ids = (List<Integer>) jsonObject.get("ids");
               return mediaItemService.deleteMediaItems(ids)
                        .flatMap(v -> ServerResponse.ok().build());
             });

  }

  public Mono<ServerResponse> patchMediaItems(ServerRequest serverRequest) {
    return serverRequest.bodyToMono(new ParameterizedTypeReference<List<MediaItem>>() {
    })
             .doOnNext(mediaItems -> logger.info("patching mediaItems: {}", mediaItems))
             .flatMap(mediaItems -> mediaItemService.patchMediaItems(mediaItems).flatMap(v -> ServerResponse.ok().build()));

  }

  public Mono<ServerResponse> createMetaData(ServerRequest serverRequest) {
    logger.info("creating metadata for id: {}", serverRequest.pathVariable("id"));
    return serverRequest.bodyToMono(MediaItemMetaData.class)
             .doOnNext(metaData -> logger.info("saving metaData: {}", metaData))
             .flatMap(metaData -> mediaItemService.createMetaData(metaData).flatMap(v -> ServerResponse.ok().build()));

  }

  /**
   * if conflict throw 404
   * @param serverRequest
   * @return
   */
  public Mono<ServerResponse> create(ServerRequest serverRequest) {
    logger.info("creating mediaItem with request {}", serverRequest.headers());
    return serverRequest
             .multipartData()
             .publishOn(Schedulers.elastic())
             .doOnNext(data -> logger.info("got multi part data " + data.toSingleValueMap()))
             .filter(data -> data.toSingleValueMap().get("file") != null)
             .map(data -> data.toSingleValueMap().get("file")).cast(FilePart.class)
             .flatMap(file -> mediaItemService.createMediaItem(file)
                                .flatMap(mediaItem -> ServerResponse.ok().bodyValue(mediaItem)));
  }


}
