package com.kooriim.pas;

import com.nimbusds.jose.KeySourceException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.net.MalformedURLException;

@Configuration
@Order(1)
@Profile("test")
public class SecurityConfigTest {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws MalformedURLException, KeySourceException {
    http.csrf().disable();
    http.authorizeExchange().anyExchange().permitAll();
    return http.build();
  }

}
