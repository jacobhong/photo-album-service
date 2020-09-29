package com.kooriim.pas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

import java.text.NumberFormat;

@SpringBootApplication
@ComponentScan(basePackages = "com.kooriim.pas")
@EnableWebFluxSecurity
@Configuration
public class WebAppApplication {
private static Logger logger = LoggerFactory.getLogger(WebAppApplication.class);
	public static void main(String[] args) {
		Runtime runtime = Runtime.getRuntime();

		final NumberFormat format = NumberFormat.getInstance();

		final long maxMemory = runtime.maxMemory();
		final long allocatedMemory = runtime.totalMemory();
		final long freeMemory = runtime.freeMemory();
		final long mb = 1024 * 1024;
		final String mega = " MB";

		logger.info("========================== Memory Info ==========================");
		logger.info("Free memory: " + format.format(freeMemory / mb) + mega);
		logger.info("Allocated memory: " + format.format(allocatedMemory / mb) + mega);
		logger.info("Max memory: " + format.format(maxMemory / mb) + mega);
		logger.info("Total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / mb) + mega);
		logger.info("=================================================================\n");
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
