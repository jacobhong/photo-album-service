package com.kooriim.pas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan(basePackages = "com.kooriim.pas")
@EnableWebFluxSecurity
@Configuration
//@EnableWebFlux
public class WebAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebAppApplication.class, args);
	}

//	@Bean
//	public RestTemplate restTemplate() {
//		final var httpClient = HttpClientBuilder.create().build();
//		final var restTemplate = new RestTemplate();
//		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
//		return restTemplate;
//	}
}
