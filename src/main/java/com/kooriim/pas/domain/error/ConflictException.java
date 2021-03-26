package com.kooriim.pas.domain.error;

public class ConflictException extends RuntimeException {
  public ConflictException(String s) {
    super(s);
  }
}
