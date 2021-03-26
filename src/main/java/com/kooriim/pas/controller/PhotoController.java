//package com.kooriim.pas.controller;
//
//import com.kooriim.pas.domain.MediaItem;
//import com.kooriim.pas.service.MediaItemService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("photos")
//public class PhotoController {
//
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  @Autowired
//  private MediaItemService photoService;
//
//  @RequestMapping(value = "")
//  public ResponseEntity<Page<List<MediaItem>>> getMediaItems(@RequestParam Map<String, String> params, Pageable pageable) {
//    logger.info("getting all photos with queryParams: {} pageable: {}", params, pageable);
//    return new ResponseEntity(photoService.getMediaItems(params, pageable), HttpStatus.OK);
//  }
//
//  @RequestMapping(value = "/{id}")
//  public ResponseEntity<MediaItem> getPhotoById(@PathVariable("id") Integer photoId,
//                                            @RequestParam(defaultValue = "false", value = "compressedImage") Boolean setcompressedImage) {
//    logger.info("getting photo by id: {}", photoId);
//    final var photo = photoService.getPhotoById(photoId, setcompressedImage);
//
//    ResponseEntity response;
//    if (photo.isPresent()) {
//      response = new ResponseEntity(photo.get(), HttpStatus.OK);
//    } else {
//      response = new ResponseEntity(HttpStatus.NOT_FOUND);
//    }
//    return response;
//  }
//
//  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
//  public ResponseEntity<Void> deletePhoto(@PathVariable Integer id) {
//    logger.info("deleting photo id: {}", id);
//    photoService.deleteMediaItems(new ArrayList<>() {{
//      add(id);
//    }});
//    return new ResponseEntity(HttpStatus.OK);
//  }
//
//  @RequestMapping(value = "", method = RequestMethod.DELETE)
//  public ResponseEntity<Void> deletePhoto(@RequestParam("photoIds") List<Integer> ids) {
//    logger.info("deleting photo id: {}", ids);
//    photoService.deleteMediaItems(ids);
//    return new ResponseEntity(HttpStatus.OK);
//  }
//
//  @RequestMapping(value = "", method = RequestMethod.PATCH)
//  public ResponseEntity<Void> patchPhotos(@RequestBody List<MediaItem> photos) {
//    logger.info("patching photos: {}", photos);
//    photoService.patchPhotos(photos);
//    return new ResponseEntity(HttpStatus.OK);
//  }
//
//  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "multipart/form-data")
//  public ResponseEntity<MediaItem> create(@RequestParam("file") MultipartFile file) throws IOException {
//    logger.info("Creating photo {}", file.getName());
//    return new ResponseEntity(photoService.createMediaItem(file), HttpStatus.OK);
//  }
//}
