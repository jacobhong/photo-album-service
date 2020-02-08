//package com.webapp.starter.service;
//
//import com.webapp.starter.domain.GoogleOidUser;
//import com.webapp.starter.repository.UserRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
//@Service
//public class CustomOidUserService extends OidcUserService {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  @Autowired
//  private UserRepository userRepository;
//
//  @Override
//  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
//    logger.info("LOADING GOOGLE USER");
//    OidcUser oidcUser = super.loadUser(userRequest);
//    Map attributes = oidcUser.getAttributes();
//    GoogleOidUser googleOidUserInfo = new GoogleOidUser();
//    googleOidUserInfo.setEmail((String) attributes.get("email"));
//    googleOidUserInfo.setGoogleId((String) attributes.get("sub"));
//    googleOidUserInfo.setName((String) attributes.get("name"));
//    updateUser(googleOidUserInfo);
//    return oidcUser;
//  }
//
//  private void updateUser(GoogleOidUser googleOidUserInfo) {
//    GoogleOidUser googleOidUser = userRepository.findByEmail(googleOidUserInfo.getEmail());
//    if(googleOidUser == null) {
//      googleOidUser = new GoogleOidUser();
//    }
//    googleOidUser.setEmail(googleOidUserInfo.getEmail());
//    googleOidUser.setName(googleOidUserInfo.getName());
//    googleOidUser.setGoogleId(googleOidUserInfo.getGoogleId());
//    userRepository.save(googleOidUser);
//  }
//}