//package com.kooriim.pas;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
//@Configuration
//@Order(1)
//@Profile("test")
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//    http.csrf().disable().authorizeRequests().anyRequest().permitAll();
//  }
//
//}
