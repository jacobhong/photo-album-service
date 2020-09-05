package com.kooriim.pas.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.usertype.UserType;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleOidUser implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name ="google_id")
  private String googleId;

  @Column
  private String name;
  @Column
  private String email;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  @Override
  public String toString() {
    return "GoogleOidUser{" +
             "id=" + id +
             ", googleId='" + googleId + '\'' +
             ", name='" + name + '\'' +
             ", email='" + email + '\'' +
             '}';
  }
}