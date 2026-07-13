package com.quanlycuahang.erp.platform.service;

import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.platform.dto.TenantCreateRequest;
import com.quanlycuahang.erp.platform.dto.TenantResponse;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.TenantRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin tao/xem/khoa Tenant (cua hang) - CHI Super Admin duoc goi (khong co dang ky cong
 * khai). Tao tenant moi luon kem 1 chi nhanh mac dinh + 1 tai khoan chu cua hang (role "owner", da
 * co san toan quyen trong tenant do qua bang role_permissions dung chung moi tenant) de chu cua
 * hang tu dang nhap va tu cau hinh moi thu con lai, khong can Super Admin lam thay.
 *
 * <p>Khong dua vao TenantContext o day: Branch/User moi tao duoc gan tenant qua setTenant(...) TRUC
 * TIEP (khong phai qua @PrePersist doc TenantContext) vi luong Super Admin khong dang nhap vao 1
 * tenant nao ca - TenantContext luon null suot request nay, dung y nhu vay (moi truy van doc o
 * Service nay tu nhien khong bi Hibernate @Filter gioi han, dung dang cho "xem duoc moi tenant").
 */
@Service
public class TenantAdminService {

  private static final String DEFAULT_BRANCH_NAME = "Chi nhanh 1";
  private static final String OWNER_ROLE_CODE = "owner";

  private final TenantRepository tenantRepository;
  private final BranchRepository branchRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final PlatformAuditService platformAuditService;
  private final JdbcTemplate jdbcTemplate;

  public TenantAdminService(
      TenantRepository tenantRepository,
      BranchRepository branchRepository,
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      PlatformAuditService platformAuditService,
      JdbcTemplate jdbcTemplate) {
    this.tenantRepository = tenantRepository;
    this.branchRepository = branchRepository;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.platformAuditService = platformAuditService;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Thu tu 35 cau DELETE duoi day la thu tu phu thuoc khoa ngoai giua 33 bang nghiep vu tenant-
   * scoped - da XAC MINH bang truy van information_schema.table_constraints truc tiep tren DB dang
   * chay (khong doan/nho lai), khong phai suy doan tu ten bang. Danh sach nay PHAI cap nhat neu co
   * Entity/bang tenant-scoped MOI duoc them vao he thong sau nay (kiem tra lai FK graph, khong chi
   * them dai vao cuoi).
   */
  private static final List<String> TENANT_DELETE_ORDER =
      List.of(
          // Muc 0: khong bang nao khac (con lai) tham chieu toi cac bang nay.
          "DELETE FROM audit_logs WHERE tenant_id = ?",
          "DELETE FROM cash_transactions WHERE tenant_id = ?",
          "DELETE FROM debt_payments WHERE tenant_id = ?",
          "DELETE FROM inventory WHERE tenant_id = ?",
          "DELETE FROM inventory_batches WHERE tenant_id = ?",
          "DELETE FROM inventory_transactions WHERE tenant_id = ?",
          "DELETE FROM invoices WHERE tenant_id = ?",
          "DELETE FROM order_payments WHERE tenant_id = ?",
          "DELETE FROM parked_orders WHERE tenant_id = ?",
          "DELETE FROM price_history WHERE tenant_id = ?",
          "DELETE FROM product_units WHERE tenant_id = ?",
          "DELETE FROM promotions WHERE tenant_id = ?",
          "DELETE FROM return_items WHERE tenant_id = ?",
          "DELETE FROM settings WHERE tenant_id = ?",
          "DELETE FROM stock_take_items WHERE tenant_id = ?",
          "DELETE FROM tenant_sequences WHERE tenant_id = ?",
          "DELETE FROM voucher_usages WHERE tenant_id = ?",
          "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE tenant_id = ?)",
          "DELETE FROM user_branches WHERE user_id IN (SELECT id FROM users WHERE tenant_id = ?)",
          // Muc 1
          "DELETE FROM purchase_order_items WHERE tenant_id = ?",
          "DELETE FROM order_items WHERE tenant_id = ?",
          "DELETE FROM invoice_templates WHERE tenant_id = ?",
          "DELETE FROM debts WHERE tenant_id = ?",
          "DELETE FROM returns WHERE tenant_id = ?",
          "DELETE FROM stock_takes WHERE tenant_id = ?",
          // Muc 2
          "DELETE FROM products WHERE tenant_id = ?",
          "DELETE FROM purchase_orders WHERE tenant_id = ?",
          "DELETE FROM orders WHERE tenant_id = ?",
          // Muc 3 (categories: go lien ket cha/con truoc - xem deleteTenant())
          "DELETE FROM categories WHERE tenant_id = ?",
          "DELETE FROM suppliers WHERE tenant_id = ?",
          "DELETE FROM vouchers WHERE tenant_id = ?",
          "DELETE FROM shifts WHERE tenant_id = ?",
          "DELETE FROM customers WHERE tenant_id = ?",
          // Muc 4
          "DELETE FROM customer_groups WHERE tenant_id = ?",
          "DELETE FROM branches WHERE tenant_id = ?",
          "DELETE FROM users WHERE tenant_id = ?");

  @Transactional(readOnly = true)
  public List<TenantResponse> listTenants() {
    return tenantRepository.findAllByOrderByIdAsc().stream()
        .map(TenantAdminService::toResponse)
        .toList();
  }

  @Transactional
  public TenantResponse createTenant(TenantCreateRequest request) {
    if (userRepository.existsByUsername(request.getOwnerUsername())) {
      throw new BusinessRuleException(
          "TENANT_OWNER_USERNAME_TAKEN", "Tên đăng nhập này đã được sử dụng");
    }
    Role ownerRole =
        roleRepository
            .findByCode(OWNER_ROLE_CODE)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Thieu du lieu he thong: role '" + OWNER_ROLE_CODE + "'"));

    Tenant tenant = new Tenant();
    tenant.setName(request.getTenantName());
    tenant.setActive(true);
    tenant = tenantRepository.save(tenant);

    Branch branch = new Branch();
    branch.setTenant(tenant);
    branch.setName(blankToDefault(request.getBranchName(), DEFAULT_BRANCH_NAME));
    branch.setActive(true);
    branch = branchRepository.save(branch);

    User owner = new User();
    owner.setTenant(tenant);
    owner.setUsername(request.getOwnerUsername());
    owner.setPasswordHash(passwordEncoder.encode(request.getOwnerPassword()));
    owner.setFullName(request.getOwnerFullName());
    owner.setActive(true);
    owner.setRoles(Set.of(ownerRole));
    owner.setBranches(Set.of(branch));
    userRepository.save(owner);

    platformAuditService.record(
        "TENANT_CREATE",
        tenant.getId(),
        tenant.getName(),
        java.util.Map.of("ownerUsername", request.getOwnerUsername()));

    return toResponse(tenant);
  }

  @Transactional
  public TenantResponse setActive(Long tenantId, boolean active) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng"));
    tenant.setActive(active);
    TenantResponse response = toResponse(tenantRepository.save(tenant));
    platformAuditService.record(
        active ? "TENANT_ACTIVATE" : "TENANT_DEACTIVATE", tenant.getId(), tenant.getName(), null);
    return response;
  }

  /**
   * XOA VINH VIEN tenant va TOAN BO du lieu nghiep vu cua no - khac han setActive(false) (khoa, van
   * giu du lieu). Dung raw SQL (JdbcTemplate) chu KHONG phai tenantRepository.delete()/
   * userRepository.delete(): moi Entity trong he thong deu co @SQLDelete (chi UPDATE deleted_at,
   * khong xoa that), goi qua JPA se chi "khoa" chu khong xoa vinh vien nhu ten goi. Toan bo nam
   * trong 1 @Transactional: neu 1 buoc bat ky loi (vd thieu 1 bang trong danh sach do them Entity
   * moi sau nay), CA GIAO DICH ROLLBACK, khong bao gio de lai trang thai xoa nua chung.
   */
  @Transactional
  public void deleteTenant(Long tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng"));
    String tenantName = tenant.getName();

    // Chup lai owner/chi nhanh va so dong TUNG bang TRUOC khi xoa - hanh dong nay khong the hoan
    // tac, nen dong nhat ky duy nhat cua no phai du chi tiet de dieu tra sau nay (truoc day chi
    // ghi targetDescription=ten cua hang, khong biet xoa bao nhieu dong/cua ai - phat hien khi
    // rieng soat, lien quan truc tiep 1 su co xoa tenant khong ro tac nhan).
    List<String> ownerUsernames =
        userRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
            .map(User::getUsername)
            .toList();
    int branchCount = branchRepository.findByTenantId(tenantId).size();
    Map<String, Long> rowCounts = new LinkedHashMap<>();

    // Danh muc tu tham chieu (parent_id, cay 2 cap) - go lien ket cha/con truoc de 1 cau DELETE
    // FROM categories duy nhat sau do khong phu thuoc thu tu xoa giua cac dong cung bang.
    jdbcTemplate.update("UPDATE categories SET parent_id = NULL WHERE tenant_id = ?", tenantId);

    for (String sql : TENANT_DELETE_ORDER) {
      String countSql = sql.replaceFirst("(?i)^DELETE FROM", "SELECT COUNT(*) FROM");
      String tableName = sql.replaceFirst("(?i)^DELETE FROM (\\w+).*", "$1");
      Long count = jdbcTemplate.queryForObject(countSql, Long.class, tenantId);
      if (count != null && count > 0) {
        rowCounts.merge(tableName, count, Long::sum);
      }
      jdbcTemplate.update(sql, tenantId);
    }

    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("ownerUsernames", ownerUsernames);
    detail.put("branchCount", branchCount);
    detail.put("rowCounts", rowCounts);

    // Ghi truoc khi xoa dong tenants - tenantId truyen null (KHONG phai tenantId vua xoa) vi FK
    // cua chinh dong nhat ky nay phai tro toi 1 tenant CON TON TAI; ten cua hang da nam san trong
    // targetDescription nen van doc duoc day du sau khi tenant khong con nua. Cac dong nhat ky CU
    // (TENANT_CREATE, TENANT_USER_CREATE...) tu dong con lai voi tenant_id=NULL nho ON DELETE SET
    // NULL (V21__platform_audit_log_survive_tenant_delete.sql), khong bi xoa theo.
    platformAuditService.record("TENANT_DELETE", null, tenantName, detail);

    jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId);
  }

  private static String blankToDefault(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private static TenantResponse toResponse(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(), tenant.getName(), tenant.isActive(), tenant.getCreatedAt());
  }
}
