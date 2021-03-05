package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class IdentityToken implements Serializable {
  @JsonProperty("access_token")
  public String accessToken;

  @JsonProperty("expires_in")
  public String  expiresIn;

  @JsonProperty("refresh_expires_in")
  public String refreshExpiresIn;

  @JsonProperty("token_type")
  public String tokenType;

  @JsonProperty("id_token")
  public String idToken;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(String expiresIn) {
    this.expiresIn = expiresIn;
  }

  public String getRefreshExpiresIn() {
    return refreshExpiresIn;
  }

  public void setRefreshExpiresIn(String refreshExpiresIn) {
    this.refreshExpiresIn = refreshExpiresIn;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }
}
