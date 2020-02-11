package com.kooriim.pas.repository;

import com.kooriim.pas.domain.GoogleOidUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<GoogleOidUser, Integer> {
  GoogleOidUser findByEmail(String email);
}
