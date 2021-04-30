package com.kooriim.pas.repository;

import com.kooriim.pas.domain.GoogleOidUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<GoogleOidUser, Integer> {
  Optional<GoogleOidUser> findByEmail(String email);
  Optional<GoogleOidUser> findByGoogleId(String googleId);

}
