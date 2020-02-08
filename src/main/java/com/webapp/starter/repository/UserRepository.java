package com.webapp.starter.repository;

import com.webapp.starter.domain.GoogleOidUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<GoogleOidUser, Integer> {

  GoogleOidUser findByEmail(String email);
}
