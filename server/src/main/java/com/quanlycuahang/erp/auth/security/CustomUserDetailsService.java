package com.quanlycuahang.erp.auth.security;

import com.quanlycuahang.erp.auth.entity.Permission;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nap UserDetails tu bang users, quyen (authorities) la hop cua toan bo permission tu moi role duoc
 * gan (D3). @Transactional de giu session mo khi doc lazy collection roles/permissions.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    User user =
        userRepository
            .findByUsernameAndActiveTrue(username)
            .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user: " + username));

    Set<GrantedAuthority> authorities =
        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .disabled(!user.isActive())
        .build();
  }
}
