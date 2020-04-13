package com.kooriim.pas.controller;

import com.kooriim.pas.domain.Photo;
import com.kooriim.pas.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("public-gallery")
public class PublicGalleryController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private PhotoService photoService;

  @RequestMapping(value = "")
  public ResponseEntity<Page<List<Photo>>> getPhotos(@RequestParam Map<String, String> params, Pageable pageable) {
    logger.info("getting all photos with queryParams: {} pageable: {}", params, pageable);
    return new ResponseEntity(photoService.getPhotosByQueryParams(params, pageable), HttpStatus.OK);
  }

}
