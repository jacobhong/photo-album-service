package com.kooriim.pas.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${baseKeycloakUrl}")
  private String baseKeycloakUrl;

  @Bean
  @Qualifier("webClient")
  public WebClient webClient() throws SSLException {
    final var sslContext = SslContextBuilder
                              .forClient()
                              .trustManager(InsecureTrustManagerFactory.INSTANCE)
                              .build();
    final var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
    final var webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .baseUrl(baseKeycloakUrl)
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();
    return webClient;
  }

  @Bean
  @Qualifier("googleClient")
  public WebClient googleClient() throws SSLException {
    final var sslContext = SslContextBuilder
                             .forClient()
                             .trustManager(InsecureTrustManagerFactory.INSTANCE)
                             .build();
    final var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
    final var webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .baseUrl("https://oauth2.googleapis.com")
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();
    return webClient;
  }
}
