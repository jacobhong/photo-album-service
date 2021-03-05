package com.kooriim.pas;

import com.kooriim.pas.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;

@ComponentScan(basePackages = {"com.kooriim.pas"}, excludeFilters = {
  @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class)})
@Profile("test")
@SpringBootApplication
@Configuration
class WebAppApplication {

  @Test
  void contextLoads() {
  }

}
