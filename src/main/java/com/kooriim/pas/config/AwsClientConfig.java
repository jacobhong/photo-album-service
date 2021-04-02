package com.kooriim.pas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.time.Duration;

@Component
public class AwsClientConfig {

  @Bean
  public S3AsyncClient awsS3Client() {
    final var client = S3AsyncClient.builder()
                         .region(Region.US_WEST_1)
                         .httpClientBuilder(NettyNioAsyncHttpClient.builder()
//                                              .maxConcurrency(80)
                                              .readTimeout(Duration.ofMinutes(5))
                                              .writeTimeout(Duration.ofMinutes(5))
                                              .connectionTimeout(Duration.ofMinutes(5))
                                              .connectionAcquisitionTimeout(Duration.ofMinutes(5)))
                         .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                  .build())
                         .build();
    return client;
  }
}
