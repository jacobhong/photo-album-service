//package com.kooriim.pas.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsConfigurationSource;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//import org.springframework.web.reactive.config.CorsRegistry;
//import org.springframework.web.reactive.config.WebFluxConfigurer;
//
//import java.util.Arrays;
//
//@Configuration
//  @Order(Ordered.HIGHEST_PRECEDENCE)
//public class KooriimCorsFilter implements WebFluxConfigurer {
////  @Bean
////  public CorsConfigurationSource corsConfigurationSource() {
////    CorsConfiguration config = new CorsConfiguration();
////    config.setAllowCredentials(true);
////    config.setAllowedOrigins(Arrays.asList("https://now.kooriim.com"));
////    config.addAllowedHeader("*");
////    config.addAllowedMethod("*");
////    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////    source.registerCorsConfiguration("/**", config);
////    return source;
//////    return new CorsWebFilter(source);
////  }
////  @Override
////  protected void configure(HttpSecurity http) throws Exception {
////    http
////      // by default uses a Bean by the name of corsConfigurationSource
////      .cors();
////  }
//
//
//  @Override
//  public void addCorsMappings(CorsRegistry registry) {
//    registry.addMapping("/**")
//      .allowedOrigins("https://poop.com")
//      .allowedHeaders("*")
//      .allowedMethods("*")
//      .allowedMethods("*");
//  }
//
////  private static UrlBasedCorsConfigurationSource configurationSource() {
////    CorsConfiguration config = new CorsConfiguration();
////    config.setAllowCredentials(true);
////    config.addAllowedOrigin("*");
////    config.addAllowedHeader("*");
////    config.addAllowedMethod("*");
////    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////    source.registerCorsConfiguration("/**", config);
////    return source;
////  }
//
////  @Bean
////  CorsWebFilter corsFilter() {
////    CorsConfiguration config = new CorsConfiguration();
////    config.setAllowCredentials(true);
////    config.addAllowedOrigin("https://www.wwwasdf.com");
////    config.addAllowedHeader("*");
////    config.addAllowedMethod("*");
////    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////    source.registerCorsConfiguration("/**", config);
////    return new CorsWebFilter(source);
////  }
//}
