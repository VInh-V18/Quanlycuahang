package com.quanlycuahang.erp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.auth.entity.Permission;
import com.quanlycuahang.erp.auth.repository.PermissionRepository;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prompt #4 (P1 — audit cách ly tenant + IDOR, mục 7: ma trận quyền): quét TOÀN BỘ bean
 * {@code @RestController} qua reflection (KHÔNG hardcode danh sách endpoint — tự cập nhật khi có
 * Controller/endpoint mới), lấy mọi chuỗi quyền {@code hasAuthority(...)}/{@code
 * hasAnyAuthority(...)} trong {@code @PreAuthorize}, rồi đối chiếu với bảng {@code permissions}
 * thật (đã Flyway migrate) — bắt lỗi "gõ nhầm mã quyền" (typo) thật sự đã tồn tại trong 1 lần rà
 * soát khi viết Prompt #4 này (xem test bên dưới): loại lỗi này khiến {@code @PreAuthorize} luôn
 * đánh giá false cho MỌI vai trò (kể cả owner/chủ cửa hàng), một bug "khoá nhầm toàn bộ chức năng"
 * âm thầm mà không unit test riêng lẻ nào của từng Controller phát hiện ra được, vì mã quyền sai
 * KHÔNG gây lỗi biên dịch (chỉ là 1 chuỗi String trong SpEL).
 *
 * <p>Không kiểm tra chiều ngược lại (quyền có trong DB nhưng không Controller nào dùng) bằng assert
 * fail — chỉ mang tính thông tin, vì 1 quyền "thừa" không phải lỗ hổng bảo mật.
 */
class PermissionMatrixIT extends AbstractIntegrationTest {

  @Autowired private ApplicationContext applicationContext;
  @Autowired private PermissionRepository permissionRepository;

  private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");
  private static final Pattern HAS_ANY_AUTHORITY = Pattern.compile("hasAnyAuthority\\(([^)]+)\\)");
  private static final Pattern QUOTED_STRING = Pattern.compile("'([^']+)'");

  private Set<String> scanPermissionCodesUsedInControllers() {
    Set<String> codes = new HashSet<>();
    Map<String, Object> controllers =
        applicationContext.getBeansWithAnnotation(RestController.class);
    for (Object bean : controllers.values()) {
      Class<?> targetClass = AopUtils.getTargetClass(bean);
      for (Method method : targetClass.getMethods()) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        if (preAuthorize == null) {
          continue;
        }
        String expression = preAuthorize.value();
        Matcher single = HAS_AUTHORITY.matcher(expression);
        while (single.find()) {
          codes.add(single.group(1));
        }
        Matcher any = HAS_ANY_AUTHORITY.matcher(expression);
        while (any.find()) {
          Matcher quoted = QUOTED_STRING.matcher(any.group(1));
          while (quoted.find()) {
            codes.add(quoted.group(1));
          }
        }
      }
    }
    return codes;
  }

  @Test
  void everyPermissionCodeReferencedInPreAuthorizeExistsInSeededPermissionsTable() {
    Set<String> usedInCode = scanPermissionCodesUsedInControllers();
    assertThat(usedInCode).isNotEmpty();

    Set<String> seededInDb =
        permissionRepository.findAllByOrderByCodeAsc().stream()
            .map(Permission::getCode)
            .collect(java.util.stream.Collectors.toSet());

    Set<String> missingFromDb = new HashSet<>(usedInCode);
    missingFromDb.removeAll(seededInDb);

    assertThat(missingFromDb)
        .as(
            "Cac ma quyen sau duoc dung trong @PreAuthorize nhung KHONG ton tai trong bang "
                + "permissions - @PreAuthorize se LUON tra ve false cho MOI vai trov (ke ca "
                + "owner), khoa nham chuc nang ma khong bao gio bien dich loi")
        .isEmpty();
  }
}
