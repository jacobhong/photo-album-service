package com.kooriim.pas.config;

import com.kooriim.pas.service.GoogleUserRegistration;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.filter.CorsFilter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Value("${require-ssl}")
  private boolean sslRequired;

  @Autowired
  private GoogleUserRegistration googleUserRegistration;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    if (sslRequired == true) {
      http
        .requiresChannel()// ssl config
        .anyRequest()
        .requiresSecure();
    }
    http
      .cors()
      .and()
      .authorizeRequests(authorizeRequests ->
                           authorizeRequests
                             .antMatchers("/actuator/*").permitAll()
                             .antMatchers("/photos/**").hasAuthority("kooriim-fe")
                             .antMatchers("/albums/**").hasAuthority("kooriim-fe")
                             .antMatchers("/users/**").hasAuthority("kooriim-fe")
                             .anyRequest().authenticated()
      )
      .oauth2ResourceServer()
      .jwt()
      .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
      .decoder(jwtDecoder());
  }

  @Bean
  public JwtDecoder jwtDecoder() throws MalformedURLException, KeySourceException {
    final var jwsKeySelector =
      JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(new URL(this.jwkSetUri));

    final var jwtProcessor =
      new DefaultJWTProcessor<>();
    jwtProcessor.setJWSKeySelector(jwsKeySelector);

    return new NimbusJwtDecoder(jwtProcessor);
  }

  @Bean
  public CorsFilter corsFilter() {
   return new KooriimCorsFilter();
  }

  public Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
    final var jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter
                                 (new GrantedAuthoritiesExtractor());
    return jwtAuthenticationConverter;
  }

  public class GrantedAuthoritiesExtractor
    implements Converter<Jwt, Collection<GrantedAuthority>> {

    public Collection<GrantedAuthority> convert(Jwt jwt) {
      final var authorities = (Collection<String>)
                                jwt.getClaims().get("grants");
      if (authorities.size() > 0) {
        // TODO move this somewhere else
        googleUserRegistration.saveUserIfNotExist(jwt.getClaims());
      }
      return authorities.stream()
               .map(SimpleGrantedAuthority::new)
               .collect(Collectors.toList());
    }
  }
}
