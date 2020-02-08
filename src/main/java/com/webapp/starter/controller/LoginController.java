//package com.webapp.starter.controller;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.ResolvableType;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.registration.ClientRegistration;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.core.OAuth2AccessToken;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.ModelAndView;
//
//import javax.servlet.http.HttpServletResponse;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("login")
//public class LoginController {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
////  @Autowired
////  private OAuth2AuthorizedClientService authorizedClientService;
////  private static String authorizationRequestBaseUri
////    = "oauth2/authorization";
////  Map<String, String> oauth2AuthenticationUrls
////    = new HashMap<>();
////
////  @Autowired
////  private ClientRegistrationRepository clientRegistrationRepository;
//////
//////  @GetMapping("/oauth_login")
//////  public String getLoginPage(Model model) {
//////    // ...
//////
//////    return "oauth_login";
//////  }
////
////  public String getLoginPage(Model model) {
////    Iterable<ClientRegistration> clientRegistrations = null;
////    ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository)
////                            .as(Iterable.class);
////    if (type != ResolvableType.NONE &&
////          ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
////      clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
////    }
////
////    clientRegistrations.forEach(registration ->
////                                  oauth2AuthenticationUrls.put(registration.getClientName(),
////                                    authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
////    model.addAttribute("urls", oauth2AuthenticationUrls);
////
////    return "oauth_login";
////  }
//////  @PostMapping("/api/logout")
//////  public void logout(HttpServletRequest request) {
//////    request.getSession(false).invalidate();
//////  }
////@RequestMapping(value = "/redirect", method = RequestMethod.GET)
////public void method(Authentication authentication) {
////  OAuth2AuthorizedClient authorizedClient =
////    this.authorizedClientService.loadAuthorizedClient("google", authentication.getName());
////
////  OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
////
////}
//  @RequestMapping(value = "/redirect2", method = RequestMethod.GET)
//  public ModelAndView method2() {
//    return new ModelAndView("redirect:" + "login");
//  }
//}
