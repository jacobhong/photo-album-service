package com.webapp.starter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("")
public class UserController {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  @GetMapping("/user")
//  public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
//    return Collections.singletonMap("name", principal.getAttribute("name"));
//  }

  @GetMapping("/")
  public String index(@AuthenticationPrincipal Jwt jwt) {
    return String.format("Hello, %s!", jwt.getSubject());
  }

  @GetMapping("/user/token")
  public String user(Authentication authentication) {
    return authentication.getPrincipal().toString();
  }

  @GetMapping("/callback")
  public ModelAndView method2() {
    return new ModelAndView("redirect:" + "192.168.1.206.xip.io:4200/photo-gallery");
  }
}

