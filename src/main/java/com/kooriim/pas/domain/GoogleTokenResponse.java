package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GoogleTokenResponse implements Serializable {
  @JsonProperty("access_token")
  public String accessToken;

  @JsonProperty("refresh_token")
  public String refreshToken;

  @JsonProperty("expires_in")
  public String expiresIn;

  @JsonProperty("refresh_expires_in")
  public String refreshExpiresIn;

  @JsonProperty("token_type")
  public String tokenType;

  @JsonProperty("id_token")
  public String idToken;

  @JsonProperty("account_link_url")
  public String accountLinkUrl;

  public String error;

  @JsonProperty("error_description")
  public String errorDescription;

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

  public String getAccountLinkUrl() {
    return accountLinkUrl;
  }

  public void setAccountLinkUrl(String accountLinkUrl) {
    this.accountLinkUrl = accountLinkUrl;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getErrorDescription() {
    return errorDescription;
  }

  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  @Override
  public String toString() {
    return "GoogleTokenResponse{" +
             "accessToken='" + accessToken + '\'' +
             ", refreshToken='" + refreshToken + '\'' +
             ", expiresIn='" + expiresIn + '\'' +
             ", refreshExpiresIn='" + refreshExpiresIn + '\'' +
             ", tokenType='" + tokenType + '\'' +
             ", idToken='" + idToken + '\'' +
             ", accountLinkUrl='" + accountLinkUrl + '\'' +
             ", error='" + error + '\'' +
             ", errorDescription='" + errorDescription + '\'' +
             '}';
  }
}
