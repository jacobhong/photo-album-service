package com.webapp.starter.controller;

import com.webapp.starter.domain.Album;
import com.webapp.starter.domain.Photo;
import com.webapp.starter.repository.AlbumRepository;
import com.webapp.starter.service.AlbumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/albums")
public class AlbumController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private AlbumService albumService;

//  @RequestMapping(value = "/{id}", method = RequestMethod.GET, consumes = "application/json")
//  public ResponseEntity<Set<Photo>> getAlbumById(@PathVariable(name="id") Integer id) {
//    logger.info("GETTING ALBUM");
//    return new ResponseEntity(albumService.getPhotosByAlbumId(id), HttpStatus.OK);
//  }

  @RequestMapping(value = "", method = RequestMethod.GET)
  public ResponseEntity<Set<Album>> getAlbums() {
    logger.info("GETTING ALBUMS");
    return new ResponseEntity(albumService.getAlbums(), HttpStatus.OK);
  }

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "application/json")
  public ResponseEntity<Album> createAlbum(@RequestBody Album album) {
    logger.info("creating album");
    return new ResponseEntity(albumService.saveAlbum(album), HttpStatus.OK);

  }
}
