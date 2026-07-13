package com.quanlycuahang.erp.auth.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bat Hibernate @Filter "tenantFilter" (khai bao o TenantScopedEntity) voi tenantId cua nguoi dang
 * nhap (doc tu TenantContext, do JwtAuthenticationFilter ghi vao truoc filter nay) — tu do MOI truy
 * van JPQL/Criteria len 33 Entity nghiep vu deu tu dong loc dung tenant, khong ai phai nho them
 * dieu kien tay (giong co che @Where(deleted_at IS NULL) da co san).
 *
 * <p>QUAN TRONG (bug phat hien khi kiem chung Giai doan 4): voi open-in-view=false (co chu dinh,
 * tranh giu connection ca request), KHONG co Session/EntityManager nao dang mo luc Filter nay chay
 * (Filter chay TRUOC moi @Transactional cua Controller/Service). goi entityManager.unwrap(...) luc
 * do se tao 1 EntityManager "dung 1 lan" rieng cho chinh loi goi nay roi dong ngay — enableFilter()
 * khong loi nhung KHONG con hieu luc voi Session THAT SU duoc @Transactional mo sau do, nghia la
 * MOI truy van JPA thuong (khong phai native query da tu sua tay o 12 file rieng) chay HOAN TOAN
 * KHONG loc tenant — ro ri toan bo du lieu giua cac cua hang.
 *
 * <p>Fix: tu quan ly 1 EntityManager rieng, gan (bind) vao TransactionSynchronizationManager cho
 * SUOT request nay truoc khi bat filter — dung chinh co che ma Spring OpenEntityManagerInViewFilter
 * dung (Spring "tim thay" EntityManagerHolder da bind nay va DUNG LAI, khong tao Session moi,
 * khi @Transactional cua Controller/Service mo sau do), nhung khoanh vung gon trong 1 Filter duy
 * nhat (khong phu thuoc thu tu 2 Filter khac nhau nhu neu bat lai open-in-view=true toan cuc).
 * Logic bind/unbind nay nam o TenantSessionBinder de dung lai duoc cho cac tac vu chay tren thread
 * khac (vd @Async — xem InvoiceEmailService), khong chi rieng HTTP request.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

  private final TenantSessionBinder tenantSessionBinder;

  public TenantFilter(TenantSessionBinder tenantSessionBinder) {
    this.tenantSessionBinder = tenantSessionBinder;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    EntityManager entityManager = tenantSessionBinder.bind(TenantContext.get());
    try {
      filterChain.doFilter(request, response);
    } finally {
      tenantSessionBinder.unbind(entityManager);
    }
  }
}
