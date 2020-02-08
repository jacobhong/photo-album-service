package com.webapp.starter.controller;

import com.webapp.starter.domain.Photo;
import com.webapp.starter.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("photos")
public class PhotoController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private PhotoService photoService;

//  @PreAuthorize("hasAuthority('kooriim-fe")
  @RequestMapping(value = "")
  public ResponseEntity<List<Photo>> getPhotos(@RequestParam Map<String, String> params) {
    logger.info("getting all photos with queryParams: {}", params);
    return new ResponseEntity(photoService.getPhotosByQueryParams(params), HttpStatus.OK);
  }

  @RequestMapping(value = "/{id}")
  public ResponseEntity<Photo> getPhotoById(@PathVariable("id") Integer photoId,
                                            @RequestParam(defaultValue = "false", value = "srcImage") Boolean setSrcImage) {
    logger.info("getting photo by id: {}", photoId);
    final var photo = photoService.getPhotoById(photoId, setSrcImage);

    ResponseEntity response;
    if (photo.isPresent()) {
      response = new ResponseEntity(photo.get(), HttpStatus.OK);
    } else {
      response = new ResponseEntity(HttpStatus.NOT_FOUND);
    }
    return response;
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<Void> deletePhoto(@PathVariable Integer id) {
    logger.info("deleting photo id: {}", id);
    photoService.deletePhotos(new ArrayList<>() {{
      add(id);
    }});
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "", method = RequestMethod.DELETE)
  public ResponseEntity<Void> deletePhoto(@RequestParam("photoIds") List<Integer> ids) {
    logger.info("deleting photo id: {}", ids);
    photoService.deletePhotos(ids);
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = "multipart/form-data")
  public ResponseEntity<Photo> create(@RequestParam("file") MultipartFile file) throws IOException {
    logger.info("Creating photo");
    return new ResponseEntity(photoService.savePhoto(file), HttpStatus.OK);
  }
}
