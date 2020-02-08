package com.webapp.starter.service;

import com.webapp.starter.domain.GoogleOidUser;
import com.webapp.starter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GoogleUserRegistration  {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserRepository userRepository;

  public void saveUserIfNotExist(Map<String, Object> userRequest) throws OAuth2AuthenticationException {
    logger.info("LOADING GOOGLE USER");
    final var googleOidUserInfo = new GoogleOidUser();
    googleOidUserInfo.setEmail((String) userRequest.get("email"));
    googleOidUserInfo.setGoogleId((String) userRequest.get("sub"));
    googleOidUserInfo.setName((String) userRequest.get("name"));
    updateUser(googleOidUserInfo);
  }

  private void updateUser(GoogleOidUser googleOidUserInfo) {
    final var existingGoogleUser = userRepository.findByEmail(googleOidUserInfo.getEmail());
    if(existingGoogleUser == null) {
      userRepository.save(googleOidUserInfo);
      logger.info("saved google user {}", googleOidUserInfo.getName());
    }
  }
}