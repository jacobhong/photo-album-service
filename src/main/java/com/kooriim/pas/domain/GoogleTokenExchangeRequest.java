package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GoogleTokenExchangeRequest implements Serializable {
  @JsonProperty("client_id")
  private String clientId;
  @JsonProperty("client_secret")
  private String clientSecret;
  @JsonProperty("subject_token")
  private String subjectToken;
  @JsonProperty("request_issuer")
  private String requestIssuer;
  @JsonProperty("grant_type")
  private String grantType;
  @JsonProperty("requested_tokenm")
  private String requestedToken;

  public GoogleTokenExchangeRequest(){}

  public GoogleTokenExchangeRequest(String clientId, String clientSecret, String subjectToken) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.subjectToken = subjectToken;
    this.grantType = "urn:ietf:params:oauth:grant-type:token-exchange";
    this.requestedToken = "urn:ietf:params:oauth:token-type:access_token";
    this.requestIssuer = "google";
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getSubjectToken() {
    return subjectToken;
  }

  public void setSubjectToken(String subjectToken) {
    this.subjectToken = subjectToken;
  }

  public String getRequestIssuer() {
    return requestIssuer;
  }

  public void setRequestIssuer(String requestIssuer) {
    this.requestIssuer = requestIssuer;
  }

  public String getGrantType() {
    return grantType;
  }

  public void setGrantType(String grantType) {
    this.grantType = grantType;
  }

  public String getRequestedToken() {
    return requestedToken;
  }

  public void setRequestedToken(String requestedToken) {
    this.requestedToken = requestedToken;
  }
}
