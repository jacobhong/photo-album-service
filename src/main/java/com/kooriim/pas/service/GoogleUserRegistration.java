package com.kooriim.pas.service;

import com.kooriim.pas.domain.GoogleOidUser;
import com.kooriim.pas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GoogleUserRegistration {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserRepository userRepository;

  public void saveUserIfNotExist(Map<String, Object> userRequest) throws OAuth2AuthenticationException {
    final var googleOidUserInfo = new GoogleOidUser();
    googleOidUserInfo.setEmail((String) userRequest.get("email"));
    googleOidUserInfo.setGoogleId((String) userRequest.get("sub"));
    googleOidUserInfo.setName((String) userRequest.get("name"));
    updateUser(googleOidUserInfo);
  }

  private void updateUser(GoogleOidUser googleOidUserInfo) {
    logger.info("looking for user by email {}", googleOidUserInfo);
    userRepository.findByEmail(googleOidUserInfo.getEmail())
      .ifPresentOrElse(existingGoogleUser -> logger.info("found user {}", existingGoogleUser.getEmail()),
        () -> {
          userRepository.save(googleOidUserInfo);
          logger.info("saved new google user {}", googleOidUserInfo.getName());
        });
  }
}