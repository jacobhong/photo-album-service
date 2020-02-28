package com.kooriim.pas.controller;

import com.kooriim.pas.repository.AlbumRepository;
import com.kooriim.pas.domain.Album;
import com.kooriim.pas.service.AlbumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/albums")
public class AlbumController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private AlbumRepository albumRepository;
  @Autowired
  private AlbumService albumService;

  @RequestMapping(value = "/{id}", method = RequestMethod.GET, consumes = "application/json")
  public ResponseEntity<Set<Album>> getAlbumById(@PathVariable(name = "id") Integer id, @RequestParam("withPhotos") Boolean withPhotos) {
    logger.info("getting album by id {}", id);
    return new ResponseEntity(albumService.getAlbumById(id, withPhotos), HttpStatus.OK);
  }

  @RequestMapping(value = "", method = RequestMethod.GET)
  public ResponseEntity<Set<Album>> getAlbums() {
    logger.info("getting all albums");
    return new ResponseEntity(albumService.getAlbums(), HttpStatus.OK);
  }

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "application/json")
  public ResponseEntity<Album> createAlbum(@RequestBody Album album) {
    logger.info("creating album {}", album.getTitle());
    return new ResponseEntity(albumService.saveAlbum(album), HttpStatus.OK);
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, consumes = "application/json")
  public ResponseEntity<Void> addPhotosToAlbum(@PathVariable("id") Integer albumId,
                                               @RequestBody List<Integer> ids) {
    logger.info("adding photoIds: {}, to album: {}", ids, albumId);
    albumService.addPhotosToAlbum(albumId, ids);
    return new ResponseEntity(HttpStatus.OK);

  }
}
