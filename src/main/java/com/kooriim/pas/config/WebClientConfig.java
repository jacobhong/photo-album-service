package com.kooriim.pas.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient() throws SSLException {
    final var sslContext = SslContextBuilder
                              .forClient()
                              .trustManager(InsecureTrustManagerFactory.INSTANCE)
                              .build();
    final var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
    final var webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .baseUrl("https://192.168.1.206.xip.io:8443")
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();
    return webClient;
  }
}
