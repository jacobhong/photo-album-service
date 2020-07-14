package com.kooriim.pas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Component
public class AwsClientConfig {

  @Bean
  public S3Client awsS3Client() {
    final var client = S3Client.builder().region(Region.US_WEST_1).build();
    return client;
  }
}
