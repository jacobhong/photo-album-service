package com.kooriim.pas.routers;

import com.kooriim.pas.domain.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class AlbumRouter {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//  @Autowired
//  private AlbumRepository albumRepository;
//  @Autowired
//  private AlbumService albumService;
////
////  @RequestMapping(value = "/{id}", method = RequestMethod.GET, consumes = "application/json")
////  public ResponseEntity<Set<Album>> getAlbumById(@PathVariable(name = "id") Integer id, @RequestParam Map<String, String> params) {
////    logger.info("getting album by id {}", id);
////    return new ResponseEntity(albumService.getAlbumById(id, params), HttpStatus.OK);
////  }
//
////  @RequestMapping(value = "", method = RequestMethod.GET)
//  public ResponseEntity<Set<Album>> getAlbums(Pageable pageable) {
//    logger.info("getting all albums");
//    return new ResponseEntity(albumService.getAlbums(pageable), HttpStatus.OK);
//  }
//
////  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "application/json")
//  public ResponseEntity<Album> saveOrUpdateAlbum(@RequestBody Album album) {
//    logger.info("creating album {}", album.getTitle());
//    return new ResponseEntity(albumService.saveOrUpdateAlbum(album), HttpStatus.OK);
//  }
//
////  @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, consumes = "application/json")
//  public ResponseEntity<Void> addPhotosToAlbum(@PathVariable("id") Integer albumId,
//                                               @RequestBody List<Integer> ids) {
//    logger.info("adding photoIds: {}, to album: {}", ids, albumId);
//    albumService.addPhotosToAlbum(albumId, ids);
//    return new ResponseEntity(HttpStatus.OK);
//
//  }
//
////  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
//  public ResponseEntity<Void> delete(@PathVariable("id") Integer albumId) {
//    logger.info("deleting album: {}", albumId);
//    albumService.deleteAlbum(albumId);
//    return new ResponseEntity(HttpStatus.OK);
//
//  }
}
