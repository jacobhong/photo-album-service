package com.kooriim.pas.config;

import com.kooriim.pas.service.GoogleUserRegistration;
import com.nimbusds.jose.KeySourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Value("${require-ssl}")
  private boolean sslRequired;

  @Value("${allowed-origin}")
  private String origin;

  @Autowired
  private GoogleUserRegistration googleUserRegistration;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.applyPermitDefaultValues();
    corsConfig.addAllowedMethod("OPTIONS");
    corsConfig.addAllowedMethod("PATCH");
    corsConfig.addAllowedMethod("DELETE");
    corsConfig.setAllowedOrigins(Arrays.asList(origin));
    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  @Bean
  public SecurityWebFilterChain securitygWebFilterChain(
    ServerHttpSecurity http) throws MalformedURLException, KeySourceException {
    http.cors().configurationSource(corsConfigurationSource());
    http.csrf().disable();
    http.authorizeExchange()
      .pathMatchers("/actuator/*").permitAll()
      .pathMatchers("/photos/**").hasAnyAuthority("kooriim-fe", "kooriim-mobile")
      .pathMatchers("/albums/**").hasAnyAuthority("kooriim-fe", "kooriim-mobile")
      .pathMatchers("/users/**").hasAnyAuthority("kooriim-fe", "kooriim-mobile")
      .anyExchange()
      .authenticated()
      .and()
      .oauth2ResourceServer()
      .jwt()
      .jwtAuthenticationConverter(grantedAuthoritiesExtractor()).jwtDecoder(jwtDecoder());
    return http.build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    return new NimbusReactiveJwtDecoder(this.jwkSetUri);
  }

  public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
    return new ReactiveJwtAuthenticationConverterAdapter(new GrantedAuthoritiesExtractor());
  }

  public class GrantedAuthoritiesExtractor extends JwtAuthenticationConverter {

    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
      final var authorities = (Collection<String>)
                                jwt.getClaims().get("grants");
      if (authorities.size() > 0) {
        // TODO move this somewhere else
        googleUserRegistration.saveUserIfNotExist(jwt.getClaims());
      }
      logger.info("jwt has these authorities {}", authorities);
      return authorities.stream()
               .map(SimpleGrantedAuthority::new)
               .collect(Collectors.toList());
    }
  }
}
