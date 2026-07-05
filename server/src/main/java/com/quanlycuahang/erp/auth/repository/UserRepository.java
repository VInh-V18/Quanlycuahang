package com.quanlycuahang.erp.auth.repository;

import com.quanlycuahang.erp.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsernameAndActiveTrue(String username);

  boolean existsByUsername(String username);
}
