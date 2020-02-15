//package com.kooriim.pas.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE)
//public class KooriimCorsFilter extends CorsFilter {
//  private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//  @Value("${cors.allowed-origin")
//  private static String allowedOrigin;
//
//  public KooriimCorsFilter() {
//    super(configurationSource());
//  }
//
//  private static UrlBasedCorsConfigurationSource configurationSource() {
//    CorsConfiguration config = new CorsConfiguration();
//    config.setAllowCredentials(true);
//    config.addAllowedOrigin(allowedOrigin);
//    config.addAllowedHeader("*");
//    config.addAllowedMethod("*");
//    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", config);
//    return source;
//  }
//
//}
