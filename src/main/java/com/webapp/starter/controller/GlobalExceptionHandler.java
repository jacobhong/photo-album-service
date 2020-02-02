package com.webapp.starter.controller;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity handleException(org.springframework.dao.DataIntegrityViolationException e) {
    logger.error(e.getMessage());
    // log exception
    return ResponseEntity
             .status(HttpStatus.CONFLICT)
             .body("Duplicate found");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity handleException(Exception e) {
    logger.error(e.getMessage());
    // log exception
    return ResponseEntity
             .status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body("Server error.");
  }
}
