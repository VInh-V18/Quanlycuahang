package com.quanlycuahang.erp.auth.security;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lay User entity dang dang nhap tu SecurityContext — dung chung cho moi Service can ghi nguoi thuc
 * hien.
 */
@Component
public class CurrentUserProvider {

  private final UserRepository userRepository;

  public CurrentUserProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Optional<User> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByUsernameAndActiveTrue(authentication.getName());
  }

  public User requireCurrentUser() {
    return getCurrentUser()
        .orElseThrow(
            () -> new ResourceNotFoundException("Không xác định được người dùng hiện tại"));
  }
}
