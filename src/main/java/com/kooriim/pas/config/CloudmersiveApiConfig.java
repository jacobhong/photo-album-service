package com.kooriim.pas.config;

import com.cloudmersive.client.ConvertImageApi;
import com.cloudmersive.client.invoker.ApiClient;
import com.cloudmersive.client.invoker.Configuration;
import com.cloudmersive.client.invoker.auth.ApiKeyAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.time.Duration;

@Component
public class CloudmersiveApiConfig {
  @Value("${cloudmersive.apiKey}")
  private String cloudMersiveApiKey;
  @Bean
  public ConvertImageApi convertImageApi() {

    ApiClient defaultClient = Configuration.getDefaultApiClient();
    ApiKeyAuth Apikey = (ApiKeyAuth) defaultClient.getAuthentication("Apikey");
    Apikey.setApiKey(cloudMersiveApiKey);
    ConvertImageApi apiInstance = new ConvertImageApi();
    apiInstance.setApiClient(defaultClient);

    return apiInstance;
  }
}
