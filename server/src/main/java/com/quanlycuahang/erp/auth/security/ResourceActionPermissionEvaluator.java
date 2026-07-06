package com.quanlycuahang.erp.auth.security;

import java.io.Serializable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * PermissionEvaluator cho quyen dang "resource:action" (Phase 1.1). Cho phep
 * dung @PreAuthorize("hasPermission(#id, 'product', 'update')") khi can gan quyen voi 1 doi tuong
 * cu the; truong hop don gian (khong gan doi tuong) nen dung truc tiep
 * hasAuthority('product:update') nhu D3 da mo ta.
 */
@Component
public class ResourceActionPermissionEvaluator implements PermissionEvaluator {

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    return hasAuthority(authentication, String.valueOf(permission));
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return hasAuthority(authentication, targetType + ":" + permission);
  }

  private boolean hasAuthority(Authentication authentication, String required) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals(required));
  }
}
